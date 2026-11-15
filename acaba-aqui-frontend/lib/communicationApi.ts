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
