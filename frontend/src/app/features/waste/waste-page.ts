import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { PageHeader } from '../../shared/components/page-header/page-header';
import { WasteData } from './data/waste-data';
import { WasteCollectionList } from './ui/waste-collection-list';
import { WasteSourcePanel } from './ui/waste-source-panel';

/**
 * Smart Component (Feature-Schicht) des Features „Mülltermine".
 *
 * Holt die Übersicht über den Data-Service und folgt damit dem gewählten
 * Haushalt (AC11). Die Seite entscheidet über ihre Zustände — kein eigener
 * Synchronisationsknopf: der Lauf gehört zur Anwendung (R4). Die dumb
 * Components der UI-Schicht bekommen nur Daten.
 */
@Component({
  selector: 'app-waste-page',
  imports: [PageHeader, RouterLink, WasteSourcePanel, WasteCollectionList],
  templateUrl: './waste-page.html',
  styleUrl: './waste-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WastePage implements OnInit {
  protected readonly wasteData = inject(WasteData);
  protected readonly workspace = inject(WorkspaceService);

  protected readonly householdId = computed(() => this.workspace.currentHouseholdId());

  protected readonly overview = this.wasteData.overview;
  protected readonly terms = this.wasteData.terms;

  /** AC14: ohne gewählten Haushalt zeigt die Seite den Grund, nicht eine leere Liste. */
  protected readonly showNoHousehold = computed(() => this.householdId() === null);

  protected readonly loading = computed(() => this.overview.isLoading() && this.householdId() !== null);

  protected readonly failed = computed(() => this.overview.error() != null);

  /** R14: ohne Quelle ist nichts zu lesen. */
  protected readonly showNoSource = computed(
    () => !this.loading() && !this.failed() && !this.showNoHousehold() && (this.overview.value()?.sourceUrl ?? null) === null,
  );

  /** R15: Quelle vorhanden, aber kein Termin im Fenster. */
  protected readonly showNoTerms = computed(
    () => !this.loading() && !this.failed() && !this.showNoHousehold() && !this.showNoSource() && this.terms().length === 0,
  );

  protected retry(): void {
    this.wasteData.reload();
  }

  /** Beim erneuten Betreten der Seite wird der Stand neu gelesen (R6). */
  ngOnInit(): void {
    this.wasteData.onEnter();
  }
}
