/**
 * Basis-Pfad der Backend-API.
 *
 * Im Development leitet der Dev-Proxy (`proxy.conf.json`) `/api` auf
 * `http://localhost:8080`; in Production macht das der nginx-Reverse-Proxy.
 */
export const API_CONFIG = {
  basePath: '/api',
} as const;
