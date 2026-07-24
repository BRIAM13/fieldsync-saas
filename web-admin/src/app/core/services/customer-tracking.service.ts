import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { WS_BASE } from '../api.config';

export interface TechnicianLocation {
  orderId: string;
  technicianId: string;
  technicianName: string;
  lat: number;
  lng: number;
  etaMinutes: number;
  arrived: boolean;
}

/**
 * Seguimiento en tiempo real de una orden: abre `/ws/tracking/{orderId}?token=...` (el token
 * va como query param — los WebSocket no llevan header Authorization, igual que en la app RN)
 * y emite cada posición del técnico que llega. Cierra el socket automáticamente al desuscribirse.
 */
@Injectable({ providedIn: 'root' })
export class CustomerTrackingService {
  track(orderId: string, token: string): Observable<TechnicianLocation> {
    return new Observable<TechnicianLocation>((subscriber) => {
      const socket = new WebSocket(
        `${WS_BASE}/ws/tracking/${orderId}?token=${encodeURIComponent(token)}`,
      );

      socket.onmessage = (event) => {
        subscriber.next(JSON.parse(event.data) as TechnicianLocation);
      };
      socket.onerror = () => {
        subscriber.error(new Error('Error de conexión con el seguimiento en vivo'));
      };

      return () => socket.close();
    });
  }
}
