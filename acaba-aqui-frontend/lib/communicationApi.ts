import { apiFetch } from './api';

export interface ProviderRegion {
  id: number;
  cidade: string;
  uf: string;
}

export interface RequestOpportunity {
  id: number;
  solicitacaoId: number;
  servicoNome: string;
  areaM2: number;
  cidade: string;
  uf: string;
  status: 'PENDENTE' | 'ACEITA' | 'RECUSADA' | 'ENCERRADA';
  lida: boolean;
  dataCriacao: string;
}

export interface Conversation {
  id: number;
  servicoNome: string;
  clienteId: number;
  clienteNome: string;
  prestadorId: number;
  prestadorNome: string;
  cidade: string;
  uf: string;
  dataCriacao: string;
}

export interface ChatMessage {
  id: number;
  conversaId: number;
  remetenteId: number;
  remetenteNome: string;
  conteudo: string;
  dataCriacao: string;
}

export interface VisitSlot {
  id: number;
  inicioLocal: string;
  fimLocal: string;
  timeZoneId: string;
  status: 'DISPONIVEL' | 'RESERVADA' | 'CONFIRMADA' | 'CANCELADA' | 'EXPIRADA';
  prestadorId: number | null;
  prestadorNome: string | null;
}

export interface VisitConfirmation {
  visita: VisitSlot;
  conversationId: number;
}

export interface ServiceQuote {
  id: number;
  solicitacaoId: number;
  prestadorId: number;
  prestadorNome: string;
  descricao: string;
  valorTotal: number;
  status: 'ENVIADO' | 'APROVADO' | 'RECUSADO' | 'EXPIRADO' | 'CANCELADO';
  dataCriacao: string;
  expiraEm: string;
}

export interface InAppNotification {
  id: number;
  tipo: string;
  mensagem: string;
  solicitacaoId: number | null;
  orcamentoId: number | null;
  lida: boolean;
  dataCriacao: string;
}

export interface ServicePayment {
  id: number;
  quoteId: number;
  status: 'AGUARDANDO_PAGAMENTO' | 'PAGO' | 'RECUSADO' | 'EXPIRADO' | 'CANCELADO'
    | 'REEMBOLSO_PENDENTE' | 'REEMBOLSADO' | 'REEMBOLSADO_PARCIAL' | 'CONTESTADO';
  checkoutUrl: string | null;
  amount: number;
  paidAmount: number;
  orderStatus: string | null;
  statusDetail: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentLookup {
  exists: boolean;
  payment: ServicePayment | null;
}

async function request<T>(path: string, method = 'GET', body?: object): Promise<T> {
  const response = await apiFetch(path, {
    method,
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  if (!response.ok) {
    const text = await response.text();
    let message = text;
    try {
      const payload: unknown = JSON.parse(text);
      if (typeof payload === 'object' && payload !== null && 'message' in payload
        && typeof payload.message === 'string') {
        message = payload.message;
      }
    } catch {
      // Preserve a plain-text backend error.
    }
    throw new Error(message || `Falha na solicitação (${response.status}).`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const communicationApi = {
  listProviderServices: async () => {
    const response = await request<{ servicoIds: number[] }>('/provider/services');
    return response.servicoIds;
  },
  saveProviderServices: async (serviceIds: number[]) => {
    const response = await request<{ servicoIds: number[] }>('/provider/services', 'PUT', {
      servicoIds: serviceIds,
    });
    return response.servicoIds;
  },
  listCoverage: () => request<ProviderRegion[]>('/provider/coverage'),
  addCoverage: (input: { cidade: string; uf: string }) =>
    request<ProviderRegion>('/provider/coverage', 'POST', input),
  removeCoverage: (id: number) => request<void>(`/provider/coverage/${id}`, 'DELETE'),
  listOpportunities: () => request<RequestOpportunity[]>('/provider/opportunities'),
  markOpportunitiesRead: () => request<{ atualizadas: number }>('/provider/opportunities/read', 'PUT'),
  acceptOpportunity: (id: number) =>
    request<void>(`/provider/opportunities/${id}/accept`, 'POST'),
  declineOpportunity: (id: number) =>
    request<RequestOpportunity>(`/provider/opportunities/${id}/decline`, 'POST'),
  listConversations: () => request<Conversation[]>('/conversations'),
  listMessages: (conversationId: number) =>
    request<ChatMessage[]>(`/conversations/${conversationId}/messages`),
  sendMessage: (conversationId: number, conteudo: string) =>
    request<ChatMessage>(`/conversations/${conversationId}/messages`, 'POST', { conteudo }),
};

export const visitSlotApi = {
  list: (requestId: number) => request<VisitSlot[]>(`/visit-slots/requests/${requestId}`),
  reserve: (slotId: number) => request<VisitSlot>(`/visit-slots/${slotId}/reserve`, 'POST'),
  confirm: (slotId: number) =>
    request<VisitConfirmation>(`/visit-slots/${slotId}/confirm`, 'POST'),
  cancel: (slotId: number) => request<VisitSlot>(`/visit-slots/${slotId}/cancel`, 'POST'),
};

export const serviceQuoteApi = {
  list: (requestId: number) => request<ServiceQuote[]>(`/service-quotes/requests/${requestId}`),
  send: (requestId: number, input: { descricao: string; valorTotal: number }) =>
    request<ServiceQuote>(`/service-quotes/requests/${requestId}`, 'POST', input),
  approve: (quoteId: number) => request<ServiceQuote>(`/service-quotes/${quoteId}/approve`, 'POST'),
  reject: (quoteId: number) => request<ServiceQuote>(`/service-quotes/${quoteId}/reject`, 'POST'),
};

export const inAppNotificationApi = {
  list: () => request<InAppNotification[]>('/notifications'),
  markRead: (notificationId: number) => request<void>(`/notifications/${notificationId}/read`, 'PUT'),
};

export const servicePaymentApi = {
  getForQuote: (quoteId: number) => request<PaymentLookup>(`/payments/quotes/${quoteId}`),
  get: (paymentId: number) => request<ServicePayment>(`/payments/${paymentId}`),
  sync: (paymentId: number) => request<ServicePayment>(`/payments/${paymentId}/sync`, 'POST'),
  startCheckout: (quoteId: number) =>
    request<ServicePayment>(`/payments/quotes/${quoteId}/checkout`, 'POST'),
  cancel: (paymentId: number) => request<ServicePayment>(`/payments/${paymentId}/cancel`, 'POST'),
};
