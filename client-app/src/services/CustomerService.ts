import { API_BASE } from '../config';
import { Company } from './CustomerAuthService';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type WorkOrderStatus =
  | 'UNASSIGNED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED';

export interface WorkOrder {
  id: string;
  title: string;
  customerName: string;
  address: string;
  priority: Priority;
  status: WorkOrderStatus;
  scheduledAtEpochMs: number;
  location?: { lat: number; lng: number } | null;
  assignedTechnicianId?: string | null;
  customerId?: string | null;
}

export interface ServiceRequestInput {
  title: string;
  address: string;
  priority: Priority;
  lat: number;
  lng: number;
}

/** Lista pública de empresas — para que el cliente elija la suya al registrarse. */
export async function getCompanies(): Promise<Company[]> {
  const res = await fetch(`${API_BASE}/api/companies`);
  if (!res.ok) throw new Error(`No se pudo cargar la lista de empresas: ${res.status}`);
  return res.json();
}

export async function createServiceRequest(
  token: string,
  body: ServiceRequestInput,
): Promise<WorkOrder> {
  const res = await fetch(`${API_BASE}/api/service-requests`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => null);
    throw new Error(err?.message ?? `No se pudo crear la solicitud: ${res.status}`);
  }
  return res.json();
}

export async function getMyRequests(token: string): Promise<WorkOrder[]> {
  const res = await fetch(`${API_BASE}/api/service-requests/mine`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`No se pudieron cargar tus solicitudes: ${res.status}`);
  return res.json();
}
