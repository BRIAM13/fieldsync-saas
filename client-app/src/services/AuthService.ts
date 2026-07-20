import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE } from '../config';

const ACCESS_KEY = 'fieldsync.token';
const REFRESH_KEY = 'fieldsync.refresh';
const DEMO_EMAIL = 'admin@fieldsync.dev';
const DEMO_PASSWORD = 'demo1234';

interface Tokens {
  token: string;
  refreshToken: string;
}

interface AuthResponse extends Tokens {
  user: { id: string; name: string; companyId: string };
  company: { id: string; name: string };
}

async function persist(res: AuthResponse): Promise<void> {
  await AsyncStorage.multiSet([
    [ACCESS_KEY, res.token],
    [REFRESH_KEY, res.refreshToken],
  ]);
}

/** Login → guarda ambos tokens en AsyncStorage (sobreviven a reinicios de la app). */
export async function login(email: string, password: string): Promise<Tokens> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error(`Login falló: ${res.status}`);
  const data: AuthResponse = await res.json();
  await persist(data);
  return data;
}

/** Renueva el access token con el refresh token (que se rota en el servidor). */
export async function refresh(refreshToken: string): Promise<Tokens> {
  const res = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!res.ok) throw new Error(`Refresh falló: ${res.status}`);
  const data: AuthResponse = await res.json();
  await persist(data);
  return data;
}

/**
 * Garantiza un access token fresco: si hay un refresh token guardado lo usa para renovar
 * (el access es corto); si no, inicia sesión con la cuenta demo. Devuelve el access token.
 */
export async function ensureAccessToken(): Promise<string> {
  const storedRefresh = await AsyncStorage.getItem(REFRESH_KEY);
  if (storedRefresh) {
    try {
      return (await refresh(storedRefresh)).token;
    } catch {
      // refresh expirado/ inválido → cae a login
    }
  }
  return (await login(DEMO_EMAIL, DEMO_PASSWORD)).token;
}

export async function clearSession(): Promise<void> {
  await AsyncStorage.multiRemove([ACCESS_KEY, REFRESH_KEY]);
}
