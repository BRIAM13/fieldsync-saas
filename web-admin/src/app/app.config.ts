import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { connectivityInterceptor } from './core/interceptors/connectivity.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // connectivity primero: observa el resultado final de cada petición (incluidos los
    // reintentos de authInterceptor tras un refresh de token).
    provideHttpClient(withInterceptors([connectivityInterceptor, authInterceptor])),
  ],
};
