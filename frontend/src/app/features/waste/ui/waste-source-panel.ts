import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Dumb Component (UI-Schicht): Panel „Quelle und Stand" der Seite
 * „Mülltermine" (AC5).
 *
 * Zeigt die hinterlegte ICS-Adresse und den Stand des letzten erfolgreichen
 * Laufs. Der veraltete Stand wird nur gezeigt, wenn die Anwendung ihn als
 * veraltet meldet (R13) — hier wird nichts daraus abgeleitet. Ohne erfolgreichen
 * Lauf steht „Noch keine Synchronisation" (R16); ohne Quelle nennt die Anzeige
 * die Grenze (R18) und dass jedes Mitglied die Adresse eintragen kann (R14).
 *
 * Alle Daten kommen per Input; das Component redet nicht mit dem Backend.
 */
@Component({
  selector: 'app-waste-source-panel',
  imports: [DatePipe],
  templateUrl: './waste-source-panel.html',
  styleUrl: './waste-source-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WasteSourcePanel {
  readonly sourceUrl = input<string | null>(null);
  readonly lastSyncedAt = input<string | null>(null);
  /** Nur wahr, wenn die Anwendung den Stand als veraltet meldet (AC5). */
  readonly stale = input<boolean>(false);
}
