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
}
