/** Modelo tipado de una orden de trabajo, compartido por todo el panel. */
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type WorkOrderStatus =
  | 'UNASSIGNED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED';

export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface Technician {
  id: string;
  name: string;
  location: GeoPoint;
  available: boolean;
}

export interface WorkOrder {
  id: string;
  title: string;
  customerName: string;
  address: string;
  priority: Priority;
  status: WorkOrderStatus;
  scheduledAtEpochMs: number;
  location?: GeoPoint;
  assignedTechnicianId?: string | null;
}
