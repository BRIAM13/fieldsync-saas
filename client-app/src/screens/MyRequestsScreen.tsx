import React, { useCallback, useState } from 'react';
import {
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/RootNavigator';
import { ensureAccessToken, clearSession, getCurrentCustomer, Customer } from '../services/CustomerAuthService';
import { getMyRequests, WorkOrder } from '../services/CustomerService';

type Props = NativeStackScreenProps<RootStackParamList, 'MyRequests'>;

const STATUS_LABELS: Record<WorkOrder['status'], string> = {
  UNASSIGNED: 'Sin asignar',
  ASSIGNED: 'Asignada',
  IN_PROGRESS: 'En camino',
  ON_HOLD: 'En espera',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
};

const STATUS_COLORS: Record<WorkOrder['status'], string> = {
  UNASSIGNED: '#94a3b8',
  ASSIGNED: '#60a5fa',
  IN_PROGRESS: '#facc15',
  ON_HOLD: '#c084fc',
  COMPLETED: '#4ade80',
  CANCELLED: '#f87171',
};

/** Lista de solicitudes del cliente. Punto de entrada tras el login/registro. */
export default function MyRequestsScreen({ navigation }: Props) {
  const [requests, setRequests] = useState<WorkOrder[]>([]);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const token = await ensureAccessToken();
      if (!token) {
        navigation.reset({ index: 0, routes: [{ name: 'CustomerLogin' }] });
        return;
      }
      const [mine, me] = await Promise.all([getMyRequests(token), getCurrentCustomer()]);
      setRequests(mine.sort((a, b) => b.scheduledAtEpochMs - a.scheduledAtEpochMs));
      setCustomer(me);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar tus solicitudes');
    } finally {
      setRefreshing(false);
    }
  }, [navigation]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const logout = async () => {
    await clearSession();
    navigation.reset({ index: 0, routes: [{ name: 'CustomerLogin' }] });
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.greeting}>Hola, {customer?.name ?? 'cliente'}</Text>
        <TouchableOpacity onPress={logout}>
          <Text style={styles.logout}>Cerrar sesión</Text>
        </TouchableOpacity>
      </View>

      {error && <Text style={styles.error}>⚠ {error}</Text>}

      <FlatList
        data={requests}
        keyExtractor={(item) => item.id}
        contentContainerStyle={requests.length === 0 ? styles.emptyContainer : styles.list}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              load();
            }}
            tintColor="#2563eb"
          />
        }
        ListEmptyComponent={<Text style={styles.empty}>Aún no tienes solicitudes.</Text>}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => navigation.navigate('Tracking', { orderId: item.id })}
          >
            <View style={styles.cardTop}>
              <Text style={styles.cardTitle}>{item.title}</Text>
              <View style={[styles.badge, { backgroundColor: `${STATUS_COLORS[item.status]}26` }]}>
                <Text style={[styles.badgeText, { color: STATUS_COLORS[item.status] }]}>
                  {STATUS_LABELS[item.status]}
                </Text>
              </View>
            </View>
            <Text style={styles.cardAddress}>{item.address}</Text>
          </TouchableOpacity>
        )}
      />

      <TouchableOpacity style={styles.fab} onPress={() => navigation.navigate('RequestService')}>
        <Text style={styles.fabText}>+ Nueva solicitud</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    paddingBottom: 8,
  },
  greeting: { color: '#e2e8f0', fontSize: 18, fontWeight: '700' },
  logout: { color: '#f87171', fontSize: 13, fontWeight: '600' },
  error: { color: '#f87171', fontSize: 13, marginHorizontal: 20, marginBottom: 8 },
  list: { padding: 20, paddingTop: 8, paddingBottom: 100 },
  emptyContainer: { flexGrow: 1, alignItems: 'center', justifyContent: 'center' },
  empty: { color: '#64748b', fontSize: 14 },
  card: { backgroundColor: '#1e293b', borderRadius: 14, padding: 16, marginBottom: 12 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cardTitle: { color: '#e2e8f0', fontSize: 16, fontWeight: '700', flex: 1, marginRight: 8 },
  cardAddress: { color: '#94a3b8', fontSize: 13, marginTop: 6 },
  badge: { paddingVertical: 4, paddingHorizontal: 10, borderRadius: 100 },
  badgeText: { fontSize: 11, fontWeight: '700' },
  fab: {
    position: 'absolute',
    bottom: 24,
    left: 20,
    right: 20,
    backgroundColor: '#2563eb',
    borderRadius: 14,
    padding: 16,
    alignItems: 'center',
  },
  fabText: { color: '#fff', fontWeight: '700', fontSize: 15 },
});
