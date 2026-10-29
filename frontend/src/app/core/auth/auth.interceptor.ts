import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

/**
 * Hängt das Bearer-Token an API-Requests (Nextcloud/OIDC).
 *
 * <p>Nur für eigene API-Pfade — der Token geht nicht an fremde Hosts.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const oauthService = inject(OAuthService);
  const isApiRequest = request.url.startsWith('/api');

  if (!isApiRequest || !oauthService.hasValidAccessToken()) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${oauthService.getAccessToken()}` },
    }),
  );
};
