/**
 * Configuración del endpoint del backend para la app cliente.
 *
 * `10.0.2.2` es el alias del localhost de la máquina anfitriona desde el emulador de Android.
 * - iOS Simulator: usa `localhost`.
 * - Dispositivo físico: usa la IP LAN del servidor (p. ej. 192.168.1.x).
 */
import { Platform } from 'react-native';

const HOST = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';

export const API_BASE = `http://${HOST}:8080`;
export const WS_BASE = `ws://${HOST}:8080`;
