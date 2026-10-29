import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AUTH_CONFIG, provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { OIDC_CONFIG } from './core/config/oidc.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),

    // OIDC-Client (Nextcloud als Identity Provider)
    {
      provide: AUTH_CONFIG,
      useValue: {
        issuer: OIDC_CONFIG.issuer,
        clientId: OIDC_CONFIG.clientId,
        redirectUri: OIDC_CONFIG.redirectUri,
        scope: OIDC_CONFIG.scope,
        responseType: 'code',
        showDebugInformation: false,
      },
    },
    provideOAuthClient(),

    // HTTP mit Bearer-Token für die eigene API
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
