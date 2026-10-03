import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

export const AUTH_TOKEN_KEY = 'app-secure-token';
const AUTH_USER_ID_KEY = 'app-secure-user-id';

export async function saveAuthToken(token: string) {
  if (Platform.OS === 'web') {
    window.sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    return;
  }

  await SecureStore.setItemAsync(AUTH_TOKEN_KEY, token);
}

export async function getAuthToken() {
  if (Platform.OS === 'web') {
    return window.sessionStorage.getItem(AUTH_TOKEN_KEY);
  }

  return SecureStore.getItemAsync(AUTH_TOKEN_KEY);
}

export async function saveAuthUserId(userId: number | string) {
  const value = String(userId);
  if (Platform.OS === 'web') {
    window.sessionStorage.setItem(AUTH_USER_ID_KEY, value);
    return;
  }

  await SecureStore.setItemAsync(AUTH_USER_ID_KEY, value);
}

export async function getAuthUserId() {
  if (Platform.OS === 'web') {
    return window.sessionStorage.getItem(AUTH_USER_ID_KEY);
  }

  return SecureStore.getItemAsync(AUTH_USER_ID_KEY);
}

export async function removeAuthToken() {
  if (Platform.OS === 'web') {
    window.sessionStorage.removeItem(AUTH_TOKEN_KEY);
    window.sessionStorage.removeItem(AUTH_USER_ID_KEY);
    return;
  }

  await SecureStore.deleteItemAsync(AUTH_TOKEN_KEY);
  await SecureStore.deleteItemAsync(AUTH_USER_ID_KEY);
}