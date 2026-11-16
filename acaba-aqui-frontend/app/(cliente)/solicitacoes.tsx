import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { InAppNotificationInbox } from '@/components/InAppNotificationInbox';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { getAuthUserId } from '@/lib/authToken';
import { serviceRequestApi, type ServiceRequest } from '@/lib/serviceRequestApi';
import {
  servicePaymentApi,
  serviceQuoteApi,
  visitSlotApi,
  type ServicePayment,
  type ServiceQuote,
  type VisitSlot,
} from '@/lib/communicationApi';
import * as WebBrowser from 'expo-web-browser';

type Filter = 'todas' | 'abertas';

export default function SolicitacoesScreen() {
  const router = useRouter();
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [visitSlotsByRequest, setVisitSlotsByRequest] = useState<Record<number, VisitSlot[]>>({});
  const [quotesByRequest, setQuotesByRequest] = useState<Record<number, ServiceQuote[]>>({});
  const [paymentsByQuote, setPaymentsByQuote] = useState<Record<number, ServicePayment>>({});
  const [filter, setFilter] = useState<Filter>('todas');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [workingVisitId, setWorkingVisitId] = useState<number | null>(null);
  const [workingQuoteId, setWorkingQuoteId] = useState<number | null>(null);
  const [workingPaymentQuoteId, setWorkingPaymentQuoteId] = useState<number | null>(null);
  const [confirmingPaymentQuoteId, setConfirmingPaymentQuoteId] = useState<number | null>(null);

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const userId = await getAuthUserId();
      if (!userId) {
        throw new Error('Sessão não encontrada. Entre novamente.');
      }
      const items = await serviceRequestApi.list(userId);
      const slotsByRequest: Record<number, VisitSlot[]> = {};
      const quotesByRequest: Record<number, ServiceQuote[]> = {};
      const paymentsByQuote: Record<number, ServicePayment> = {};
      await Promise.all(items.map(async (request) => {
        const [slots, quotes] = await Promise.all([
          visitSlotApi.list(request.id),
          serviceQuoteApi.list(request.id),
        ]);
        await Promise.all(quotes
          .filter((quote) => quote.status === 'APROVADO')
          .map(async (quote) => {
            const lookup = await servicePaymentApi.getForQuote(quote.id);
            if (lookup.payment) {
              paymentsByQuote[quote.id] = lookup.payment;
            }
          }));
        slotsByRequest[request.id] = slots;
        quotesByRequest[request.id] = quotes;
      }));
      setRequests(items);
      setVisitSlotsByRequest(slotsByRequest);
      setQuotesByRequest(quotesByRequest);
      setPaymentsByQuote(paymentsByQuote);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as solicitações.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void loadRequests();
  }, [loadRequests]));

  async function cancelRequest(requestId: number) {
    setCancellingId(requestId);
    setError('');
    try {
      const userId = await getAuthUserId();
      if (!userId) {
        throw new Error('Sessão não encontrada. Entre novamente.');
      }
      await serviceRequestApi.cancel(userId, requestId);
      setConfirmingId(null);
      await loadRequests();
    } catch (cancelError) {
      setError(cancelError instanceof Error ? cancelError.message : 'Não foi possível cancelar a solicitação.');
    } finally {
      setCancellingId(null);
    }
  }

  async function payQuote(quoteId: number) {
    if (workingPaymentQuoteId !== null) return;
    setWorkingPaymentQuoteId(quoteId);
    setError('');
    try {
      const payment = await servicePaymentApi.startCheckout(quoteId);
      setPaymentsByQuote((current) => ({ ...current, [quoteId]: payment }));
      if (payment.status === 'AGUARDANDO_PAGAMENTO' && payment.checkoutUrl) {
        await WebBrowser.openBrowserAsync(payment.checkoutUrl);
      }
      const lookup = await servicePaymentApi.getForQuote(quoteId);
      setPaymentsByQuote((current) => lookup.payment
        ? { ...current, [quoteId]: lookup.payment }
        : current);
    } catch (paymentError) {
      setError(paymentError instanceof Error ? paymentError.message : 'Não foi possível iniciar o pagamento.');
    } finally {
      setWorkingPaymentQuoteId(null);
    }
  }

  async function cancelPayment(quoteId: number) {
    const payment = paymentsByQuote[quoteId];
    if (!payment || workingPaymentQuoteId !== null) return;
    setWorkingPaymentQuoteId(quoteId);
    setError('');
    try {
      const updatedPayment = await servicePaymentApi.cancel(payment.id);
      setPaymentsByQuote((current) => ({ ...current, [quoteId]: updatedPayment }));
      setConfirmingPaymentQuoteId(null);
    } catch (paymentError) {
      const message = paymentError instanceof Error
        ? paymentError.message
        : 'Não foi possível cancelar o checkout.';
      await loadRequests();
      setError(message);
    } finally {
      setWorkingPaymentQuoteId(null);
    }
  }

  async function respondToVisit(slot: VisitSlot, action: 'confirm' | 'cancel') {
    if (workingVisitId !== null) {
      return;
    }
    setWorkingVisitId(slot.id);
    setError('');
    try {
      if (action === 'confirm') {
        const result = await visitSlotApi.confirm(slot.id);
        await loadRequests();
        router.push({
          pathname: '/chat/[conversationId]',
          params: { conversationId: String(result.conversationId) },
        });
      } else {
        await visitSlotApi.cancel(slot.id);
        await loadRequests();
      }
    } catch (visitError) {
      setError(visitError instanceof Error ? visitError.message : 'Não foi possível atualizar a visita.');
      await loadRequests();
    } finally {
      setWorkingVisitId(null);
    }
  }

  async function decideQuote(quoteId: number, action: 'approve' | 'reject') {
    if (workingQuoteId !== null) return;
    setWorkingQuoteId(quoteId);
    setError('');
    try {
      if (action === 'approve') {
        await serviceQuoteApi.approve(quoteId);
      } else {
        await serviceQuoteApi.reject(quoteId);
      }
      await loadRequests();
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : 'Não foi possível atualizar o orçamento.');
      await loadRequests();
    } finally {
      setWorkingQuoteId(null);
    }
  }

  const visibleRequests = filter === 'abertas'
    ? requests.filter((request) => request.status !== 'ENCERRADA')
    : requests;

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Minhas solicitações" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <InAppNotificationInbox />
        <AcabaAquiButton
          text="Nova solicitação"
          onPress={() => router.push('/cliente/novaSolicitacao')}
        />
        <View style={styles.filters}>
          <FilterButton label={`Todas (${requests.length})`} selected={filter === 'todas'}
            onPress={() => setFilter('todas')} />
          <FilterButton label={`Abertas (${requests.filter((item) => item.status !== 'ENCERRADA').length})`}
            selected={filter === 'abertas'} onPress={() => setFilter('abertas')} />
        </View>

        {loading ? <Text style={styles.muted}>Carregando solicitações...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && !error && visibleRequests.length === 0 ? (
          <Text style={styles.muted}>
            {filter === 'abertas' ? 'Você não tem solicitações abertas.' : 'Você ainda não criou solicitações.'}
          </Text>
        ) : null}

        {!loading ? visibleRequests.map((request) => (
          <View key={request.id} style={styles.card}>
            <View style={styles.cardHeading}>
              <Text style={styles.cardTitle}>{request.servicoNome}</Text>
              <Text style={[styles.status, request.status === 'ENCERRADA' && styles.closedStatus]}>
                {request.status === 'ENCERRADA' ? 'Encerrada' : 'Criada'}
              </Text>
            </View>
            <Text style={styles.detail}>Área: {formatArea(request.areaM2)} m²</Text>
            <Text style={styles.detail}>{formatAddress(request)}</Text>
            <Text style={styles.date}>Criada em {formatDate(request.dataCriacao)}</Text>

            <View style={styles.visits}>
              <Text style={styles.visitHeading}>Horários de visita</Text>
              {(visitSlotsByRequest[request.id] ?? []).length === 0
                ? <Text style={styles.muted}>Nenhum horário disponível cadastrado.</Text>
                : (visitSlotsByRequest[request.id] ?? []).map((slot) => (
                  <View key={slot.id} style={styles.visitCard}>
                    <View style={styles.visitInfo}>
                      <Text style={styles.detail}>{formatVisitTime(slot)}</Text>
                      <Text style={styles.muted}>{visitStatusLabel(slot)}</Text>
                      {slot.prestadorNome ? (
                        <Text style={styles.muted}>Prestador: {slot.prestadorNome}</Text>
                      ) : null}
                    </View>
                    {request.status !== 'ENCERRADA' && slot.status === 'RESERVADA' ? (
                      <View style={styles.visitActions}>
                        <Pressable
                          disabled={workingVisitId !== null}
                          onPress={() => { void respondToVisit(slot, 'confirm'); }}
                        >
                          <Text style={styles.action}>
                            {workingVisitId === slot.id ? 'Confirmando...' : 'Confirmar visita'}
                          </Text>
                        </Pressable>
                        <Pressable
                          disabled={workingVisitId !== null}
                          onPress={() => { void respondToVisit(slot, 'cancel'); }}
                        >
                          <Text style={styles.dangerAction}>Cancelar visita</Text>
                        </Pressable>
                      </View>
                    ) : null}
                    {request.status !== 'ENCERRADA' && slot.status === 'CONFIRMADA' ? (
                      <View style={styles.visitActions}>
                        <Pressable onPress={() => router.push('/(cliente)/conversas')}>
                          <Text style={styles.action}>Abrir conversa</Text>
                        </Pressable>
                        <Pressable
                          disabled={workingVisitId !== null}
                          onPress={() => { void respondToVisit(slot, 'cancel'); }}
                        >
                          <Text style={styles.dangerAction}>Cancelar visita</Text>
                        </Pressable>
                      </View>
                    ) : null}
                  </View>
                ))}
            </View>

            <View style={styles.quotes}>
              <Text style={styles.visitHeading}>Orçamentos recebidos</Text>
              {(quotesByRequest[request.id] ?? []).length === 0
                ? <Text style={styles.muted}>Você ainda não recebeu orçamentos.</Text>
                : (quotesByRequest[request.id] ?? []).map((quote) => (
                  <View key={quote.id} style={styles.quoteCard}>
                    <View style={styles.quoteHeading}>
                      <Text style={styles.cardTitle}>{quote.prestadorNome}</Text>
                      <Text style={[styles.status, quote.status !== 'ENVIADO' && styles.closedStatus]}>
                        {quoteStatusLabel(quote.status)}
                      </Text>
                    </View>
                    <Text style={styles.quotePrice}>{formatCurrency(quote.valorTotal)}</Text>
                    <Text style={styles.detail}>{quote.descricao}</Text>
                    <Text style={styles.date}>Enviado em {formatDate(quote.dataCriacao)}</Text>
                    {quote.status === 'ENVIADO' ? (
                      <>
                        <Text style={styles.date}>Válido até {formatDate(quote.expiraEm)}</Text>
                        {request.status !== 'ENCERRADA' ? (
                          <View style={styles.quoteActions}>
                            <Pressable
                              disabled={workingQuoteId !== null}
                              onPress={() => { void decideQuote(quote.id, 'approve'); }}
                              style={[styles.quoteButton, styles.approveButton]}
                            >
                              <Text style={styles.approveText}>
                                {workingQuoteId === quote.id ? 'Aguarde...' : 'Aprovar orçamento'}
                              </Text>
                            </Pressable>
                            <Pressable
                              disabled={workingQuoteId !== null}
                              onPress={() => { void decideQuote(quote.id, 'reject'); }}
                              style={[styles.quoteButton, styles.rejectButton]}
                            >
                              <Text style={styles.rejectText}>
                                {workingQuoteId === quote.id ? 'Aguarde...' : 'Recusar'}
                              </Text>
                            </Pressable>
                          </View>
                        ) : null}
                      </>
                    ) : null}
                    {quote.status === 'APROVADO' ? (
                      <View style={styles.paymentInfo}>
                        {paymentsByQuote[quote.id] ? (
                          <>
                            <Text style={styles.muted}>
                              Pagamento: {paymentStatusLabel(paymentsByQuote[quote.id].status)}
                            </Text>
                            {paymentsByQuote[quote.id].status === 'AGUARDANDO_PAGAMENTO'
                              && paymentsByQuote[quote.id].checkoutUrl ? (
                              <>
                                <Pressable
                                  disabled={workingPaymentQuoteId !== null}
                                  onPress={() => { void payQuote(quote.id); }}
                                >
                                  <Text style={styles.action}>Retomar pagamento</Text>
                                </Pressable>
                                {confirmingPaymentQuoteId === quote.id ? (
                                  <View style={styles.paymentConfirmation}>
                                    <Text style={styles.muted}>Cancelar apenas esta tentativa de pagamento?</Text>
                                    <Pressable
                                      disabled={workingPaymentQuoteId !== null}
                                      onPress={() => { void cancelPayment(quote.id); }}
                                    >
                                      <Text style={styles.dangerAction}>
                                        {workingPaymentQuoteId === quote.id ? 'Cancelando...' : 'Confirmar cancelamento'}
                                      </Text>
                                    </Pressable>
                                    <Pressable onPress={() => setConfirmingPaymentQuoteId(null)}>
                                      <Text style={styles.action}>Manter pagamento</Text>
                                    </Pressable>
                                  </View>
                                ) : (
                                  <Pressable onPress={() => setConfirmingPaymentQuoteId(quote.id)}>
                                    <Text style={styles.dangerAction}>Cancelar checkout</Text>
                                  </Pressable>
                                )}
                              </>
                            ) : null}
                            {['RECUSADO', 'EXPIRADO', 'CANCELADO'].includes(paymentsByQuote[quote.id].status) ? (
                              <Pressable
                                disabled={workingPaymentQuoteId !== null}
                                onPress={() => { void payQuote(quote.id); }}
                                style={[styles.quoteButton, styles.approveButton]}
                              >
                                <Text style={styles.approveText}>
                                  {workingPaymentQuoteId === quote.id ? 'Preparando pagamento...' : 'Tentar novamente'}
                                </Text>
                              </Pressable>
                            ) : null}
                          </>
                        ) : (
                          <Pressable
                            disabled={workingPaymentQuoteId !== null}
                            onPress={() => { void payQuote(quote.id); }}
                            style={[styles.quoteButton, styles.approveButton]}
                          >
                            <Text style={styles.approveText}>
                              {workingPaymentQuoteId === quote.id ? 'Preparando pagamento...' : 'Pagar orçamento'}
                            </Text>
                          </Pressable>
                        )}
                      </View>
                    ) : null}
                  </View>
                ))}
            </View>

            {request.status !== 'ENCERRADA' ? (
              confirmingId === request.id ? (
                <View style={styles.confirmation}>
                  <Text style={styles.confirmText}>Deseja encerrar esta solicitação?</Text>
                  <Pressable
                    disabled={cancellingId === request.id}
                    onPress={() => { void cancelRequest(request.id); }}
                  >
                    <Text style={styles.dangerAction}>
                      {cancellingId === request.id ? 'Encerrando...' : 'Confirmar cancelamento'}
                    </Text>
                  </Pressable>
                  <Pressable onPress={() => setConfirmingId(null)}>
                    <Text style={styles.action}>Manter solicitação</Text>
                  </Pressable>
                </View>
              ) : (
                <Pressable onPress={() => setConfirmingId(request.id)}>
                  <Text style={styles.dangerAction}>Cancelar solicitação</Text>
                </Pressable>
              )
            ) : null}
          </View>
        )) : null}
      </ScrollAreaView>
    </View>
  );
}

function FilterButton({ label, selected, onPress }: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable onPress={onPress} style={[styles.filter, selected && styles.selectedFilter]}>
      <Text style={[styles.filterText, selected && styles.selectedFilterText]}>{label}</Text>
    </Pressable>
  );
}

function formatArea(area: number) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(area);
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function formatVisitTime(slot: VisitSlot) {
  const [date, time] = slot.inicioLocal.split('T');
  return `${date} às ${time.slice(0, 5)} (${slot.timeZoneId})`;
}

function visitStatusLabel(slot: VisitSlot) {
  if (slot.status === 'DISPONIVEL') return 'Aguardando escolha do prestador';
  if (slot.status === 'RESERVADA') return 'Aguardando sua confirmação';
  if (slot.status === 'CONFIRMADA') return 'Confirmada';
  return slot.status === 'CANCELADA' ? 'Cancelada' : 'Expirada';
}

function quoteStatusLabel(status: ServiceQuote['status']) {
  return {
    ENVIADO: 'Aguardando decisão',
    APROVADO: 'Aprovado',
    RECUSADO: 'Recusado',
    EXPIRADO: 'Expirado',
    CANCELADO: 'Cancelado',
  }[status];
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function paymentStatusLabel(status: ServicePayment['status']) {
  return {
    AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
    PAGO: 'Confirmado',
    RECUSADO: 'Não aprovado',
    EXPIRADO: 'Expirado',
    CANCELADO: 'Cancelado',
    REEMBOLSO_PENDENTE: 'Reembolso em processamento',
    REEMBOLSADO: 'Reembolsado',
    REEMBOLSADO_PARCIAL: 'Reembolso parcial',
    CONTESTADO: 'Em contestação',
  }[status];
}

function formatAddress(request: ServiceRequest) {
  const address = request.endereco;
  const street = [address.logradouro, address.numero, address.complemento, address.bairro]
    .filter(Boolean).join(', ');
  return [address.apelido, street, `${address.cidade} - ${address.uf}`].filter(Boolean).join(' · ');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { paddingHorizontal: 20, paddingVertical: 18, gap: 14 },
  filters: { flexDirection: 'row', gap: 8 },
  filter: { borderWidth: 1, borderColor: '#ccc', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8 },
  selectedFilter: { backgroundColor: '#a43434', borderColor: '#a43434' },
  filterText: { color: '#555' },
  selectedFilterText: { color: '#fff', fontWeight: '600' },
  card: { borderWidth: 1, borderColor: '#e1e1e1', borderRadius: 12, gap: 9, padding: 14 },
  cardHeading: { alignItems: 'flex-start', flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
  cardTitle: { color: '#222', flex: 1, fontSize: 17, fontWeight: '700' },
  status: { backgroundColor: '#edf4eb', borderRadius: 12, color: '#426341', fontSize: 12, overflow: 'hidden', paddingHorizontal: 9, paddingVertical: 4 },
  closedStatus: { backgroundColor: '#eee', color: '#666' },
  detail: { color: '#444', lineHeight: 21 },
  date: { color: '#777', fontSize: 13 },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
  confirmation: { borderTopColor: '#eee', borderTopWidth: 1, gap: 8, paddingTop: 10 },
  confirmText: { color: '#333' },
  dangerAction: { color: '#a43434', fontWeight: '700', paddingVertical: 4 },
  action: { color: '#555', fontWeight: '600', paddingVertical: 4 },
  visits: { borderTopColor: '#eee', borderTopWidth: 1, gap: 8, marginTop: 2, paddingTop: 10 },
  quotes: { borderTopColor: '#eee', borderTopWidth: 1, gap: 8, marginTop: 2, paddingTop: 10 },
  quoteCard: { backgroundColor: '#fafafa', borderRadius: 9, gap: 7, padding: 11 },
  quoteHeading: { alignItems: 'center', flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
  quotePrice: { color: '#333', fontSize: 17, fontWeight: '700' },
  quoteActions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
  paymentInfo: { borderTopColor: '#eee', borderTopWidth: 1, gap: 6, marginTop: 4, paddingTop: 8 },
  paymentConfirmation: { borderTopColor: '#eee', borderTopWidth: 1, gap: 4, paddingTop: 6 },
  quoteButton: { alignItems: 'center', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 9 },
  approveButton: { backgroundColor: '#a43434' },
  rejectButton: { borderColor: '#a43434', borderWidth: 1 },
  approveText: { color: '#fff', fontWeight: '700' },
  rejectText: { color: '#a43434', fontWeight: '700' },
  visitHeading: { color: '#333', fontSize: 15, fontWeight: '700' },
  visitCard: { alignItems: 'flex-start', borderColor: '#e1e1e1', borderRadius: 9, borderWidth: 1,
    gap: 8, padding: 10 },
  visitInfo: { gap: 4 },
  visitActions: { flexDirection: 'row', flexWrap: 'wrap', gap: 14 },
});
