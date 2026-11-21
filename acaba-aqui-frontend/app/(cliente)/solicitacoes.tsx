import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
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

type Filter = 'atencao' | 'andamento' | 'encerradas';

export default function SolicitacoesScreen() {
  const router = useRouter();
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [visitSlotsByRequest, setVisitSlotsByRequest] = useState<Record<number, VisitSlot[]>>({});
  const [quotesByRequest, setQuotesByRequest] = useState<Record<number, ServiceQuote[]>>({});
  const [paymentsByQuote, setPaymentsByQuote] = useState<Record<number, ServicePayment>>({});
  const [filter, setFilter] = useState<Filter>('atencao');
  const [expandedRequestId, setExpandedRequestId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [workingVisitId, setWorkingVisitId] = useState<number | null>(null);
  const [workingQuoteId, setWorkingQuoteId] = useState<number | null>(null);
  const [workingPaymentQuoteId, setWorkingPaymentQuoteId] = useState<number | null>(null);
  const [confirmingPaymentQuoteId, setConfirmingPaymentQuoteId] = useState<number | null>(null);
  const [serviceCancellationQuoteId, setServiceCancellationQuoteId] = useState<number | null>(null);
  const [cancellationReasons, setCancellationReasons] = useState<Record<number, string>>({});
  const [cancellationResults, setCancellationResults] = useState<Record<number, string>>({});
  const [confirmingQuoteId, setConfirmingQuoteId] = useState<number | null>(null);

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

  async function cancelPaidService(quoteId: number) {
    const payment = paymentsByQuote[quoteId];
    if (!payment || workingPaymentQuoteId !== null) return;
    setWorkingPaymentQuoteId(quoteId);
    setError('');
    try {
      const result = await servicePaymentApi.cancelService(
        payment.id,
        cancellationReasons[quoteId] ?? '',
      );
      setPaymentsByQuote((current) => ({ ...current, [quoteId]: result.payment }));
      setCancellationResults((current) => ({ ...current, [quoteId]: result.result }));
      if (result.status === 'REEMBOLSADO') {
        setServiceCancellationQuoteId(null);
      }
    } catch (cancellationError) {
      const message = cancellationError instanceof Error
        ? cancellationError.message
        : 'Não foi possível solicitar o cancelamento do serviço.';
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

  const needsAttention = (request: ServiceRequest) => Boolean(getRequestNextAction(
    request.status,
    visitSlotsByRequest[request.id] ?? [],
    quotesByRequest[request.id] ?? [],
    paymentsByQuote,
  ));
  const attentionCount = requests.filter((request) => request.status !== 'ENCERRADA'
    && needsAttention(request)).length;
  const inProgressCount = requests.filter((request) => request.status !== 'ENCERRADA'
    && !needsAttention(request)).length;
  const closedCount = requests.filter((request) => request.status === 'ENCERRADA').length;
  const visibleRequests = requests.filter((request) => {
    if (filter === 'atencao') return request.status !== 'ENCERRADA' && needsAttention(request);
    if (filter === 'andamento') return request.status !== 'ENCERRADA' && !needsAttention(request);
    return request.status === 'ENCERRADA';
  });

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Minhas solicitações" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <InAppNotificationInbox />
        <AcabaAquiButton
          text="Nova solicitação"
          onPress={() => router.push('/cliente/novaSolicitacao')}
        />
        <View style={styles.filters} accessibilityRole="tablist">
          <FilterButton label={`Precisa de você · ${attentionCount}`} selected={filter === 'atencao'}
            onPress={() => setFilter('atencao')} />
          <FilterButton label={`Em andamento · ${inProgressCount}`} selected={filter === 'andamento'}
            onPress={() => setFilter('andamento')} />
          <FilterButton label={`Encerradas · ${closedCount}`} selected={filter === 'encerradas'}
            onPress={() => setFilter('encerradas')} />
        </View>

        {loading ? <Text style={styles.muted}>Carregando solicitações...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && !error && visibleRequests.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>
              {filter === 'atencao' ? 'Tudo em dia por aqui' : filter === 'andamento'
                ? 'Nenhuma solicitação em andamento' : 'Nenhuma solicitação encerrada'}
            </Text>
            <Text style={styles.muted}>
              {filter === 'atencao'
                ? 'Não há solicitações aguardando uma ação sua no momento. Confira as que estão em andamento.'
                : filter === 'andamento'
                  ? 'Solicitações sem ações pendentes aparecerão aqui.'
                  : 'Quando uma solicitação for encerrada, você poderá encontrá-la aqui.'}
            </Text>
            {filter === 'atencao' && inProgressCount > 0 ? (
              <Pressable onPress={() => setFilter('andamento')} accessibilityRole="button">
                <Text style={styles.action}>Ver solicitações em andamento</Text>
              </Pressable>
            ) : null}
          </View>
        ) : null}

        {!loading ? visibleRequests.map((request) => {
          const requestSlots = visitSlotsByRequest[request.id] ?? [];
          const requestQuotes = quotesByRequest[request.id] ?? [];
          const nextAction = getRequestNextAction(
            request.status,
            requestSlots,
            requestQuotes,
            paymentsByQuote,
          );
          const expanded = expandedRequestId === request.id;
          const activeSlotCount = requestSlots.filter((slot) =>
            ['DISPONIVEL', 'RESERVADA', 'CONFIRMADA'].includes(slot.status)).length;
          const newQuoteCount = requestQuotes.filter((quote) => quote.status === 'ENVIADO').length;
          const progressHint = request.status === 'ENCERRADA'
            ? 'Esta solicitação foi encerrada.'
            : requestQuotes.some((quote) =>
              quote.status === 'APROVADO' && paymentsByQuote[quote.id]?.status === 'PAGO')
              ? 'Pagamento confirmado. Acompanhe o serviço na aba Serviços.'
              : requestSlots.some((slot) => slot.status === 'CONFIRMADA')
                ? 'Visita confirmada. Aguarde o contato do prestador e novos orçamentos.'
                : 'A solicitação está aguardando retorno dos prestadores.';
          return (
          <View key={request.id} style={[styles.card, expanded && styles.expandedCard]}>
            <View style={styles.summaryHeader}>
              <View style={styles.summaryText}>
                <Text style={styles.cardTitle}>{request.servicoNome}</Text>
                <Text style={styles.summaryLocation}>{formatShortAddress(request)}</Text>
              </View>
              <Text style={[
                styles.status,
                request.status === 'ENCERRADA' ? styles.closedStatus : nextAction
                  ? styles.attentionStatus : styles.progressStatus,
              ]}>
                {request.status === 'ENCERRADA' ? 'Encerrada' : nextAction ? 'Precisa de você' : 'Em andamento'}
              </Text>
            </View>
            <Text style={styles.summaryMeta}>
              {formatArea(request.areaM2)} m²
              {' · '}{activeSlotCount} {activeSlotCount === 1 ? 'horário ativo' : 'horários ativos'}
              {' · '}{newQuoteCount} {newQuoteCount === 1 ? 'novo orçamento' : 'novos orçamentos'}
            </Text>
            <Text style={[styles.nextAction, !nextAction && styles.muted]}>
              {nextAction ?? progressHint}
            </Text>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ expanded }}
              onPress={() => setExpandedRequestId(expanded ? null : request.id)}
              style={styles.detailsToggle}
            >
              <Text style={styles.detailsToggleText}>
                {expanded ? 'Ocultar detalhes' : 'Ver detalhes e ações'}
              </Text>
              <Text style={styles.detailsChevron}>{expanded ? '−' : '+'}</Text>
            </Pressable>

            {expanded ? <>
            <Text style={styles.detail}>Endereço: {formatAddress(request)}</Text>
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
                    <Text style={styles.detail}>
                      {quote.garantiaDias > 0
                        ? `Garantia: ${quote.garantiaDias} dias corridos após a aprovação`
                        : 'Garantia não configurada neste orçamento'}
                    </Text>
                    <Text style={styles.date}>Enviado em {formatDate(quote.dataCriacao)}</Text>
                    {quote.status === 'ENVIADO' ? (
                      <>
                        <Text style={styles.date}>Válido até {formatDate(quote.expiraEm)}</Text>
                        {request.status !== 'ENCERRADA' ? (
                          <View style={styles.quoteActions}>
                            {confirmingQuoteId === quote.id ? (
                              <View style={styles.quoteConfirmation}>
                                <Text style={styles.confirmText}>
                                  Aprovar com {quote.prestadorNome} por {formatCurrency(quote.valorTotal)}?
                                  {' '}A garantia será de {quote.garantiaDias} dias corridos.
                                </Text>
                                <Pressable disabled={workingQuoteId !== null}
                                  onPress={() => { void decideQuote(quote.id, 'approve'); }}
                                  style={[styles.quoteButton, styles.approveButton]}>
                                  <Text style={styles.approveText}>
                                    {workingQuoteId === quote.id ? 'Aprovando...' : 'Confirmar aprovação'}
                                  </Text>
                                </Pressable>
                                <Pressable onPress={() => setConfirmingQuoteId(null)}>
                                  <Text style={styles.action}>Voltar</Text>
                                </Pressable>
                              </View>
                            ) : (
                              <Pressable
                                disabled={workingQuoteId !== null}
                                onPress={() => setConfirmingQuoteId(quote.id)}
                                style={[styles.quoteButton, styles.approveButton]}
                              >
                                <Text style={styles.approveText}>Aprovar orçamento</Text>
                              </Pressable>
                            )}
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
                            {cancellationResults[quote.id] ? (
                              <Text style={styles.muted}>{cancellationResults[quote.id]}</Text>
                            ) : null}
                            {paymentsByQuote[quote.id].status === 'PAGO' ? (
                              serviceCancellationQuoteId === quote.id ? (
                                <View style={styles.paymentConfirmation}>
                                  <Text style={styles.muted}>
                                    O cancelamento só é permitido antes do início do serviço. O reembolso será
                                    solicitado pelo Mercado Pago ao meio de pagamento original.
                                  </Text>
                                  <TextInput
                                    accessibilityLabel="Motivo do cancelamento"
                                    maxLength={1000}
                                    multiline
                                    onChangeText={(value) => setCancellationReasons((current) => ({
                                      ...current,
                                      [quote.id]: value,
                                    }))}
                                    placeholder="Descreva o motivo (mínimo 5 caracteres)"
                                    style={styles.cancellationReason}
                                    value={cancellationReasons[quote.id] ?? ''}
                                  />
                                  <Pressable
                                    disabled={workingPaymentQuoteId !== null}
                                    onPress={() => { void cancelPaidService(quote.id); }}
                                  >
                                    <Text style={styles.dangerAction}>
                                      {workingPaymentQuoteId === quote.id
                                        ? 'Solicitando reembolso...'
                                        : 'Confirmar cancelamento e reembolso'}
                                    </Text>
                                  </Pressable>
                                  <Pressable onPress={() => setServiceCancellationQuoteId(null)}>
                                    <Text style={styles.action}>Voltar</Text>
                                  </Pressable>
                                </View>
                              ) : (
                                <Pressable onPress={() => setServiceCancellationQuoteId(quote.id)}>
                                  <Text style={styles.dangerAction}>Cancelar serviço e solicitar reembolso</Text>
                                </Pressable>
                              )
                            ) : null}
                            {paymentsByQuote[quote.id].status === 'REEMBOLSO_PENDENTE' ? (
                              <>
                                <Text style={styles.muted}>
                                  A execução do serviço permanece bloqueada enquanto o reembolso é confirmado.
                                </Text>
                                <Pressable
                                  disabled={workingPaymentQuoteId !== null}
                                  onPress={() => { void cancelPaidService(quote.id); }}
                                >
                                  <Text style={styles.action}>
                                    {workingPaymentQuoteId === quote.id ? 'Consultando...' : 'Consultar ou tentar reembolso novamente'}
                                  </Text>
                                </Pressable>
                              </>
                            ) : null}
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
            </> : null}
          </View>
          );
        }) : null}
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

function getRequestNextAction(
  requestStatus: ServiceRequest['status'],
  slots: VisitSlot[],
  quotes: ServiceQuote[],
  payments: Record<number, ServicePayment>,
) {
  if (requestStatus === 'ENCERRADA') return null;
  if (slots.some((slot) => slot.status === 'RESERVADA')) {
    return 'Confirme ou cancele o horário de visita reservado pelo prestador.';
  }
  if (quotes.some((quote) => quote.status === 'ENVIADO')) {
    return 'Revise os novos orçamentos e escolha se deseja aprovar um deles.';
  }
  const approvedQuote = quotes.find((quote) => quote.status === 'APROVADO');
  if (approvedQuote) {
    const payment = payments[approvedQuote.id];
    if (!payment) return 'Pague o orçamento aprovado para iniciar o serviço.';
    if (payment.status === 'AGUARDANDO_PAGAMENTO') {
      return 'Conclua o pagamento do orçamento aprovado.';
    }
    if (['RECUSADO', 'EXPIRADO', 'CANCELADO'].includes(payment.status)) {
      return 'O pagamento não foi concluído. Tente novamente nos detalhes.';
    }
    if (payment.status === 'REEMBOLSO_PENDENTE') {
      return 'O reembolso está em processamento. Confira os detalhes para acompanhar.';
    }
  }
  return null;
}

function formatArea(area: number) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(area);
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function formatVisitTime(slot: VisitSlot) {
  const [date, time] = slot.inicioLocal.split('T');
  const zoneLabel = brazilianZoneLabels[slot.timeZoneId] ?? 'horário local do endereço';
  return `${date} às ${time.slice(0, 5)} (${zoneLabel})`;
}

const brazilianZoneLabels: Record<string, string> = {
  'America/Noronha': 'Fernando de Noronha',
  'America/Belem': 'Belém',
  'America/Fortaleza': 'Fortaleza',
  'America/Recife': 'Recife',
  'America/Araguaina': 'Araguaína',
  'America/Maceio': 'Maceió',
  'America/Bahia': 'Bahia',
  'America/Sao_Paulo': 'São Paulo',
  'America/Campo_Grande': 'Campo Grande',
  'America/Cuiaba': 'Cuiabá',
  'America/Santarem': 'Santarém',
  'America/Porto_Velho': 'Porto Velho',
  'America/Boa_Vista': 'Boa Vista',
  'America/Manaus': 'Manaus',
  'America/Eirunepe': 'Eirunepé',
  'America/Rio_Branco': 'Rio Branco',
};

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
    REPASSE_PENDENTE: 'Repasse em processamento',
    REPASSADO: 'Repasse realizado',
  }[status];
}

function formatAddress(request: ServiceRequest) {
  const address = request.endereco;
  const street = [address.logradouro, address.numero, address.complemento, address.bairro]
    .filter(Boolean).join(', ');
  return [address.apelido, street, `${address.cidade} - ${address.uf}`].filter(Boolean).join(' · ');
}

function formatShortAddress(request: ServiceRequest) {
  const address = request.endereco;
  return [address.apelido, `${address.cidade} - ${address.uf}`].filter(Boolean).join(' · ');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { paddingHorizontal: 20, paddingVertical: 18, gap: 14 },
  filters: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  filter: { borderWidth: 1, borderColor: '#ccc', borderRadius: 20,
    paddingHorizontal: 12, paddingVertical: 9, minHeight: 40 },
  selectedFilter: { backgroundColor: '#a43434', borderColor: '#a43434' },
  filterText: { color: '#555' },
  selectedFilterText: { color: '#fff', fontWeight: '600' },
  card: { borderWidth: 1, borderColor: '#e1e1e1', borderRadius: 14, gap: 9, padding: 14 },
  expandedCard: { borderColor: '#d3d3d3' },
  summaryHeader: { alignItems: 'flex-start', flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
  summaryText: { flex: 1, gap: 3 },
  summaryLocation: { color: '#666', fontSize: 13 },
  summaryMeta: { color: '#777', fontSize: 13 },
  nextAction: { color: '#333', fontSize: 14, fontWeight: '600', lineHeight: 20 },
  attentionStatus: { backgroundColor: '#fff3df', color: '#865208' },
  progressStatus: { backgroundColor: '#edf4eb', color: '#426341' },
  detailsToggle: { alignItems: 'center', borderTopColor: '#eee', borderTopWidth: 1,
    flexDirection: 'row', justifyContent: 'space-between', marginTop: 2, minHeight: 42, paddingTop: 7 },
  detailsToggleText: { color: '#a43434', fontSize: 14, fontWeight: '700' },
  detailsChevron: { color: '#a43434', fontSize: 22, fontWeight: '500' },
  emptyState: { alignItems: 'flex-start', backgroundColor: '#fafafa', borderRadius: 12, gap: 8, padding: 16 },
  emptyTitle: { color: '#333', fontSize: 17, fontWeight: '700' },
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
  quoteConfirmation: { width: '100%', gap: 8, borderTopColor: '#eee', borderTopWidth: 1, paddingTop: 8 },
  paymentInfo: { borderTopColor: '#eee', borderTopWidth: 1, gap: 6, marginTop: 4, paddingTop: 8 },
  paymentConfirmation: { borderTopColor: '#eee', borderTopWidth: 1, gap: 4, paddingTop: 6 },
  cancellationReason: { minHeight: 80, borderColor: '#ccc', borderRadius: 8, borderWidth: 1,
    color: '#222', padding: 10, textAlignVertical: 'top' },
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
