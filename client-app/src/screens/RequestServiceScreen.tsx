import React, { useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/RootNavigator';
import { ensureAccessToken } from '../services/CustomerAuthService';
import { createServiceRequest, Priority } from '../services/CustomerService';
import { requestAndGetCurrentLocation, Coordinates } from '../services/LocationService';

type Props = NativeStackScreenProps<RootStackParamList, 'RequestService'>;

const PRIORITIES: { value: Priority; label: string }[] = [
  { value: 'LOW', label: 'Baja' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'HIGH', label: 'Alta' },
  { value: 'URGENT', label: 'Urgente' },
];

/** Formulario de nueva solicitud. Exige capturar el GPS antes de poder enviar. */
export default function RequestServiceScreen({ navigation }: Props) {
  const [title, setTitle] = useState('');
  const [address, setAddress] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');

  const [location, setLocation] = useState<Coordinates | null>(null);
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const captureLocation = async () => {
    setLocating(true);
    setLocationError(null);
    try {
      setLocation(await requestAndGetCurrentLocation());
    } catch (e) {
      setLocationError(e instanceof Error ? e.message : 'No se pudo obtener tu ubicación');
    } finally {
      setLocating(false);
    }
  };

  const submit = async () => {
    if (!location) return;
    if (!title.trim() || !address.trim()) {
      setError('Título y dirección son obligatorios');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const token = await ensureAccessToken();
      if (!token) {
        navigation.reset({ index: 0, routes: [{ name: 'CustomerLogin' }] });
        return;
      }
      await createServiceRequest(token, {
        title: title.trim(),
        address: address.trim(),
        priority,
        lat: location.lat,
        lng: location.lng,
      });
      navigation.goBack();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo enviar la solicitud');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.label}>¿Qué necesitas?</Text>
      <TextInput
        style={styles.input}
        value={title}
        onChangeText={setTitle}
        placeholder="Ej. Fuga en tubería principal"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.label}>Dirección</Text>
      <TextInput
        style={styles.input}
        value={address}
        onChangeText={setAddress}
        placeholder="Calle, número, referencia"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.label}>Prioridad</Text>
      <View style={styles.chipRow}>
        {PRIORITIES.map((p) => (
          <TouchableOpacity
            key={p.value}
            style={[styles.chip, priority === p.value && styles.chipSelected]}
            onPress={() => setPriority(p.value)}
          >
            <Text style={[styles.chipText, priority === p.value && styles.chipTextSelected]}>
              {p.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Ubicación</Text>
      <TouchableOpacity style={styles.locationButton} onPress={captureLocation} disabled={locating}>
        {locating ? (
          <ActivityIndicator color="#2563eb" />
        ) : (
          <Text style={styles.locationButtonText}>
            {location ? `📍 Ubicación capturada (${location.lat.toFixed(4)}, ${location.lng.toFixed(4)})` : '📍 Usar mi ubicación actual'}
          </Text>
        )}
      </TouchableOpacity>
      {locationError && <Text style={styles.error}>⚠ {locationError}</Text>}

      {error && <Text style={styles.error}>⚠ {error}</Text>}

      <TouchableOpacity
        style={[styles.button, !location && styles.buttonDisabled]}
        onPress={submit}
        disabled={!location || submitting}
      >
        {submitting ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>
            {location ? 'Enviar solicitud' : 'Captura tu ubicación para continuar'}
          </Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  content: { padding: 20, paddingBottom: 60 },
  label: { color: '#94a3b8', fontSize: 12, fontWeight: '600', marginBottom: 6, marginTop: 16 },
  input: { backgroundColor: '#1e293b', borderRadius: 12, padding: 14, color: '#e2e8f0', fontSize: 15 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 100,
    backgroundColor: '#1e293b',
    borderWidth: 1,
    borderColor: '#334155',
  },
  chipSelected: { backgroundColor: 'rgba(37,99,235,0.18)', borderColor: '#2563eb' },
  chipText: { color: '#94a3b8', fontSize: 13, fontWeight: '600' },
  chipTextSelected: { color: '#60a5fa' },
  locationButton: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#334155',
  },
  locationButtonText: { color: '#e2e8f0', fontSize: 14, fontWeight: '600' },
  button: { backgroundColor: '#2563eb', borderRadius: 12, padding: 15, alignItems: 'center', marginTop: 24 },
  buttonDisabled: { backgroundColor: '#1e293b' },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 14 },
  error: { color: '#f87171', fontSize: 13, marginTop: 10 },
});
