import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE } from '../config';

const TOKEN_KEY = 'fieldsync.customer.token';
const REFRESH_KEY = 'fieldsync.customer.refresh';
const CUSTOMER_KEY = 'fieldsync.customer.profile';

export interface Customer {
  id: string;
  companyId: string;
  name: string;
  email: string;
  phone?: string | null;
}

export interface Company {
  id: string;
  name: string;
}

interface CustomerAuthResponse {
  token: string;
  refreshToken: string;
  customer: Customer;
  company: Company;
}

async function persist(res: CustomerAuthResponse): Promise<void> {
  await AsyncStorage.multiSet([
    [TOKEN_KEY, res.token],
    [REFRESH_KEY, res.refreshToken],
    [CUSTOMER_KEY, JSON.stringify(res.customer)],
  ]);
}

/** Registra un cliente nuevo bajo la empresa elegida y deja la sesión iniciada. */
export async function register(
  companyId: string,
  name: string,
  email: string,
  password: string,
  phone?: string,
): Promise<Customer> {
  const res = await fetch(`${API_BASE}/customer/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ companyId, name, email, phone: phone || null, password }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `Registro falló: ${res.status}`);
  }
  const data: CustomerAuthResponse = await res.json();
  await persist(data);
  return data.customer;
}

export async function login(email: string, password: string): Promise<Customer> {
  const res = await fetch(`${API_BASE}/customer/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `Login falló: ${res.status}`);
  }
  const data: CustomerAuthResponse = await res.json();
  await persist(data);
  return data.customer;
}

async function refresh(refreshToken: string): Promise<string> {
  const res = await fetch(`${API_BASE}/customer/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!res.ok) throw new Error(`Refresh falló: ${res.status}`);
  const data: CustomerAuthResponse = await res.json();
  await persist(data);
  return data.token;
}

/** Access token fresco, renovando con el refresh token guardado. Null si no hay sesión. */
export async function ensureAccessToken(): Promise<string | null> {
  const storedRefresh = await AsyncStorage.getItem(REFRESH_KEY);
  if (!storedRefresh) return null;
  try {
    return await refresh(storedRefresh);
  } catch {
    await clearSession();
    return null;
  }
}

/** true si hay una sesión de cliente guardada (no valida el token, solo su presencia). */
export async function hasSession(): Promise<boolean> {
  return (await AsyncStorage.getItem(REFRESH_KEY)) !== null;
}

export async function getCurrentCustomer(): Promise<Customer | null> {
  const raw = await AsyncStorage.getItem(CUSTOMER_KEY);
  return raw ? (JSON.parse(raw) as Customer) : null;
}

export async function clearSession(): Promise<void> {
  await AsyncStorage.multiRemove([TOKEN_KEY, REFRESH_KEY, CUSTOMER_KEY]);
}
