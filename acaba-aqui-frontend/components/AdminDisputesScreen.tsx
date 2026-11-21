import { useCallback, useState } from 'react';
import { Alert, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import {
  serviceDisputeApi,
  type ServiceDispute,
  type ServiceDisputeDecision,
} from '@/lib/communicationApi';
import { openDisputeEvidence } from '@/lib/openDisputeEvidence';

type DisputeFilter = 'abertas' | 'encerradas' | 'todas';

export function AdminDisputesScreen() {
  const [disputes, setDisputes] = useState<ServiceDispute[]>([]);
  const [selected, setSelected] = useState<ServiceDispute | null>(null);
  const [decisionReason, setDecisionReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<DisputeFilter>('abertas');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setDisputes(await serviceDisputeApi.list());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as disputas.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function toggleDetails(disputeId: number) {
    if (selected?.id === disputeId) {
      setSelected(null);
      setDecisionReason('');
      return;
    }
    setError('');
    try {
      setSelected(await serviceDisputeApi.get(disputeId));
      setDecisionReason('');
    } catch (detailError) {
      setError(detailError instanceof Error ? detailError.message : 'Não foi possível carregar a disputa.');
    }
  }

  function confirmDecision(dispute: ServiceDispute, decision: ServiceDisputeDecision) {
    if (!decisionReason.trim()) {
      setError('Informe a justificativa da decisão administrativa.');
      return;
    }
    const label = decision === 'REEMBOLSAR_CLIENTE' ? 'reembolsar o cliente' : 'pagar o prestador';
    Alert.alert('Encerrar disputa', `Confirma a decisão de ${label}?`, [
      { text: 'Voltar', style: 'cancel' },
      { text: 'Confirmar decisão', onPress: () => { void decide(dispute, decision); } },
    ]);
  }

  async function decide(dispute: ServiceDispute, decision: ServiceDisputeDecision) {
    if (working) return;
    setWorking(true);
    setError('');
    try {
      const updated = await serviceDisputeApi.decide(dispute.id, decision, decisionReason.trim());
      setSelected(updated);
      if (updated.status !== 'ABERTA' || updated.paymentStatus !== 'REEMBOLSO_PENDENTE') {
        setDecisionReason('');
      }
      await load();
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : 'Não foi possível registrar a decisão.');
    } finally {
      setWorking(false);
    }
  }

  const openCount = disputes.filter((dispute) => dispute.status === 'ABERTA').length;
  const closedCount = disputes.length - openCount;
  const visibleDisputes = disputes.filter((dispute) => {
    if (filter === 'abertas') return dispute.status === 'ABERTA';
    if (filter === 'encerradas') return dispute.status !== 'ABERTA';
    return true;
  });

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Disputas" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <Text style={styles.intro}>
          Revise o relato e as evidências antes de encerrar cada disputa. O repasse fica bloqueado enquanto ela estiver aberta.
          Evidências expiram 180 dias após o encerramento.
        </Text>
        <View style={styles.filters}>
          <AdminFilterButton label={`Abertas · ${openCount}`} selected={filter === 'abertas'}
            onPress={() => { setFilter('abertas'); setSelected(null); setDecisionReason(''); }} />
          <AdminFilterButton label={`Encerradas · ${closedCount}`} selected={filter === 'encerradas'}
            onPress={() => { setFilter('encerradas'); setSelected(null); setDecisionReason(''); }} />
          <AdminFilterButton label={`Todas · ${disputes.length}`} selected={filter === 'todas'}
            onPress={() => { setFilter('todas'); setSelected(null); setDecisionReason(''); }} />
        </View>
        {loading ? <Text style={styles.muted}>Carregando disputas...</Text> : null}
        {error ? <Text accessibilityRole="alert" style={styles.error}>{error}</Text> : null}
        {!loading && visibleDisputes.length === 0
          ? <Text style={styles.muted}>
              {filter === 'abertas' ? 'Não há disputas abertas aguardando análise.'
                : filter === 'encerradas' ? 'Nenhuma disputa encerrada ainda.' : 'Não há disputas registradas.'}
            </Text>
          : null}
        {visibleDisputes.map((dispute) => (
          <View key={dispute.id} style={styles.card}>
            <View style={styles.heading}>
              <Text style={styles.title}>{dispute.serviceName}</Text>
              <Text style={[styles.badge, dispute.status !== 'ABERTA' && styles.closedBadge]}>
                {dispute.status === 'ABERTA' ? 'Aberta' : 'Encerrada'}
              </Text>
            </View>
            <Text style={styles.line}>Cliente: {dispute.clientName}</Text>
            <Text style={styles.line}>Prestador: {dispute.providerName}</Text>
            <Text style={styles.line}>Aberta: {formatDate(dispute.openedAt)}</Text>
            <Text numberOfLines={3} style={styles.reason}>{dispute.reason}</Text>
            <Pressable onPress={() => { void toggleDetails(dispute.id); }} style={styles.detailButton}>
              <Text style={styles.detailButtonText}>
                {selected?.id === dispute.id ? 'Ocultar detalhes' : 'Ver evidências e decidir'}
              </Text>
            </Pressable>
            {selected?.id === dispute.id ? (
              <View style={styles.details}>
                {selected.evidence.length === 0
                  ? <Text style={styles.muted}>Nenhuma evidência foi anexada.</Text>
                  : selected.evidence.map((evidence) => (
                    <Pressable
                      key={evidence.id}
                      onPress={() => {
                        void openDisputeEvidence(selected.id, evidence).catch((openError: unknown) => {
                          setError(openError instanceof Error ? openError.message : 'Não foi possível abrir a evidência.');
                        });
                      }}
                      style={styles.evidenceRow}
                    >
                      <Text style={styles.evidenceName}>{evidence.filename}</Text>
                      <Text style={styles.muted}>
                        {evidence.uploaderName} · {evidence.contentType} · {(evidence.sizeBytes / 1024 / 1024).toFixed(2)} MB · Abrir
                      </Text>
                    </Pressable>
                  ))}
                {selected.status === 'ABERTA' ? (
                  <>
                    <Text style={styles.deadline}>
                      Prazo para evidências: {formatDate(selected.evidenceDeadline)}
                    </Text>
                    {selected.paymentStatus === 'REEMBOLSO_PENDENTE' ? (
                      <Text style={styles.warning}>
                        O reembolso está em processamento no Mercado Pago; a disputa permanece aberta.
                      </Text>
                    ) : null}
                    {selected.paymentStatus
                      && selected.paymentStatus !== 'PAGO'
                      && selected.paymentStatus !== 'REPASSE_PENDENTE'
                      && selected.paymentStatus !== 'REEMBOLSO_PENDENTE' ? (
                      <Text style={styles.warning}>
                        O estado financeiro atual ({selected.paymentStatus}) impede uma nova decisão automática.
                      </Text>
                    ) : null}
                    <Text style={styles.label}>Justificativa administrativa</Text>
                    <TextInput
                      style={[styles.input, styles.multilineInput]}
                      value={decisionReason}
                      onChangeText={setDecisionReason}
                      maxLength={1000}
                      multiline
                      textAlignVertical="top"
                      placeholder="Registre os fundamentos da decisão"
                    />
                    <View style={styles.actions}>
                      <Pressable
                        disabled={working || !decisionReason.trim()}
                        onPress={() => confirmDecision(selected, 'REEMBOLSAR_CLIENTE')}
                        style={[styles.refundButton, (working || !decisionReason.trim()) && styles.disabled]}
                      >
                        <Text style={styles.decisionText}>{working ? 'Processando...' : 'Reembolsar cliente'}</Text>
                      </Pressable>
                      <Pressable
                        disabled={working || !decisionReason.trim()
                          || (selected.paymentStatus !== 'PAGO'
                            && selected.paymentStatus !== 'REPASSE_PENDENTE')}
                        onPress={() => confirmDecision(selected, 'PAGAR_PRESTADOR')}
                        style={[styles.payoutButton, (working || !decisionReason.trim()
                          || (selected.paymentStatus !== 'PAGO'
                            && selected.paymentStatus !== 'REPASSE_PENDENTE')) && styles.disabled]}
                      >
                        <Text style={styles.decisionText}>{working ? 'Processando...' : 'Pagar prestador'}</Text>
                      </Pressable>
                    </View>
                    <Text style={styles.muted}>
                      A disputa só é encerrada depois da confirmação integral do reembolso pelo Mercado Pago.
                    </Text>
                  </>
                ) : (
                  <>
                    <Text style={styles.line}>Decisão: {decisionLabel(selected.decision)}</Text>
                    <Text style={styles.line}>Justificativa: {selected.decisionReason}</Text>
                    <Text style={styles.line}>Administrador: {selected.administratorName}</Text>
                    {selected.closedAt ? <Text style={styles.line}>Encerrada: {formatDate(selected.closedAt)}</Text> : null}
                  </>
                )}
              </View>
            ) : null}
          </View>
        ))}
      </ScrollAreaView>
    </View>
  );
}

function AdminFilterButton({ label, selected, onPress }: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[styles.filter, selected && styles.selectedFilter]}
    >
      <Text style={[styles.filterText, selected && styles.selectedFilterText]}>{label}</Text>
    </Pressable>
  );
}

function decisionLabel(decision: ServiceDispute['decision']) {
  return decision === 'REEMBOLSAR_CLIENTE' ? 'Reembolsar cliente' : 'Pagar prestador';
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('pt-BR');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f6f6f6' },
  content: { padding: 16, paddingBottom: 32, gap: 12 },
  filters: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  filter: { backgroundColor: '#eee', borderRadius: 20, minHeight: 40,
    justifyContent: 'center', paddingHorizontal: 13, paddingVertical: 8 },
  selectedFilter: { backgroundColor: '#a43434' },
  filterText: { color: '#444', fontWeight: '600' },
  selectedFilterText: { color: '#fff' },
  intro: { color: '#444', fontSize: 14, lineHeight: 20 },
  muted: { color: '#666', fontSize: 13, lineHeight: 19 },
  warning: { color: '#854c00', fontSize: 13, lineHeight: 19, fontWeight: '600' },
  error: { color: '#a43434', fontSize: 14, lineHeight: 20 },
  card: { backgroundColor: '#fff', borderRadius: 12, padding: 16, gap: 8 },
  heading: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  title: { color: '#222', fontSize: 17, fontWeight: '700', flexShrink: 1 },
  badge: { color: '#854c00', backgroundColor: '#fff0d6', borderRadius: 12, paddingHorizontal: 9, paddingVertical: 4, fontSize: 12, fontWeight: '700' },
  closedBadge: { color: '#246b31', backgroundColor: '#e5f4e8' },
  line: { color: '#4a4a4a', fontSize: 14 },
  reason: { color: '#333', fontSize: 14, lineHeight: 20, marginTop: 4 },
  detailButton: { alignSelf: 'flex-start', paddingVertical: 6 },
  detailButtonText: { color: '#a43434', fontWeight: '700' },
  details: { borderTopWidth: 1, borderTopColor: '#eee', paddingTop: 10, gap: 9 },
  evidenceRow: { backgroundColor: '#f7f7f7', borderRadius: 8, padding: 10, gap: 4 },
  evidenceName: { color: '#a43434', fontSize: 14, fontWeight: '700' },
  deadline: { color: '#854c00', fontSize: 14, fontWeight: '700' },
  label: { color: '#333', fontSize: 13, fontWeight: '700' },
  input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 10, color: '#222', backgroundColor: '#fff' },
  multilineInput: { minHeight: 75 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  refundButton: { backgroundColor: '#a43434', borderRadius: 8, padding: 11 },
  payoutButton: { backgroundColor: '#286b3a', borderRadius: 8, padding: 11 },
  decisionText: { color: '#fff', fontWeight: '700' },
  disabled: { opacity: 0.55 },
});
