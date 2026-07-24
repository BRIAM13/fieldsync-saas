import React, { useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { hasSession } from '../services/CustomerAuthService';
import CustomerLoginScreen from '../screens/CustomerLoginScreen';
import CustomerRegisterScreen from '../screens/CustomerRegisterScreen';
import MyRequestsScreen from '../screens/MyRequestsScreen';
import RequestServiceScreen from '../screens/RequestServiceScreen';
import TrackingScreen from '../screens/TrackingScreen';

export type RootStackParamList = {
  CustomerLogin: undefined;
  CustomerRegister: undefined;
  MyRequests: undefined;
  RequestService: undefined;
  Tracking: { orderId: string };
};

const Stack = createNativeStackNavigator<RootStackParamList>();

const navTheme = {
  ...DarkTheme,
  colors: { ...DarkTheme.colors, background: '#0f172a', card: '#111a2e', border: '#22304d' },
};

const screenOptions = {
  headerStyle: { backgroundColor: '#111a2e' },
  headerTintColor: '#e2e8f0',
  headerShadowVisible: false,
  contentStyle: { backgroundColor: '#0f172a' },
};

/** Raíz de navegación: si hay sesión de cliente guardada, arranca en "Mis solicitudes". */
export default function RootNavigator() {
  const [initialRoute, setInitialRoute] = useState<keyof RootStackParamList | null>(null);

  useEffect(() => {
    hasSession().then((ok) => setInitialRoute(ok ? 'MyRequests' : 'CustomerLogin'));
  }, []);

  if (!initialRoute) {
    return (
      <View style={{ flex: 1, backgroundColor: '#0f172a', alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator color="#2563eb" />
      </View>
    );
  }

  return (
    <NavigationContainer theme={navTheme}>
      <Stack.Navigator initialRouteName={initialRoute} screenOptions={screenOptions}>
        <Stack.Screen
          name="CustomerLogin"
          component={CustomerLoginScreen}
          options={{ title: 'Ingresar', headerShown: false }}
        />
        <Stack.Screen
          name="CustomerRegister"
          component={CustomerRegisterScreen}
          options={{ title: 'Crear cuenta' }}
        />
        <Stack.Screen
          name="MyRequests"
          component={MyRequestsScreen}
          options={{ title: 'Mis solicitudes', headerBackVisible: false }}
        />
        <Stack.Screen
          name="RequestService"
          component={RequestServiceScreen}
          options={{ title: 'Nueva solicitud' }}
        />
        <Stack.Screen
          name="Tracking"
          component={TrackingScreen}
          options={{ title: 'Seguimiento' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
