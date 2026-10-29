import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AuthConfig, OAuthService, provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { OIDC_CONFIG } from './core/config/oidc.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),

    // OIDC-Client (Nextcloud als Identity Provider).
    // Achtung: die Library injiziert die Konfiguration über die KLASSE
    // `AuthConfig` — der Token `AUTH_CONFIG` wird intern nicht verwendet.
    // Mit dem falschen Token bleibt `issuer` leer, die Discovery-URL wird
    // relativ und der Login bricht lautlos ab.
    {
      provide: AuthConfig,
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

    /**
     * Das Discovery-Dokument wird **nicht** vom Konstruktor geladen.
     * Ohne diesen Initialisiser bleibt die Login-URL leer und `initCodeFlow()`
     * wartet vergeblich auf das Discovery-Event — der Klick tut dann nichts.
     * `loadDiscoveryDocumentAndTryLogin()` lädt es und verarbeitet zugleich
     * einen Callback (Code im URL), falls einer vorliegt.
     */
    provideAppInitializer(() =>
      inject(OAuthService)
        .loadDiscoveryDocumentAndTryLogin()
        .catch(() => false),
    ),

    // HTTP mit Bearer-Token für die eigene API
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
