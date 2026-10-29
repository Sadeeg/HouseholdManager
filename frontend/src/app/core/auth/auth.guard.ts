import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

/**
 * Schützt Routen: ohne gültiges Token geht es zur Anmeldung.
 *
 * <p>Gilt nur für die App-Routen — der Login selbst ist öffentlich.
 */
export const authGuard: CanActivateFn = async (_route, state) => {
  const oauthService = inject(OAuthService);
  const router = inject(Router);

  if (oauthService.hasValidAccessToken()) {
    return true;
  }

  // Rückkehr vom Identity Provider (Code Flow) verarbeiten
  try {
    await oauthService.tryLoginCodeFlow();
    if (oauthService.hasValidAccessToken()) {
      return true;
    }
  } catch {
    // Kein Callback im URL → normal zur Anmeldung
  }

  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
