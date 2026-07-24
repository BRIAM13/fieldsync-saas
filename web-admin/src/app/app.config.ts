import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { customerAuthInterceptor } from './core/interceptors/customer-auth.interceptor';
import { connectivityInterceptor } from './core/interceptors/connectivity.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // connectivity primero: observa el resultado final de cada petición (incluidos los
    // reintentos de los interceptores de auth tras un refresh de token). authInterceptor y
    // customerAuthInterceptor están acotados cada uno a sus propias rutas (staff vs cliente)
    // y no se pisan entre sí.
    provideHttpClient(
      withInterceptors([connectivityInterceptor, authInterceptor, customerAuthInterceptor]),
    ),
  ],
};
