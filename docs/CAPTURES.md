# Guía de captura — screenshots y GIFs

El GIF hero del README raíz sale del **video promocional** (HyperFrames), que muestra la UI
*simulada*. Esta guía es para grabar **capturas reales de cada app** en tu máquina (donde tienes
el backend corriendo, Android Studio y un emulador) y colocarlas en `docs/media/`.

> Requisito común: el **backend corriendo** (`cd backend && gradle run` o `docker compose up`).
> Login demo: `admin@fieldsync.dev` / `demo1234`.

## Convención de nombres (lo que esperan los README)

| Archivo en `docs/media/` | Qué mostrar | Estado |
|--------------------------|-------------|--------|
| `web-login.png` | Pantalla de login del panel Angular | ✅ real, contra producción |
| `web-dispatch.gif` | Asignar una orden a un técnico en el mapa | ✅ real, contra producción |
| `android-tasks.png` | Lista de órdenes en la app del técnico | ✅ real, contra producción |
| `android-offline.gif` | Badge "pendiente" → "sincronizado" al volver la red | ✅ real, contra producción |
| `client-tracking.gif` | Login/registro → solicitar servicio con GPS → seguimiento en vivo | ✅ real, contra producción |

---

## Panel web (Angular)

1. `cd web-admin && npm start` → abre http://localhost:4200 e inicia sesión.
2. **Screenshot:** recorta la ventana del navegador (Win: `Win`+`Shift`+`S`; macOS: `Cmd`+`Shift`+`4`).
3. **GIF:** graba con [ScreenToGif](https://www.screentogif.com/) (Windows) o
   [LICEcap](https://www.cockos.com/licecap/) (Win/mac); exporta a `docs/media/web-dispatch.gif`.

## App Android (Kotlin)

Con un emulador o dispositivo (`adb`) conectado y la app corriendo:

```bash
# Screenshot
adb exec-out screencap -p > docs/media/android-tasks.png

# Grabar vídeo (detén con Ctrl+C), luego convertir a GIF con el comando de abajo
adb shell screenrecord /sdcard/rec.mp4
adb pull /sdcard/rec.mp4 rec.mp4
```

## App cliente (React Native / Expo)

1. `cd client-app && npm start` con el emulador Android (`adb`) o Expo Go.
2. Graba con `adb shell screenrecord` (igual que arriba) o la grabación de pantalla del dispositivo.
3. Convierte el `.mp4` a GIF con el comando de abajo → `docs/media/client-tracking.gif`.

---

## Convertir cualquier grabación .mp4 → GIF limpio (ffmpeg)

Mismo método de paleta que usamos para el GIF del promo (buena calidad, tamaño contenido):

```bash
ffmpeg -y -i rec.mp4 -vf "fps=12,scale=760:-1:flags=lanczos,palettegen=stats_mode=diff" palette.png
ffmpeg -y -i rec.mp4 -i palette.png \
  -lavfi "fps=12,scale=760:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=3" \
  docs/media/<nombre>.gif
```

Ajusta `fps` (10–15) y `scale` (640–800) para equilibrar fluidez y peso. Apunta a **< 5 MB** por GIF.

## Insertar en un README

```markdown
![Login del panel](docs/media/web-login.png)
```

Los README de cada app ya tienen una sección **📸 Capturas** con el hueco listo; solo descomenta
la línea de la imagen cuando el archivo exista en `docs/media/`.
