/**
 * URL base del backend Ktor, centralizada.
 *
 * Apunta al backend en producción (Render + Aiven). Para desarrollo local contra un
 * backend corriendo en tu máquina (`gradle run` en `backend/`), cambia temporalmente a
 * `http://localhost:8080` — no lo dejes así al commitear.
 */
export const API_BASE = 'https://fieldsync-backend-cipm.onrender.com';
