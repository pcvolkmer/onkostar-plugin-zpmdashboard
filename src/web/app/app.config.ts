import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {MockBackendInterceptor} from '../mock-backend.interceptor';
import {environment} from '../environment';

const providers: any[] = [
  provideBrowserGlobalErrorListeners(),
  provideRouter(routes)
];

if (environment.useMockApi) {
  console.log("Using MockBackendInterceptor");
  providers.push(
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MockBackendInterceptor,
      multi: true
    }
  );
} else {
  console.log("Production");
  providers.push(provideHttpClient());
}

export const appConfig: ApplicationConfig = {
  providers: providers
};
