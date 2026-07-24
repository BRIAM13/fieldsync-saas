import React, { useState } from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/RootNavigator';
import { login } from '../services/CustomerAuthService';

type Props = NativeStackScreenProps<RootStackParamList, 'CustomerLogin'>;

/** Login del cliente final. Sin cuenta demo silenciosa — siempre exige credenciales reales. */
export default function CustomerLoginScreen({ navigation }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setLoading(true);
    setError(null);
    try {
      await login(email.trim(), password);
      navigation.reset({ index: 0, routes: [{ name: 'MyRequests' }] });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo ingresar');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.mark}>🔧</Text>
        <Text style={styles.title}>FieldSync</Text>
        <Text style={styles.subtitle}>Solicita y sigue tu servicio</Text>
      </View>

      <TextInput
        style={styles.input}
        placeholder="Correo electrónico"
        placeholderTextColor="#64748b"
        autoCapitalize="none"
        keyboardType="email-address"
        value={email}
        onChangeText={setEmail}
      />
      <TextInput
        style={styles.input}
        placeholder="Contraseña"
        placeholderTextColor="#64748b"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />

      {error && <Text style={styles.error}>⚠ {error}</Text>}

      <TouchableOpacity style={styles.button} onPress={submit} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Ingresar</Text>}
      </TouchableOpacity>

      <TouchableOpacity onPress={() => navigation.navigate('CustomerRegister')}>
        <Text style={styles.link}>¿No tienes cuenta? Regístrate aquí</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', padding: 24, justifyContent: 'center' },
  hero: { alignItems: 'center', marginBottom: 32 },
  mark: { fontSize: 40, marginBottom: 8 },
  title: { color: '#e2e8f0', fontSize: 26, fontWeight: '700' },
  subtitle: { color: '#94a3b8', fontSize: 14, marginTop: 4 },
  input: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 14,
    color: '#e2e8f0',
    fontSize: 15,
    marginBottom: 12,
  },
  button: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    padding: 15,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  error: { color: '#f87171', fontSize: 13, marginBottom: 8 },
  link: { color: '#2563eb', textAlign: 'center', marginTop: 20, fontSize: 14, fontWeight: '600' },
});
