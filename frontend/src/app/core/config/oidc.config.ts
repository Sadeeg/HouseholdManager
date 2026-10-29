/**
 * OIDC-Konfiguration des Frontends (Nextcloud als Identity Provider).
 *
 * Öffentliche Werte — kein Secret. Es gibt nur *einen* Client: den
 * public Frontend-Client auf dem Nextcloud. Das Backend ist reiner
 * Resource Server und prüft die JWT-Access-Tokens über den JWKS des
 * Issuers — es braucht deshalb kein Client-Secret; die Client-ID nutzt es
 * optional zur Audience-Bindung (`nextcloud.oidc.client-id`).
 *
 * Der Redirect-URI wird aus der Laufzeit-Origin abgeleitet: damit funktioniert
 * die App auf jedem Port (4200 lokal, 4300 im Test-Setup, 80 hinter nginx).
 * Der Nextcloud-Client muss die passende URI als Redirect-URI kennen
 * (`occ oidc:list`).
 */
const origin = typeof window === 'undefined' ? 'http://localhost:4200' : window.location.origin;

export const OIDC_CONFIG = {
  issuer: 'https://cloud.deeg-mail.de',
  clientId: '1b3ba5574d4542aeab30dc3211f23698dcbe404b46d193642f0152f5e9a46c78',
  redirectUri: origin,
  scope: 'openid profile email',
} as const;
