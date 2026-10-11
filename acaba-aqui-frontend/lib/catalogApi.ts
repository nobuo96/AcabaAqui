import { apiFetch } from './api';

export interface ServiceCategory {
  id: number;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

export interface CatalogService {
  id: number;
  nome: string;
  descricao: string;
  ativo: boolean;
  categoria: ServiceCategory;
}

export interface CategoryInput {
  nome: string;
  descricao: string;
  ativo?: boolean;
}

export interface ServiceInput {
  nome: string;
  descricao: string;
  categoriaId: number;
  ativo?: boolean;
}

async function request<T>(path: string, method = 'GET', body?: object): Promise<T> {
  const response = await apiFetch(path, {
    method,
    ...(body ? { body: JSON.stringify(body) } : {}),
  });

  if (!response.ok) {
    const responseText = await response.text();
    let message = responseText;
    try {
      const payload: unknown = JSON.parse(responseText);
      if (typeof payload === 'object' && payload !== null && 'message' in payload
        && typeof payload.message === 'string') {
        message = payload.message;
      }
    } catch {
      // Preserve a plain-text backend error message.
    }
    throw new Error(message || `Falha na solicitação (${response.status}).`);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const catalogApi = {
  listCategories: () => request<ServiceCategory[]>('/categories'),
  createCategory: (input: CategoryInput) => request<ServiceCategory>('/categories', 'POST', input),
  updateCategory: (id: number, input: CategoryInput) =>
    request<ServiceCategory>(`/categories/${id}`, 'PUT', input),
  deactivateCategory: (id: number) => request<ServiceCategory>(`/categories/${id}`, 'DELETE'),
  listServices: () => request<CatalogService[]>('/services'),
  createService: (input: ServiceInput) => request<CatalogService>('/services', 'POST', input),
  updateService: (id: number, input: ServiceInput) =>
    request<CatalogService>(`/services/${id}`, 'PUT', input),
  deactivateService: (id: number) => request<CatalogService>(`/services/${id}`, 'DELETE'),
};
