import { Priority, WorkOrderStatus } from '../models/work-order.model';

/**
 * Traduce los enums del dominio (en inglés — contrato con el backend) al español.
 * Mismo mapeo semántico que usa la app Android, para consistencia entre plataformas.
 * Funciones puras: las usa tanto el pipe de plantillas como el código TS (popups de Leaflet).
 */
const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
  URGENT: 'Urgente',
};

const STATUS_LABELS: Record<WorkOrderStatus, string> = {
  UNASSIGNED: 'Sin asignar',
  ASSIGNED: 'Asignada',
  IN_PROGRESS: 'En progreso',
  ON_HOLD: 'En espera',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
};

export function priorityLabel(value: Priority): string {
  return PRIORITY_LABELS[value] ?? value;
}

export function statusLabel(value: WorkOrderStatus): string {
  return STATUS_LABELS[value] ?? value;
}
