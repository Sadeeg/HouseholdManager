/**
 * OIDC-Konfiguration des Frontends (Nextcloud als Identity Provider).
 *
 * Öffentliche Werte — kein Secret. Die Client-ID gehört zum *Frontend*-Client
 * (public) auf dem Nextcloud; das Backend hat einen eigenen confidential
 * Client, dessen Secret ausschließlich in `.env` steht (siehe `.env.example`).
 *
 * Anpassen, wenn sich Adresse oder Port ändern — und den Redirect-URI
 * passend auf dem Nextcloud-Client (`occ oidc:list`).
 */
export const OIDC_CONFIG = {
  issuer: 'https://cloud.deeg-mail.de',
  clientId: '164c390ed24f324934e31e3c44ca3d171b456b0d09296e85d6c4f22caa5fb493',
  redirectUri: 'http://localhost:4200',
  scope: 'openid profile email',
  // RFC 9728 Resource Indicator — muss zum registrierten Client passen
  resource: 'http://localhost:8080/api',
} as const;
