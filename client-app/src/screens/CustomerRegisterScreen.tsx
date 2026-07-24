import React, { useEffect, useState } from 'react';
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
import { register, Company } from '../services/CustomerAuthService';
import { getCompanies } from '../services/CustomerService';

type Props = NativeStackScreenProps<RootStackParamList, 'CustomerRegister'>;

/** Alta de cliente: elige su empresa de la lista pública, luego sus datos. */
export default function CustomerRegisterScreen({ navigation }: Props) {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [companiesLoading, setCompaniesLoading] = useState(true);
  const [companiesError, setCompaniesError] = useState<string | null>(null);
  const [companyId, setCompanyId] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getCompanies()
      .then((list) => {
        setCompanies(list);
        if (list.length > 0) setCompanyId(list[0].id);
      })
      .catch((e) => setCompaniesError(e instanceof Error ? e.message : 'No se pudo cargar'))
      .finally(() => setCompaniesLoading(false));
  }, []);

  const submit = async () => {
    if (!companyId) {
      setError('Elige una empresa');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await register(companyId, name.trim(), email.trim(), password, phone.trim() || undefined);
      navigation.reset({ index: 0, routes: [{ name: 'MyRequests' }] });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo crear la cuenta');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.label}>Empresa</Text>
      {companiesLoading ? (
        <ActivityIndicator color="#2563eb" style={{ marginBottom: 12 }} />
      ) : companiesError ? (
        <Text style={styles.error}>⚠ {companiesError}</Text>
      ) : (
        <View style={styles.companyList}>
          {companies.map((c) => (
            <TouchableOpacity
              key={c.id}
              style={[styles.companyChip, companyId === c.id && styles.companyChipSelected]}
              onPress={() => setCompanyId(c.id)}
            >
              <Text
                style={[styles.companyChipText, companyId === c.id && styles.companyChipTextSelected]}
              >
                {c.name}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      <Text style={styles.label}>Tu nombre</Text>
      <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="Nombre completo" placeholderTextColor="#64748b" />

      <Text style={styles.label}>Correo electrónico</Text>
      <TextInput
        style={styles.input}
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        placeholder="tu@correo.com"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.label}>Teléfono (opcional)</Text>
      <TextInput
        style={styles.input}
        value={phone}
        onChangeText={setPhone}
        keyboardType="phone-pad"
        placeholder="+51 999 999 999"
        placeholderTextColor="#64748b"
      />

      <Text style={styles.label}>Contraseña</Text>
      <TextInput
        style={styles.input}
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        placeholder="Mínimo 6 caracteres"
        placeholderTextColor="#64748b"
      />

      {error && <Text style={styles.error}>⚠ {error}</Text>}

      <TouchableOpacity style={styles.button} onPress={submit} disabled={submitting}>
        {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Crear cuenta</Text>}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  content: { padding: 20 },
  label: { color: '#94a3b8', fontSize: 12, fontWeight: '600', marginBottom: 6, marginTop: 14 },
  input: { backgroundColor: '#1e293b', borderRadius: 12, padding: 14, color: '#e2e8f0', fontSize: 15 },
  companyList: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  companyChip: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 100,
    backgroundColor: '#1e293b',
    borderWidth: 1,
    borderColor: '#334155',
  },
  companyChipSelected: { backgroundColor: 'rgba(37,99,235,0.18)', borderColor: '#2563eb' },
  companyChipText: { color: '#94a3b8', fontSize: 13, fontWeight: '600' },
  companyChipTextSelected: { color: '#60a5fa' },
  button: { backgroundColor: '#2563eb', borderRadius: 12, padding: 15, alignItems: 'center', marginTop: 24 },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  error: { color: '#f87171', fontSize: 13, marginTop: 10 },
});
