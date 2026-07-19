import { useEffect, useState } from 'react';

export interface TechnicianPosition {
  technicianId: string;
  name: string;
  lat: number;
  lng: number;
  etaMinutes: number;
}

/**
 * Servicio de seguimiento en tiempo real (característica clave #3).
 *
 * En producción abriría un WebSocket (o Firebase Realtime DB) hacia el backend.
 * Aquí lo simulamos con un intervalo que va acercando al técnico y reduciendo el ETA,
 * para que la pantalla del cliente muestre el movimiento sin backend.
 */
export function useTechnicianTracking(orderId: string): TechnicianPosition | null {
  const [position, setPosition] = useState<TechnicianPosition | null>(null);

  useEffect(() => {
    let lat = -12.09;
    let lng = -77.05;
    let eta = 18;

    const id = setInterval(() => {
      // Aproxima al destino del cliente y reduce el ETA (simula el stream en vivo).
      lat += 0.0008;
      lng += 0.0006;
      eta = Math.max(0, eta - 1);
      setPosition({ technicianId: 'T-01', name: 'Carlos Ramírez', lat, lng, etaMinutes: eta });
    }, 2000);

    return () => clearInterval(id); // limpieza al desmontar (evita fugas)
  }, [orderId]);

  return position;
}
