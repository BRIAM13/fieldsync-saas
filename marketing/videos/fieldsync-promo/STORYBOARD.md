---
format: 1920x1080
message: "FieldSync conecta oficina, técnico y cliente en un solo flujo — incluso sin señal."
arc: Hook → Despacho → Offline-first → Tiempo real → CTA
audience: Empresas de servicios de campo (y reclutadores que evalúan el portafolio)
mode: collaborative
silent: true
music: none
---

## Frame 1 — Hook / Portada

- scene: El wordmark "FieldSync" entra bajo una línea cobalto; el tagline aparece debajo.
- duration: 4s
- transition_in: cut
- status: animated
- src: compositions/frames/01-hook.html
- voiceover: (silencioso) EN PANTALLA — "FieldSync" · "Tu operación de campo, sincronizada."
- asset_candidates: construido en el frame (portada tipográfica, panel diagonal + grid de puntos del preset)

Apertura en frío sobre la promesa. Tratamiento **Cover** del preset: canvas cream, panel diagonal
cobalto a la derecha, grid de puntos 3×3. `h1` near-black a la izquierda bajo la accent-line + meta
"GESTIÓN DE SERVICIOS DE CAMPO". Establece la marca y la tesis que pagan los tres frames siguientes.

## Frame 2 — Asignación inteligente en mapa (Angular)

- scene: Un mock del panel de despacho; una orden de trabajo se asigna al técnico más cercano en el mapa.
- duration: 5s
- transition_in: crossfade
- status: animated
- src: compositions/frames/02-map-dispatch.html
- voiceover: (silencioso) EN PANTALLA — "Asigna en segundos" · eyebrow "PANEL WEB · ANGULAR"
- asset_candidates: construido en el frame (mock de mapa con pines de órdenes + lista de técnicos; UI en HTML/CSS)

Tratamiento **Split + Highlight**: slide-header (eyebrow cobalto "DESPACHO INTELIGENTE" + tag-pill
"ANGULAR"), `h2` "Asigna la orden al técnico más cercano". A la derecha, un mock del mapa de despacho
(lienzo oscuro, pines de órdenes) donde un pin de orden se conecta con una línea cobalto al técnico
más próximo y cambia de estado a "Asignada". Prueba la característica #1.

## Frame 3 — Sincronización offline-first (Kotlin · Room + Coroutines)

- scene: Mock de la app del técnico; se corta la señal, un cambio queda "pendiente" y luego "sincronizado".
- duration: 5s
- transition_in: crossfade
- status: animated
- src: compositions/frames/03-offline-sync.html
- voiceover: (silencioso) EN PANTALLA — "Funciona sin señal" · eyebrow "APP ANDROID · KOTLIN"
- asset_candidates: construido en el frame (mock de lista de órdenes con badge pendiente→sincronizado; toggle de señal)

Tratamiento **Split + Highlight** espejado. Eyebrow "OFFLINE-FIRST", tag-pill "ROOM · COROUTINES".
`h2` "El técnico trabaja aunque no haya señal". Mock de la app: un ícono de señal se apaga, una orden
cambia a estado "En progreso" con un badge cobalto "• pendiente de sincronizar"; al reaparecer la
señal el badge pasa a verde "✓ sincronizado". Es el corazón técnico del portafolio Android. #2.

## Frame 4 — Seguimiento en tiempo real (React Native)

- scene: Mock de la app del cliente; un pin de técnico avanza en el mapa y el ETA baja en vivo.
- duration: 5s
- transition_in: crossfade
- status: animated
- src: compositions/frames/04-realtime-tracking.html
- voiceover: (silencioso) EN PANTALLA — "Tu cliente lo ve llegar" · eyebrow "APP CLIENTE · REACT NATIVE"
- asset_candidates: construido en el frame (mock de tarjeta de seguimiento con pin en movimiento + ETA descendente)

Tratamiento **Split + Highlight**. Eyebrow "SEGUIMIENTO EN VIVO", tag-pill "REACT NATIVE". `h2`
"El cliente sigue a su técnico en tiempo real". Mock de la app cliente: un pin cobalto se desplaza por
una ruta hacia un destino mientras un contador de ETA cae "18 → 8 min" y aparece "En camino". #3.

## Frame 5 — Cierre / CTA

- scene: Las tres piezas convergen en el wordmark; aparece el CTA "Tu operación, sincronizada".
- duration: 4s
- transition_in: crossfade
- status: animated
- src: compositions/frames/05-cta.html
- voiceover: (silencioso) EN PANTALLA — "FieldSync" · CTA "Tu operación de campo, sincronizada."
- asset_candidates: construido en el frame (cierre tipográfico con rings concéntricos + botón CTA)

Tratamiento **Closing / CTA**: canvas cream con rings concéntricos, `h1` "FieldSync" centrado bajo la
accent-line, y el único botón cobalto sólido con "Tu operación de campo, sincronizada.". Tres tags
—Angular · Kotlin · React Native— cierran el argumento de portafolio. Sello final.
