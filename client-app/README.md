# FieldSync — App Cliente (React Native)

App móvil multiplataforma para el **cliente final**. Muestra el **seguimiento del técnico en tiempo real** (característica clave #3).

## Stack

- **React Native** (Expo) + **TypeScript**
- Hook `useTechnicianTracking` — encapsula el stream en tiempo real (WebSocket/Firebase en producción; simulado aquí)

## Por qué está en el portafolio

Demuestra **versatilidad móvil**: entiendo tanto el desarrollo **nativo** (la app de técnicos en Kotlin) como el **multiplataforma** (esta app en React Native), y sé argumentar cuándo conviene cada uno.

## Estructura

```
client-app/
├── App.tsx
└── src/
    ├── screens/    TrackingScreen.tsx   (UI de seguimiento en vivo)
    └── services/   TrackingService.ts   (hook del stream en tiempo real)
```

## Ejecutar

```bash
npm install
npm start        # expo start
```
