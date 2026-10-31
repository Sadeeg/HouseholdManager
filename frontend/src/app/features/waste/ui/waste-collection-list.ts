import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { WasteTerm, formatDay } from '../model/waste.model';

/**
 * Dumb Component (UI-Schicht): Liste der Mülltermine (AC4).
 *
 * Zeigt die Termine in der Reihenfolge, die die Anwendung liefert — aufsteigend
 * nach Datum, bei gleichem Datum nach Tonnenart. Das Component sortiert nicht
 * selbst: abgeleiteter Zustand gehört in den Data-Service (Regel der
 * Feature-Architektur), und die Reihenfolge ist dort entschieden.
 *
 * Keine Filter, keine Auswahl, keine Gruppen (R9). Daten kommen per Input.
 */
@Component({
  selector: 'app-waste-collection-list',
  templateUrl: './waste-collection-list.html',
  styleUrl: './waste-collection-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WasteCollectionList {
  readonly terms = input<readonly WasteTerm[]>([]);

  /** R12: ganze Tage — das Datum wird ohne Zeitzone gelesen (siehe `formatDay`). */
  protected dayLabel(date: string): string {
    return formatDay(date);
  }
}
