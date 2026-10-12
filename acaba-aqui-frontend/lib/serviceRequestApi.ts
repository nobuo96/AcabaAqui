import { apiFetch } from './api';

export interface RequestAddress {
  id: number;
  cep: string;
  logradouro: string | null;
  numero: string;
  complemento: string | null;
  bairro: string | null;
  cidade: string;
  uf: string;
  apelido: string | null;
}

export interface ServiceRequest {
  id: number;
  servicoId: number;
  servicoNome: string;
  areaM2: number;
  status: 'CRIADA' | 'ENCERRADA';
  dataCriacao: string;
  endereco: RequestAddress;
}

interface AddressResponse extends RequestAddress {
  principal: boolean;
}

async function parseResponse<T>(response: Response): Promise<T> {
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
      // Keep a plain-text backend error.
    }
    throw new Error(message || `Falha na solicitação (${response.status}).`);
  }
  return response.json() as Promise<T>;
}

function requestPath(userId: string) {
  return `/user/${encodeURIComponent(userId)}/service-requests`;
}

export const serviceRequestApi = {
  async list(userId: string) {
    return parseResponse<ServiceRequest[]>(await apiFetch(requestPath(userId)));
  },
  async listAddresses(userId: string) {
    return parseResponse<AddressResponse[]>(
      await apiFetch(`/user/${encodeURIComponent(userId)}/addresses`),
    );
  },
  async create(userId: string, input: { servicoId: number; enderecoId: number; areaM2: number }) {
    return parseResponse<ServiceRequest>(await apiFetch(requestPath(userId), {
      method: 'POST',
      body: JSON.stringify(input),
    }));
  },
  async cancel(userId: string, requestId: number) {
    return parseResponse<ServiceRequest>(await apiFetch(
      `${requestPath(userId)}/${encodeURIComponent(requestId)}/cancel`,
      { method: 'POST' },
    ));
  },
};
