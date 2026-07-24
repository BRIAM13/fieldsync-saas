import { Priority, WorkOrderStatus, GeoPoint } from './work-order.model';

export interface Customer {
  id: string;
  companyId: string;
  name: string;
  email: string;
  phone?: string | null;
}

export interface CompanySummary {
  id: string;
  name: string;
}

/** `website` es un honeypot: campo oculto en el form real; los bots suelen llenarlo. */
export interface CustomerRegisterRequest {
  companyId: string;
  name: string;
  email: string;
  phone?: string;
  password: string;
  website?: string;
}

export interface CustomerLoginRequest {
  email: string;
  password: string;
}

export interface CustomerAuthResponse {
  token: string;
  refreshToken: string;
  customer: Customer;
  company: CompanySummary;
}

/** Cuerpo de POST /api/service-requests — el cliente pide un servicio a su empresa. */
export interface ServiceRequestCreate {
  title: string;
  address: string;
  priority: Priority;
  lat: number;
  lng: number;
}

/** Misma forma que WorkOrder (el backend responde el mismo tipo), con customerId presente. */
export interface CustomerServiceRequest {
  id: string;
  title: string;
  customerName: string;
  address: string;
  priority: Priority;
  status: WorkOrderStatus;
  scheduledAtEpochMs: number;
  location?: GeoPoint | null;
  assignedTechnicianId?: string | null;
  customerId?: string | null;
}
