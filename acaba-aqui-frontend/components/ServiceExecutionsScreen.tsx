import { useCallback, useState } from 'react';
import { Alert, Pressable, StyleSheet, View } from 'react-native';
import { useFocusEffect } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { InAppNotificationInbox } from '@/components/InAppNotificationInbox';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import {
  serviceExecutionApi,
  type ServiceExecution,
} from '@/lib/communicationApi';

interface Props {
  role: 'cliente' | 'prestador';
}

export function ServiceExecutionsScreen({ role }: Props) {
  const [executions, setExecutions] = useState<ServiceExecution[]>([]);
  const [selected, setSelected] = useState<ServiceExecution | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setExecutions(await serviceExecutionApi.list());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar os serviços.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function toggleDetails(quoteId: number) {
    if (selected?.quoteId === quoteId) {
      setSelected(null);
      return;
    }
    setLoadingDetail(true);
    setError('');
    try {
      setSelected(await serviceExecutionApi.get(quoteId));
    } catch (detailError) {
      setError(detailError instanceof Error ? detailError.message : 'Não foi possível carregar os detalhes.');
    } finally {
      setLoadingDetail(false);
    }
  }

  async function performAction(
    execution: ServiceExecution,
    action: 'start' | 'complete' | 'approve',
  ) {
    if (workingId !== null) return;
    setWorkingId(execution.quoteId);
    setError('');
    try {
      const updated = action === 'start'
        ? await serviceExecutionApi.start(execution.quoteId)
        : action === 'complete'
          ? await serviceExecutionApi.complete(execution.quoteId)
          : await serviceExecutionApi.approve(execution.quoteId);
      setSelected(updated);
      await load();
    } catch (actionError) {
      const message = actionError instanceof Error
        ? actionError.message
        : 'Não foi possível atualizar o serviço.';
      await load();
      if (selected?.quoteId === execution.quoteId) {
        try {
          setSelected(await serviceExecutionApi.get(execution.quoteId));
        } catch (detailError) {
          setSelected(null);
          setError(detailError instanceof Error
            ? `${message} Não foi possível atualizar os detalhes: ${detailError.message}`
            : message);
          return;
        }
      }
      setError(message);
    } finally {
      setWorkingId(null);
    }
  }

  function confirmApproval(execution: ServiceExecution) {
    Alert.alert(
      'Aprovar serviço',
      'Ao aprovar, você confirma que o serviço foi concluído.',
      [
        { text: 'Voltar', style: 'cancel' },
        { text: 'Aprovar', onPress: () => { void performAction(execution, 'approve'); } },
      ],
    );
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Serviços" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <InAppNotificationInbox />
        <Text style={styles.intro}>
          {role === 'cliente'
            ? 'Acompanhe os serviços contratados e aprove a conclusão quando estiver tudo certo.'
            : 'Acompanhe os serviços contratados e atualize o andamento da execução.'}
        </Text>
        {loading ? <Text style={styles.muted}>Carregando serviços...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && executions.length === 0
          ? <Text style={styles.muted}>Ainda não há serviços pagos em execução.</Text>
          : null}
        {executions.map((execution) => (
          <View key={execution.quoteId} style={styles.card}>
            <View style={styles.heading}>
              <Text style={styles.title}>{execution.serviceName}</Text>
              <Text style={styles.status}>{statusLabel(execution.status)}</Text>
            </View>
            <Text style={styles.detail}>
              {role === 'cliente' ? `Prestador: ${execution.providerName}` : `Cliente: ${execution.clientName}`}
            </Text>
            <Text style={styles.detail}>Valor: {formatCurrency(execution.amount)}</Text>
            {execution.paymentStatus && execution.paymentStatus !== 'PAGO' ? (
              <Text style={styles.paymentWarning}>
                Pagamento: {paymentStatusLabel(execution.paymentStatus)}
              </Text>
            ) : null}
            <Pressable
              disabled={loadingDetail}
              onPress={() => { void toggleDetails(execution.quoteId); }}
              style={styles.detailButton}
            >
              <Text style={styles.detailButtonText}>
                {loadingDetail && selected?.quoteId !== execution.quoteId
                  ? 'Carregando...'
                  : selected?.quoteId === execution.quoteId ? 'Ocultar detalhes' : 'Ver detalhes e histórico'}
              </Text>
            </Pressable>
            {selected?.quoteId === execution.quoteId ? (
              <ExecutionDetails execution={selected} role={role} />
            ) : null}
            {role === 'prestador' && execution.status === 'AGUARDANDO_INICIO'
              && execution.paymentStatus === 'PAGO' ? (
              <ActionButton
                label={workingId === execution.quoteId ? 'Iniciando...' : 'Iniciar serviço'}
                disabled={workingId !== null}
                onPress={() => { void performAction(execution, 'start'); }}
              />
            ) : null}
            {role === 'prestador' && execution.status === 'EM_ANDAMENTO' ? (
              <ActionButton
                label={workingId === execution.quoteId ? 'Concluindo...' : 'Marcar como concluído'}
                disabled={workingId !== null}
                onPress={() => { void performAction(execution, 'complete'); }}
              />
            ) : null}
            {role === 'cliente' && canApprove(execution) ? (
              <ActionButton
                label={workingId === execution.quoteId ? 'Aprovando...' : 'Aprovar serviço'}
                disabled={workingId !== null}
                onPress={() => confirmApproval(execution)}
              />
            ) : null}
          </View>
        ))}
      </ScrollAreaView>
    </View>
  );
}

function ExecutionDetails({ execution, role }: Props & { execution: ServiceExecution }) {
  return (
    <View style={styles.details}>
      <Text style={styles.description}>{execution.description}</Text>
      {execution.startedAt ? <Text style={styles.detail}>Iniciado: {formatDate(execution.startedAt)}</Text> : null}
      {execution.completedAt ? <Text style={styles.detail}>Concluído: {formatDate(execution.completedAt)}</Text> : null}
      {execution.approveBy && execution.status === 'CONCLUIDA_PELO_PRESTADOR' ? (
        <>
          <Text style={styles.deadline}>
            Aprovação até: {formatDate(execution.approveBy)}
          </Text>
          {role === 'cliente' ? (
            <Text style={styles.muted}>
              O prazo é de 7 dias corridos. A contestação pelo aplicativo será implementada na próxima etapa.
            </Text>
          ) : null}
        </>
      ) : null}
      {execution.approvedAt ? <Text style={styles.detail}>Aprovado: {formatDate(execution.approvedAt)}</Text> : null}
      <Text style={styles.historyHeading}>Histórico</Text>
      {execution.history.map((item, index) => (
        <Text key={`${item.status}-${item.occurredAt}-${index}`} style={styles.historyItem}>
          {statusLabel(item.status)} — {formatDate(item.occurredAt)}
        </Text>
      ))}
    </View>
  );
}

function ActionButton({
  label,
  disabled,
  onPress,
}: {
  label: string;
  disabled: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable disabled={disabled} onPress={onPress} style={[styles.actionButton, disabled && styles.disabled]}>
      <Text style={styles.actionText}>{label}</Text>
    </Pressable>
  );
}

function canApprove(execution: ServiceExecution) {
  return execution.status === 'CONCLUIDA_PELO_PRESTADOR'
    && execution.approveBy !== null
    && Date.now() <= new Date(execution.approveBy).getTime();
}

function statusLabel(status: string) {
  switch (status) {
    case 'AGUARDANDO_INICIO': return 'Aguardando início';
    case 'EM_ANDAMENTO': return 'Em andamento';
    case 'CONCLUIDA_PELO_PRESTADOR': return 'Aguardando aprovação';
    case 'APROVADA': return 'Aprovado';
    case 'CONTESTADA': return 'Contestada';
    case 'CANCELADA': return 'Cancelado';
    default: return status;
  }
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function paymentStatusLabel(status: string) {
  switch (status) {
    case 'REEMBOLSO_PENDENTE': return 'reembolso em processamento';
    case 'REEMBOLSADO': return 'reembolsado';
    case 'REEMBOLSADO_PARCIAL': return 'reembolso parcial';
    case 'CONTESTADO': return 'contestado';
    case 'AGUARDANDO_PAGAMENTO': return 'aguardando pagamento';
    default: return status.toLowerCase().replace(/_/g, ' ');
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('pt-BR');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  content: { padding: 16, paddingBottom: 32, gap: 12 },
  intro: { color: '#414141', fontSize: 15, lineHeight: 21, marginBottom: 4 },
  card: { backgroundColor: '#fff', borderRadius: 12, padding: 16, gap: 8 },
  heading: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  title: { color: '#282828', fontSize: 17, fontWeight: '700', flexShrink: 1 },
  status: { color: '#a43434', fontSize: 13, fontWeight: '700', textAlign: 'right' },
  detail: { color: '#484848', fontSize: 14 },
  paymentWarning: { color: '#854c00', fontSize: 13, fontWeight: '600' },
  muted: { color: '#777', fontSize: 14, lineHeight: 20 },
  error: { color: '#a43434', fontSize: 14 },
  detailButton: { alignSelf: 'flex-start', paddingVertical: 6 },
  detailButtonText: { color: '#a43434', fontWeight: '600' },
  details: { borderTopWidth: 1, borderTopColor: '#eee', paddingTop: 10, gap: 8 },
  description: { color: '#414141', fontSize: 14, lineHeight: 20 },
  deadline: { color: '#854c00', fontSize: 14, fontWeight: '700' },
  historyHeading: { color: '#282828', fontSize: 15, fontWeight: '700', marginTop: 4 },
  historyItem: { color: '#555', fontSize: 13 },
  actionButton: { backgroundColor: '#a43434', borderRadius: 8, padding: 12, alignItems: 'center', marginTop: 4 },
  actionText: { color: '#fff', fontSize: 14, fontWeight: '700' },
  disabled: { opacity: 0.6 },
});
