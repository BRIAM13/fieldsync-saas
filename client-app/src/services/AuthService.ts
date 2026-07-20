import { API_BASE } from '../config';

interface AuthResponse {
  token: string;
  user: { id: string; name: string; companyId: string };
  company: { id: string; name: string };
}

/**
 * Inicia sesión contra el backend y devuelve el JWT. La app cliente lo usa para
 * autorizar el WebSocket de seguimiento (que exige un token válido).
 */
export async function login(email: string, password: string): Promise<string> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    throw new Error(`Login falló: ${res.status}`);
  }
  const data: AuthResponse = await res.json();
  return data.token;
}
