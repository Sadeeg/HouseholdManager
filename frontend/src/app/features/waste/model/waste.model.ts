/**
 * Datenmodell des Features „Mülltermine".
 *
 * Zeitstempel kommen als ISO-8601-String aus dem Backend (Jackson 3
 * serialisiert `Instant` als Text). `stale` ist die Aussage der Anwendung —
 * die Anzeige leitet sie nicht aus dem Alter des Zeitstempels ab (R13).
 * Ein Termin ist ganztägig: es gibt keine Uhrzeit (R12).
 */
export interface WasteTerm {
  readonly date: string;
  readonly wasteType: string;
}

export interface WasteOverview {
  readonly sourceUrl: string | null;
  readonly lastSyncedAt: string | null;
  readonly stale: boolean;
  readonly collections: readonly WasteTerm[];
}

/**
 * R12: ein Termin ist ganztägig. Das Datum wird aus dem ISO-Wert gelesen, ohne
 * Zeitzone — sonst zeigt eine Zeitzone hinter UTC den vorherigen Tag.
 */
export function formatDay(date: string): string {
  const [year, month, day] = date.split('-');
  return `${day}.${month}.${year}`;
}

/**
 * Reihenfolge der Anzeige (R9, AC4, Annahme 8): aufsteigend nach Datum, bei
 * gleichem Datum nach Tonnenart. Reine Funktion — die geladene Liste bleibt
 * unangetastet.
 */
export function sortTerms(terms: readonly WasteTerm[]): WasteTerm[] {
  return [...terms].sort(
    (a, b) => a.date.localeCompare(b.date) || a.wasteType.localeCompare(b.wasteType, 'de'),
  );
}
