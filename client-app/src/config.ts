/**
 * Configuración del endpoint del backend para la app cliente.
 *
 * Apunta al backend en producción (Render + Aiven) — funciona desde cualquier dispositivo
 * o emulador sin configuración adicional.
 *
 * Para desarrollo local contra un backend en tu máquina (`gradle run` en `backend/`),
 * descomenta el bloque de abajo: `10.0.2.2` es el alias del localhost del host desde el
 * emulador de Android; iOS Simulator usa `localhost`; un dispositivo físico necesita la
 * IP LAN de tu PC (p. ej. `192.168.1.x`).
 */
const PROD_HOST = 'fieldsync-backend-cipm.onrender.com';

// import { Platform } from 'react-native';
// const LOCAL_HOST = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
// export const API_BASE = `http://${LOCAL_HOST}:8080`;
// export const WS_BASE = `ws://${LOCAL_HOST}:8080`;

export const API_BASE = `https://${PROD_HOST}`;
export const WS_BASE = `wss://${PROD_HOST}`;
