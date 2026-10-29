import { Component, inject, signal } from '@angular/core';
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

  /** Fehlertext, wenn der Login nicht starten konnte (sonst passiert nichts). */
  protected readonly error = signal<string | null>(null);

  protected async login(): Promise<void> {
    this.error.set(null);
    try {
      // Das Discovery-Dokument wird beim App-Start geladen. Fehlt es noch
      // (z. B. weil der erste Request fehlschlug), wird es hier nachgeladen.
      if (!this.oauthService.hasValidAccessToken()) {
        await this.oauthService.loadDiscoveryDocument();
      }
      this.oauthService.initCodeFlow();
    } catch {
      this.error.set(
        'Die Anmeldung konnte nicht gestartet werden. Bitte prüfen, ob der Nextcloud-Server erreichbar ist, und es erneut versuchen.',
      );
    }
  }
}
