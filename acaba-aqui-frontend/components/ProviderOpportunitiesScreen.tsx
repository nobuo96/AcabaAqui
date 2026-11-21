import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { InAppNotificationInbox } from '@/components/InAppNotificationInbox';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { getAuthUserId } from '@/lib/authToken';
import {
  communicationApi,
  serviceQuoteApi,
  visitSlotApi,
  type RequestOpportunity,
  type ServiceQuote,
  type VisitSlot,
} from '@/lib/communicationApi';

export function ProviderOpportunitiesScreen() {
  const router = useRouter();
  const [opportunities, setOpportunities] = useState<RequestOpportunity[]>([]);
  const [visitSlotsByRequest, setVisitSlotsByRequest] = useState<Record<number, VisitSlot[]>>({});
  const [quotesByRequest, setQuotesByRequest] = useState<Record<number, ServiceQuote[]>>({});
  const [providerId, setProviderId] = useState<number | null>(null);
  const [quoteDescriptions, setQuoteDescriptions] = useState<Record<number, string>>({});
  const [quoteAmounts, setQuoteAmounts] = useState<Record<number, string>>({});
  const [quoteWarrantyDays, setQuoteWarrantyDays] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [workingSlotId, setWorkingSlotId] = useState<number | null>(null);
  const [sendingQuoteId, setSendingQuoteId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [items, authenticatedProviderId] = await Promise.all([
        communicationApi.listOpportunities(),
        getAuthUserId(),
      ]);
      if (!authenticatedProviderId) {
        throw new Error('Sessão não encontrada. Entre novamente.');
      }
      const currentProviderId = Number(authenticatedProviderId);
      setProviderId(currentProviderId);
      const slotsByRequest: Record<number, VisitSlot[]> = {};
      const quotesByRequest: Record<number, ServiceQuote[]> = {};
      await Promise.all(items.filter((item) => item.status === 'ACEITA').map(async (item) => {
        const [slots, quotes] = await Promise.all([
          visitSlotApi.list(item.solicitacaoId),
          serviceQuoteApi.list(item.solicitacaoId),
        ]);
        slotsByRequest[item.solicitacaoId] = slots;
        quotesByRequest[item.solicitacaoId] = quotes;
      }));
      setOpportunities(items);
      setVisitSlotsByRequest(slotsByRequest);
      setQuotesByRequest(quotesByRequest);
      await communicationApi.markOpportunitiesRead();
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as oportunidades.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function respond(item: RequestOpportunity, action: 'accept' | 'decline') {
    if (workingId !== null) {
      return;
    }
    setWorkingId(item.id);
    setError('');
    try {
      if (action === 'accept') {
        await communicationApi.acceptOpportunity(item.id);
        const slots = await visitSlotApi.list(item.solicitacaoId);
        setVisitSlotsByRequest((current) => ({ ...current, [item.solicitacaoId]: slots }));
        setOpportunities((current) => current.map((opportunity) => opportunity.id === item.id
          ? { ...opportunity, status: 'ACEITA' } : opportunity));
      } else {
        await communicationApi.declineOpportunity(item.id);
        setOpportunities((current) => current.map((opportunity) => opportunity.id === item.id
          ? { ...opportunity, status: 'RECUSADA' } : opportunity));
      }
    } catch (responseError) {
      setError(responseError instanceof Error ? responseError.message : 'Não foi possível responder à oportunidade.');
      await load();
    } finally {
      setWorkingId(null);
    }
  }

  async function updateVisit(slotId: number, action: 'reserve' | 'cancel') {
    if (workingSlotId !== null) {
      return;
    }
    setWorkingSlotId(slotId);
    setError('');
    try {
      if (action === 'reserve') {
        await visitSlotApi.reserve(slotId);
      } else {
        await visitSlotApi.cancel(slotId);
      }
      await load();
    } catch (visitError) {
      setError(visitError instanceof Error ? visitError.message : 'Não foi possível atualizar a visita.');
      await load();
    } finally {
      setWorkingSlotId(null);
    }
  }

  async function sendQuote(item: RequestOpportunity) {
    if (sendingQuoteId !== null) return;
    const description = quoteDescriptions[item.solicitacaoId] ?? '';
    const parsedAmount = Number((quoteAmounts[item.solicitacaoId] ?? '').replace(',', '.'));
    const warrantyDays = Number(quoteWarrantyDays[item.solicitacaoId] ?? '');
    if (!description.trim() || !Number.isFinite(parsedAmount) || parsedAmount <= 0
      || !Number.isInteger(warrantyDays) || warrantyDays <= 0) {
      setError('Informe a descrição, o valor total e a garantia em dias inteiros positivos.');
      return;
    }

    setSendingQuoteId(item.solicitacaoId);
    setError('');
    try {
      await serviceQuoteApi.send(item.solicitacaoId, {
        descricao: description.trim(),
        valorTotal: parsedAmount,
        garantiaDias: warrantyDays,
      });
      setQuoteDescriptions((current) => ({ ...current, [item.solicitacaoId]: '' }));
      setQuoteAmounts((current) => ({ ...current, [item.solicitacaoId]: '' }));
      setQuoteWarrantyDays((current) => ({ ...current, [item.solicitacaoId]: '' }));
      await load();
    } catch (sendError) {
      setError(sendError instanceof Error ? sendError.message : 'Não foi possível enviar o orçamento.');
      await load();
    } finally {
      setSendingQuoteId(null);
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Oportunidades" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <InAppNotificationInbox />
        {loading ? <Text style={styles.muted}>Carregando oportunidades...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && opportunities.length === 0
          ? <Text style={styles.muted}>Ainda não há oportunidades para seus serviços e regiões.</Text> : null}
        {opportunities.map((item) => (
          <View key={item.id} style={styles.card}>
            <View style={styles.heading}>
              <Text style={styles.title}>{item.servicoNome}</Text>
              <Text style={[styles.status, item.status !== 'PENDENTE' && styles.closedStatus]}>
                {statusLabel(item.status)}
              </Text>
            </View>
            <Text style={styles.detail}>Área aproximada: {formatArea(item.areaM2)} m²</Text>
            <Text style={styles.detail}>Região: {item.cidade} - {item.uf}</Text>
            <Text style={styles.date}>Recebida em {new Date(item.dataCriacao).toLocaleString('pt-BR')}</Text>
            {item.status === 'PENDENTE' ? (
              <View style={styles.actions}>
                <Pressable disabled={workingId !== null || workingSlotId !== null}
                  onPress={() => { void respond(item, 'accept'); }}
                  style={[styles.actionButton, styles.acceptButton]}>
                  <Text style={styles.actionText}>{workingId === item.id ? 'Aguarde...' : 'Aceitar oportunidade'}</Text>
                </Pressable>
                <Pressable disabled={workingId !== null || workingSlotId !== null}
                  onPress={() => { void respond(item, 'decline'); }}
                  style={[styles.actionButton, styles.declineButton]}>
                  <Text style={[styles.actionText, styles.declineText]}>Recusar</Text>
                </Pressable>
              </View>
            ) : null}
            {item.status === 'ACEITA' ? (
              <>
                <View style={styles.visitSection}>
                  <Text style={styles.visitHeading}>Horários de visita</Text>
                  <Text style={styles.muted}>O chat ficará disponível após o cliente confirmar.</Text>
                  {(visitSlotsByRequest[item.solicitacaoId] ?? []).length === 0
                    ? <Text style={styles.muted}>O cliente ainda não informou horários disponíveis.</Text>
                    : (visitSlotsByRequest[item.solicitacaoId] ?? []).map((slot) => (
                    <View key={slot.id} style={styles.slot}>
                      <View style={styles.slotInfo}>
                        <Text style={styles.detail}>{formatVisitTime(slot)}</Text>
                        <Text style={styles.slotStatus}>{visitStatusLabel(slot)}</Text>
                      </View>
                      {slot.status === 'DISPONIVEL' ? (
                        <Pressable
                          disabled={workingSlotId !== null || workingId !== null}
                          onPress={() => { void updateVisit(slot.id, 'reserve'); }}
                          style={styles.slotButton}
                        >
                          <Text style={styles.slotButtonText}>
                            {workingSlotId === slot.id ? 'Reservando...' : 'Reservar'}
                          </Text>
                        </Pressable>
                      ) : null}
                      {(slot.status === 'RESERVADA' || slot.status === 'CONFIRMADA')
                        && slot.prestadorId !== null ? (
                          <View style={styles.slotActions}>
                            {slot.status === 'CONFIRMADA' ? (
                              <Pressable onPress={() => router.push('/(prestador)/conversas')}>
                                <Text style={styles.link}>Abrir conversas</Text>
                              </Pressable>
                            ) : null}
                            <Pressable
                              disabled={workingSlotId !== null || workingId !== null}
                              onPress={() => { void updateVisit(slot.id, 'cancel'); }}
                            >
                              <Text style={styles.cancelVisit}>
                                {workingSlotId === slot.id ? 'Cancelando...' : 'Cancelar visita'}
                              </Text>
                            </Pressable>
                          </View>
                        ) : null}
                    </View>
                    ))}
                </View>
                <View style={styles.quoteSection}>
                  <Text style={styles.visitHeading}>Orçamentos</Text>
                  {(quotesByRequest[item.solicitacaoId] ?? []).length === 0
                    ? <Text style={styles.muted}>Você ainda não enviou um orçamento.</Text>
                    : (quotesByRequest[item.solicitacaoId] ?? []).map((quote) => (
                      <View key={quote.id} style={styles.quoteCard}>
                        <Text style={styles.quotePrice}>{formatCurrency(quote.valorTotal)}</Text>
                        <Text style={styles.detail}>{quote.descricao}</Text>
                        <Text style={styles.detail}>
                          {quote.garantiaDias > 0
                            ? `Garantia: ${quote.garantiaDias} dias corridos após a aprovação`
                            : 'Garantia não configurada neste orçamento'}
                        </Text>
                        <Text style={styles.slotStatus}>{quoteStatusLabel(quote.status)}</Text>
                        <Text style={styles.date}>Válido até {formatDate(quote.expiraEm)}</Text>
                      </View>
                    ))}
                  {(visitSlotsByRequest[item.solicitacaoId] ?? []).some((slot) =>
                    slot.status === 'EXPIRADA' && slot.prestadorId === providerId)
                    && !(quotesByRequest[item.solicitacaoId] ?? []).some((quote) =>
                      quote.status === 'ENVIADO' || quote.status === 'APROVADO') ? (
                    <View style={styles.quoteForm}>
                      <Text style={styles.muted}>A visita terminou. Envie seu orçamento em até 7 dias.</Text>
                      <TextInput
                        value={quoteDescriptions[item.solicitacaoId] ?? ''}
                        onChangeText={(value) => setQuoteDescriptions((current) => ({
                          ...current,
                          [item.solicitacaoId]: value,
                        }))}
                        multiline
                        maxLength={2000}
                        placeholder="Descrição do serviço e materiais"
                        style={[styles.input, styles.descriptionInput]}
                      />
                      <TextInput
                        value={quoteAmounts[item.solicitacaoId] ?? ''}
                        onChangeText={(value) => setQuoteAmounts((current) => ({
                          ...current,
                          [item.solicitacaoId]: value,
                        }))}
                        keyboardType="decimal-pad"
                        placeholder="Valor total (R$)"
                        style={styles.input}
                      />
                      <TextInput
                        value={quoteWarrantyDays[item.solicitacaoId] ?? ''}
                        onChangeText={(value) => setQuoteWarrantyDays((current) => ({
                          ...current,
                          [item.solicitacaoId]: value.replace(/[^0-9]/g, ''),
                        }))}
                        keyboardType="number-pad"
                        placeholder="Garantia (dias corridos)"
                        style={styles.input}
                      />
                      <Pressable
                        disabled={sendingQuoteId !== null}
                        onPress={() => { void sendQuote(item); }}
                        style={styles.slotButton}
                      >
                        <Text style={styles.slotButtonText}>
                          {sendingQuoteId === item.solicitacaoId ? 'Enviando...' : 'Enviar orçamento'}
                        </Text>
                      </Pressable>
                    </View>
                  ) : null}
                </View>
              </>
            ) : null}
          </View>
        ))}
      </ScrollAreaView>
    </View>
  );
}

function statusLabel(status: RequestOpportunity['status']) {
  return {
    PENDENTE: 'Nova',
    ACEITA: 'Aceita',
    RECUSADA: 'Recusada',
    ENCERRADA: 'Encerrada',
  }[status];
}

function formatVisitTime(slot: VisitSlot) {
  const [date, time] = slot.inicioLocal.split('T');
  return `${date} às ${time.slice(0, 5)} (${slot.timeZoneId})`;
}

function visitStatusLabel(slot: VisitSlot) {
  if (slot.status === 'DISPONIVEL') return 'Disponível';
  if (slot.status === 'RESERVADA') {
    return slot.prestadorId === null ? 'Reservado por outro prestador' : 'Aguardando confirmação do cliente';
  }
  if (slot.status === 'CONFIRMADA') {
    return slot.prestadorId === null ? 'Confirmado por outro prestador' : 'Confirmada pelo cliente';
  }
  return slot.status === 'CANCELADA' ? 'Cancelada' : 'Expirada';
}

function quoteStatusLabel(status: ServiceQuote['status']) {
  return {
    ENVIADO: 'Aguardando decisão do cliente',
    APROVADO: 'Aprovado',
    RECUSADO: 'Recusado',
    EXPIRADO: 'Expirado',
    CANCELADO: 'Cancelado',
  }[status];
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function formatArea(value: number) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(value);
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, gap: 14 },
  card: { borderColor: '#e1e1e1', borderRadius: 12, borderWidth: 1, gap: 9, padding: 14 },
  heading: { alignItems: 'center', flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
  title: { color: '#222', flex: 1, fontSize: 17, fontWeight: '700' },
  status: { backgroundColor: '#edf4eb', borderRadius: 12, color: '#426341', fontSize: 12,
    overflow: 'hidden', paddingHorizontal: 9, paddingVertical: 4 },
  closedStatus: { backgroundColor: '#eee', color: '#666' },
  detail: { color: '#444', lineHeight: 21 },
  date: { color: '#777', fontSize: 13 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 4 },
  actionButton: { alignItems: 'center', borderRadius: 9, flex: 1, padding: 11 },
  acceptButton: { backgroundColor: '#a43434' },
  declineButton: { borderColor: '#a43434', borderWidth: 1 },
  actionText: { color: '#fff', fontWeight: '700' },
  declineText: { color: '#a43434' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
  visitSection: { borderTopColor: '#eee', borderTopWidth: 1, gap: 9, marginTop: 4, paddingTop: 10 },
  quoteSection: { borderTopColor: '#eee', borderTopWidth: 1, gap: 9, marginTop: 4, paddingTop: 10 },
  quoteCard: { backgroundColor: '#fafafa', borderRadius: 9, gap: 5, padding: 10 },
  quotePrice: { color: '#333', fontSize: 16, fontWeight: '700' },
  quoteForm: { gap: 9, marginTop: 4 },
  input: { borderColor: '#ccc', borderRadius: 8, borderWidth: 1, color: '#222', padding: 11 },
  descriptionInput: { minHeight: 90, textAlignVertical: 'top' },
  visitHeading: { color: '#333', fontSize: 15, fontWeight: '700' },
  slot: { alignItems: 'center', borderColor: '#e1e1e1', borderRadius: 9, borderWidth: 1,
    flexDirection: 'row', gap: 8, justifyContent: 'space-between', padding: 10 },
  slotInfo: { flex: 1, gap: 4 },
  slotStatus: { color: '#666', fontSize: 12 },
  slotButton: { backgroundColor: '#a43434', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 9 },
  slotButtonText: { color: '#fff', fontWeight: '700' },
  slotActions: { alignItems: 'flex-end', gap: 8 },
  link: { color: '#a43434', fontWeight: '700' },
  cancelVisit: { color: '#a43434', fontWeight: '600' },
});
