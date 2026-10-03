import { Platform } from 'react-native';
import { getAuthToken } from './authToken';

const configuredApiUrl = process.env.EXPO_PUBLIC_API_URL?.replace(/\/+$/, '');

export const API_BASE_URL = configuredApiUrl ?? (Platform.OS === 'android'
  ? 'http://10.0.2.2:8080'
  : 'http://localhost:8080');

export async function apiFetch(path: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  const normalizedPath = path.replace(/^\/+|\/+$/g, '');
  const isPublicAuthRequest = (init.method ?? 'GET').toUpperCase() === 'POST'
    && ['auth/login', 'auth/email-verification/request', 'auth/email-verification/resend',
      'auth/email-verification/confirm', 'auth/password-reset/request',
      'auth/password-reset/resend', 'auth/password-reset/confirm'].includes(normalizedPath);

  if (isPublicAuthRequest) {
    headers.delete('Authorization');
  } else {
    const token = await getAuthToken();
    if (token) {
    headers.set('Authorization', `Bearer ${token}`);
    }
  }

  return fetch(`${API_BASE_URL}${path}`, { ...init, headers });
}