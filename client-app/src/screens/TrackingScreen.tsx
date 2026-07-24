import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/RootNavigator';
import { useTechnicianTracking } from '../services/TrackingService';
import { ensureAccessToken } from '../services/CustomerAuthService';

type Props = NativeStackScreenProps<RootStackParamList, 'Tracking'>;

/**
 * Pantalla que ve el cliente final: sigue a su técnico en tiempo real y ve el ETA de
 * la orden que eligió en "Mis solicitudes". Reutiliza el token de cliente persistido
 * (renovándolo si hace falta) y abre el WebSocket autorizado.
 */
export default function TrackingScreen({ route }: Props) {
  const { orderId } = route.params;
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    ensureAccessToken()
      .then(setToken)
      .catch((e) => console.warn('No se pudo autenticar', e));
  }, []);

  const position = useTechnicianTracking(orderId, token);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Tu servicio está en camino</Text>

      <View style={styles.mapPlaceholder}>
        <Text style={styles.mapHint}>🗺️ Mapa en vivo (react-native-maps)</Text>
        {position && <Text style={styles.pin}>📍</Text>}
      </View>

      {position ? (
        <View style={styles.card}>
          <Text style={styles.tech}>{position.name}</Text>
          <Text style={styles.eta}>
            {position.etaMinutes > 0
              ? `Llega en ~${position.etaMinutes} min`
              : '¡Tu técnico ha llegado!'}
          </Text>
        </View>
      ) : (
        <Text style={styles.loading}>Conectando con el técnico…</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', padding: 20, justifyContent: 'center' },
  title: { color: '#e2e8f0', fontSize: 22, fontWeight: '700', marginBottom: 16 },
  mapPlaceholder: { height: 260, backgroundColor: '#0b1220', borderRadius: 12,
    alignItems: 'center', justifyContent: 'center' },
  mapHint: { color: '#64748b' },
  pin: { fontSize: 28, marginTop: 8 },
  card: { backgroundColor: '#1e293b', borderRadius: 12, padding: 16, marginTop: 16 },
  tech: { color: '#e2e8f0', fontSize: 18, fontWeight: '600' },
  eta: { color: '#2563eb', fontSize: 16, marginTop: 4 },
  loading: { color: '#94a3b8', marginTop: 16 },
});
