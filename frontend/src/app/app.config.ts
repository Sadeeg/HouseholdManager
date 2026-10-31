import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withAutoCleanupInjectors } from '@angular/router';
import { AuthConfig, OAuthService, provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { OIDC_CONFIG } from './core/config/oidc.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    /**
     * `withAutoCleanupInjectors()` zerstört die route-scoped Provider, sobald
     * ihre Route verlassen wird. Das ist die Voraussetzung dafür, dass ein
     * Feature beim erneuten Betreten wieder frisch liest: `httpResource` feuert
     * nur, wenn sich die Request-URL ändert — eine überlebende Instanz zeigte
     * also den alten Stand („Mülltermine" zeigt den Stand beim nächsten
     * Aufruf).
     *
     * Die Option wirkt auf **alle** Features: Tasks, Haushalte und Mülltermine
     * legen keinen Zustand über Navigationswechsel hinweg, der das Verhalten
     * überleben müsste.
     */
    provideRouter(routes, withAutoCleanupInjectors()),

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
