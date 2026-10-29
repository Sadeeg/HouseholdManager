/**
 * OIDC-Konfiguration des Frontends (Nextcloud als Identity Provider).
 *
 * Öffentliche Werte — kein Secret. Die Client-ID gehört zum *Frontend*-Client
 * (public) auf dem Nextcloud; das Backend hat einen eigenen confidential
 * Client, dessen Secret ausschließlich in `.env` steht (siehe `.env.example`).
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
