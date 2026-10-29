import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

/**
 * Anmeldeseite: der Nutzer meldet sich über Nextcloud (OIDC) an.
 *
 * <p>Kein Passwort-Formular — die Anmeldung läuft beim Identity Provider.
 */
@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);

  protected login(): void {
    this.oauthService.initCodeFlow();
  }

  protected async continueAfterLogin(): Promise<void> {
    await this.oauthService.tryLoginCodeFlow();
    if (this.oauthService.hasValidAccessToken()) {
      await this.router.navigateByUrl('/');
    }
  }
}
