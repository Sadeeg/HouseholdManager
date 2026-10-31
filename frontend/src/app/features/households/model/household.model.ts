/**
 * Datenmodell des Features „Haushalte".
 *
 * Zeitstempel kommen als ISO-8601-String aus dem Backend (Jackson 3
 * serialisiert `Instant` standardmäßig als Text, nicht als Zahl).
 * `createdBy` ist die Kennung der Haushaltsleitung — der Frontend-Test
 * „bin ich die Leitung?“ vergleicht sie mit der angemeldeten Person.
 */
export interface Household {
  readonly id: number;
  readonly name: string;
  readonly createdAt: string;
  readonly createdBy: string;
  readonly wasteCalendarUrl: string | null;
}

/**
 * Grenze der ICS-Adresse — dieselbe Grenze wie die Spalte
 * `household.waste_calendar_url` im Backend (R18). Die Anzeige nennt sie.
 */
export const WASTE_URL_MAX_LENGTH = 500;

/** warnen ab 90 % der Grenze (Muster Zeichenlänge), danach danger. */
export const WASTE_URL_WARN_LENGTH = 450;

/** Antwort des Backends beim Setzen der Quelle — die clientseitige Sicht. */
export interface WasteSourceResponse {
  readonly sourceUrl: string | null;
}
