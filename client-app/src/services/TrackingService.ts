import { useEffect, useState } from 'react';
import { WS_BASE } from '../config';

export interface TechnicianPosition {
  technicianId: string;
  name: string;
  lat: number;
  lng: number;
  etaMinutes: number;
  arrived: boolean;
}

/** Forma del mensaje JSON que emite el WebSocket del backend (/ws/tracking/{orderId}). */
interface TechnicianLocationMessage {
  orderId: string;
  technicianId: string;
  technicianName: string;
  lat: number;
  lng: number;
  etaMinutes: number;
  arrived: boolean;
}

/**
 * Seguimiento en tiempo real (característica clave #3) **conectado al backend real**.
 *
 * Abre un WebSocket al backend Ktor y actualiza la posición del técnico con cada mensaje.
 * Antes era un intervalo simulado; ahora el stream viene del servidor.
 */
export function useTechnicianTracking(
  orderId: string,
  token: string | null,
): TechnicianPosition | null {
  const [position, setPosition] = useState<TechnicianPosition | null>(null);

  useEffect(() => {
    if (!token) return; // sin token no se autoriza el WebSocket
    // El token viaja como query param: los WebSocket no llevan header Authorization.
    const socket = new WebSocket(
      `${WS_BASE}/ws/tracking/${orderId}?token=${encodeURIComponent(token)}`,
    );

    socket.onmessage = (event) => {
      const msg: TechnicianLocationMessage = JSON.parse(event.data);
      setPosition({
        technicianId: msg.technicianId,
        name: msg.technicianName,
        lat: msg.lat,
        lng: msg.lng,
        etaMinutes: msg.etaMinutes,
        arrived: msg.arrived,
      });
    };

    socket.onerror = (e) => {
      console.warn('Tracking WebSocket error', e);
    };

    return () => socket.close(); // cierra la conexión al desmontar (evita fugas)
  }, [orderId, token]);

  return position;
}
