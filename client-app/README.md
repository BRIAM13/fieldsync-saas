# FieldSync — App Cliente (React Native)

App móvil multiplataforma para el **cliente final**: se registra bajo la empresa que le presta
el servicio, **solicita un servicio** con su ubicación real (GPS), y sigue a su técnico en
**tiempo real** una vez asignado (característica clave #3).

## Stack

- **React Native** (Expo) + **TypeScript**
- **React Navigation** (stack) — Login → Registro → Mis solicitudes → Nueva solicitud → Seguimiento
- **expo-location** — captura la ubicación real del cliente al crear una solicitud, para que la
  orden aparezca como pin en el mapa de despacho (Angular) igual que las creadas por staff
- Hook `useTechnicianTracking` — abre un **WebSocket real** al backend Ktor
  (`ws://.../ws/tracking/{orderId}`) y actualiza la posición del técnico con cada mensaje

> La URL base está en `src/config.ts`, apuntando por defecto a producción (Render + Aiven).
> Para desarrollo local contra un backend en tu máquina, ver el comentario en ese archivo
> (`10.0.2.2` para el emulador Android, `localhost` para iOS Simulator/web).

### Autenticación de cliente

Cuenta **separada** de la de staff (Angular): `POST /customer/auth/{register,login,refresh}`,
con su propio JWT (`customerId`, sin `userId`) — un cliente nunca puede autenticar en las rutas
de staff, y viceversa. La sesión se persiste en AsyncStorage (`CustomerAuthService.ts`) y se
renueva sola al abrir la app.

## Por qué está en el portafolio

Demuestra **versatilidad móvil**: entiendo tanto el desarrollo **nativo** (la app de técnicos en
Kotlin) como el **multiplataforma** (esta app en React Native), y sé argumentar cuándo conviene
cada uno. También cierra el ciclo completo del dominio: un cliente solicita → el staff (Angular)
lo ve y asigna → el técnico (Android) lo atiende → el cliente (aquí) lo sigue en vivo.

## Estructura

```
client-app/
├── App.tsx                          NavigationContainer + RootNavigator
├── index.js                         entry point (registerRootComponent)
└── src/
    ├── navigation/RootNavigator.tsx stack de pantallas
    ├── screens/
    │   ├── CustomerLoginScreen.tsx
    │   ├── CustomerRegisterScreen.tsx   (elige empresa de una lista pública)
    │   ├── MyRequestsScreen.tsx         (lista + pull-to-refresh)
    │   ├── RequestServiceScreen.tsx     (form + captura de GPS obligatoria)
    │   └── TrackingScreen.tsx           (seguimiento en vivo de una orden)
    └── services/
        ├── CustomerAuthService.ts   registro/login/sesión del cliente
        ├── CustomerService.ts       empresas, crear/listar solicitudes
        ├── LocationService.ts       wrapper de expo-location
        └── TrackingService.ts       hook del WebSocket en tiempo real
```

## Ejecutar

```bash
npm install
npm start        # expo start — escanea el QR con Expo Go, o pulsa a/i para emulador/simulador
npm run web       # alternativa rápida: corre en el navegador (útil para probar sin dispositivo)
```

## 📸 Capturas

Graba el flujo completo (registro → solicitud con GPS → seguimiento en vivo) con el backend
corriendo (ver la [guía de captura](../docs/CAPTURES.md)) y colócalo en `docs/media/`. Luego
descomenta:

<!-- ![Solicitar servicio y seguimiento en tiempo real](../docs/media/client-tracking.gif) -->
