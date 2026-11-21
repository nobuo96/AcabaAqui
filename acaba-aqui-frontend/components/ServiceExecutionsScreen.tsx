import { useCallback, useState } from 'react';
import { Alert, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect } from 'expo-router';
import * as DocumentPicker from 'expo-document-picker';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { InAppNotificationInbox } from '@/components/InAppNotificationInbox';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import {
  serviceDisputeApi,
  serviceExecutionApi,
  type ServiceDispute,
  type ServiceDisputeEvidence,
  type ServiceExecution,
} from '@/lib/communicationApi';
import { getAuthUserId } from '@/lib/authToken';
import { openDisputeEvidence } from '@/lib/openDisputeEvidence';

interface Props {
  role: 'cliente' | 'prestador';
}

export function ServiceExecutionsScreen({ role }: Props) {
  const [executions, setExecutions] = useState<ServiceExecution[]>([]);
  const [selected, setSelected] = useState<ServiceExecution | null>(null);
  const [dispute, setDispute] = useState<ServiceDispute | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const [disputeReason, setDisputeReason] = useState('');
  const [disputeWorking, setDisputeWorking] = useState(false);
  const [evidenceWorking, setEvidenceWorking] = useState(false);
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

  useFocusEffect(useCallback(() => {
    let active = true;
    void getAuthUserId().then((id) => {
      if (active) setUserId(id);
    });
    return () => { active = false; };
  }, []));

  async function toggleDetails(quoteId: number) {
    if (selected?.quoteId === quoteId) {
      setSelected(null);
      setDispute(null);
      return;
    }
    setLoadingDetail(true);
    setError('');
    try {
      const [execution, currentDispute] = await Promise.all([
        serviceExecutionApi.get(quoteId),
        serviceDisputeApi.getForQuote(quoteId),
      ]);
      setSelected(execution);
      setDispute(currentDispute);
      setDisputeReason('');
    } catch (detailError) {
      setError(detailError instanceof Error ? detailError.message : 'Não foi possível carregar os detalhes.');
    } finally {
      setLoadingDetail(false);
    }
  }

  async function openDispute(execution: ServiceExecution) {
    if (disputeWorking || !disputeReason.trim()) {
      setError('Descreva o motivo da contestação.');
      return;
    }
    setDisputeWorking(true);
    setError('');
    try {
      const created = await serviceDisputeApi.open(execution.quoteId, disputeReason.trim());
      setDispute(created);
      setDisputeReason('');
      setSelected(await serviceExecutionApi.get(execution.quoteId));
      await load();
    } catch (openError) {
      setError(openError instanceof Error ? openError.message : 'Não foi possível abrir a disputa.');
    } finally {
      setDisputeWorking(false);
    }
  }

  async function attachEvidence(currentDispute: ServiceDispute) {
    if (evidenceWorking) return;
    setEvidenceWorking(true);
    setError('');
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['image/jpeg', 'image/png', 'application/pdf'],
        copyToCacheDirectory: true,
        multiple: false,
      });
      if (result.canceled) return;
      const asset = result.assets[0];
      if (asset.size !== undefined && asset.size > 10 * 1024 * 1024) {
        throw new Error('Cada evidência pode ter no máximo 10 MB.');
      }
      const fileResponse = await fetch(asset.uri);
      const blob = await fileResponse.blob();
      const contentType = evidenceContentType(asset.name);
      if (!contentType) {
        throw new Error('Formato não permitido. Envie JPG, PNG ou PDF.');
      }
      const formData = new FormData();
      formData.append('file', blob.slice(0, blob.size, contentType), asset.name);
      await serviceDisputeApi.uploadEvidence(currentDispute.id, formData);
      setDispute(await serviceDisputeApi.get(currentDispute.id));
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : 'Não foi possível anexar a evidência.');
    } finally {
      setEvidenceWorking(false);
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
              <ExecutionDetails
                execution={selected}
                role={role}
                dispute={dispute}
                userId={userId}
                disputeReason={disputeReason}
                disputeWorking={disputeWorking}
                evidenceWorking={evidenceWorking}
                onDisputeReasonChange={setDisputeReason}
                onOpenDispute={() => { void openDispute(execution); }}
                onAttachEvidence={() => { if (dispute) void attachEvidence(dispute); }}
                onOpenEvidence={(evidence) => {
                  if (dispute) {
                    void openDisputeEvidence(dispute.id, evidence).catch((openError: unknown) => {
                      setError(openError instanceof Error ? openError.message : 'Não foi possível abrir a evidência.');
                    });
                  }
                }}
              />
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

function evidenceContentType(filename: string) {
  const extension = filename.split('.').pop()?.toLowerCase();
  if (extension === 'jpg' || extension === 'jpeg') return 'image/jpeg';
  if (extension === 'png') return 'image/png';
  if (extension === 'pdf') return 'application/pdf';
  return null;
}

function ExecutionDetails({
  execution,
  role,
  dispute,
  userId,
  disputeReason,
  disputeWorking,
  evidenceWorking,
  onDisputeReasonChange,
  onOpenDispute,
  onAttachEvidence,
  onOpenEvidence,
}: Props & {
  execution: ServiceExecution;
  dispute: ServiceDispute | null;
  userId: string | null;
  disputeReason: string;
  disputeWorking: boolean;
  evidenceWorking: boolean;
  onDisputeReasonChange: (value: string) => void;
  onOpenDispute: () => void;
  onAttachEvidence: () => void;
  onOpenEvidence: (evidence: ServiceDisputeEvidence) => void;
}) {
  const canOpenDispute = role === 'cliente'
    && (execution.status === 'EM_ANDAMENTO' || execution.status === 'CONCLUIDA_PELO_PRESTADOR')
    && execution.startedAt !== null
    && execution.approvedAt === null
    && dispute === null;
  const ownEvidenceCount = dispute?.evidence.filter(
    (item) => String(item.uploaderId) === userId,
  ).length ?? 0;
  const canUploadEvidence = dispute?.status === 'ABERTA'
    && dispute.evidenceDeadline !== null
    && Date.now() <= new Date(dispute.evidenceDeadline).getTime()
    && ownEvidenceCount < 10;

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
        </>
      ) : null}
      {execution.approvedAt ? <Text style={styles.detail}>Aprovado: {formatDate(execution.approvedAt)}</Text> : null}
      {canOpenDispute ? (
        <View style={styles.disputeBox}>
          <Text style={styles.historyHeading}>Contestar serviço</Text>
          <Text style={styles.muted}>Após abrir a disputa, ambas as partes terão 7 dias corridos para anexar até 10 arquivos JPG, PNG ou PDF (máximo 10 MB cada).</Text>
          <TextInput
            style={[styles.input, styles.multilineInput]}
            value={disputeReason}
            onChangeText={onDisputeReasonChange}
            maxLength={1000}
            multiline
            textAlignVertical="top"
            placeholder="Explique o motivo da contestação"
          />
          <ActionButton
            label={disputeWorking ? 'Abrindo disputa...' : 'Abrir disputa'}
            disabled={disputeWorking || !disputeReason.trim()}
            onPress={onOpenDispute}
          />
        </View>
      ) : null}
      {dispute ? (
        <View style={styles.disputeBox}>
          <Text style={styles.historyHeading}>Disputa: {disputeStatusLabel(dispute.status)}</Text>
          <Text style={styles.detail}>Motivo: {dispute.reason}</Text>
          {dispute.status === 'ABERTA' ? (
            <>
              <Text style={styles.deadline}>Evidências até: {formatDate(dispute.evidenceDeadline)}</Text>
              {dispute.paymentStatus === 'REEMBOLSO_PENDENTE' ? (
                <Text style={styles.paymentWarning}>
                  O reembolso solicitado está em processamento no Mercado Pago; a disputa continua aberta.
                </Text>
              ) : null}
            </>
          ) : (
            <>
              <Text style={styles.detail}>Decisão: {disputeDecisionLabel(dispute.decision)}</Text>
              {dispute.decisionReason ? <Text style={styles.detail}>Justificativa: {dispute.decisionReason}</Text> : null}
            </>
          )}
          {dispute.evidence.map((evidence) => (
            <Pressable key={evidence.id} onPress={() => onOpenEvidence(evidence)} style={styles.evidenceRow}>
              <Text style={styles.evidenceName}>{evidence.filename}</Text>
              <Text style={styles.muted}>
                {evidence.uploaderName} · {(evidence.sizeBytes / 1024 / 1024).toFixed(2)} MB · Abrir
              </Text>
            </Pressable>
          ))}
          {canUploadEvidence ? (
            <ActionButton
              label={evidenceWorking ? 'Anexando...' : `Anexar evidência (${ownEvidenceCount}/10)`}
              disabled={evidenceWorking}
              onPress={onAttachEvidence}
            />
          ) : dispute.status === 'ABERTA' ? (
            <Text style={styles.muted}>
              {ownEvidenceCount >= 10 ? 'Você já anexou o limite de 10 arquivos.' : 'O prazo para anexar evidências foi encerrado.'}
            </Text>
          ) : null}
        </View>
      ) : null}
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

function disputeStatusLabel(status: ServiceDispute['status']) {
  switch (status) {
    case 'ABERTA': return 'em análise';
    case 'ENCERRADA_REEMBOLSO': return 'encerrada com reembolso ao cliente';
    case 'ENCERRADA_REPASSE': return 'encerrada com repasse ao prestador';
  }
}

function disputeDecisionLabel(decision: ServiceDispute['decision']) {
  return decision === 'REEMBOLSAR_CLIENTE' ? 'Reembolsar cliente' : 'Pagar prestador';
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
  disputeBox: { backgroundColor: '#faf5f5', borderRadius: 9, padding: 12, gap: 8 },
  evidenceRow: { backgroundColor: '#fff', borderRadius: 8, padding: 10, gap: 4 },
  evidenceName: { color: '#a43434', fontSize: 14, fontWeight: '700' },
  input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 10, color: '#222', backgroundColor: '#fff' },
  multilineInput: { minHeight: 75 },
  description: { color: '#414141', fontSize: 14, lineHeight: 20 },
  deadline: { color: '#854c00', fontSize: 14, fontWeight: '700' },
  historyHeading: { color: '#282828', fontSize: 15, fontWeight: '700', marginTop: 4 },
  historyItem: { color: '#555', fontSize: 13 },
  actionButton: { backgroundColor: '#a43434', borderRadius: 8, padding: 12, alignItems: 'center', marginTop: 4 },
  actionText: { color: '#fff', fontSize: 14, fontWeight: '700' },
  disabled: { opacity: 0.6 },
});
