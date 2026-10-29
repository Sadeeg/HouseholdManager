/**
 * Datenmodell des Features „Haushalte".
 *
 * Zeitstempel kommen als ISO-8601-String aus dem Backend (Jackson 3
 * serialisiert `Instant` standardmäßig als Text, nicht als Zahl).
 */
export interface Household {
  readonly id: number;
  readonly name: string;
  readonly createdAt: string;
}
