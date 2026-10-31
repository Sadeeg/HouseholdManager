import { httpResource } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { WorkspaceService } from '../../../core/workspace/workspace-service';
import { WasteApi } from './waste-api';
import { WasteOverview, WasteTerm, sortTerms } from '../model/waste.model';

/**
 * Reaktiver Lesezugriff auf die Übersicht der Mülltermine.
 *
 * Route-scoped providers (siehe `routes.ts`): der Zustand lebt nur, solange
 * das Feature aktiv ist. Der Request folgt dem Workspace — wechselt der
 * gewählte Haushalt, lädt die Übersicht neu (AC11). Das Feature folgt dem
 * Workspace, ohne ihn selbst zu kennen.
 */
@Injectable()
export class WasteData {
  private readonly wasteApi = inject(WasteApi);
  private readonly workspace = inject(WorkspaceService);

  readonly householdId = computed(() => this.workspace.currentHouseholdId());

  readonly overview = httpResource<WasteOverview>(() => {
    const householdId = this.householdId();
    if (householdId === null) {
      return undefined;
    }
    return this.wasteApi.overviewUrl(householdId);
  });

  /** Termine in Reihenfolge der Anzeige (R9, AC4) — aufsteigend, dann nach Tonnenart. */
  readonly terms = computed<WasteTerm[]>(() =>
    sortTerms(this.overview.value()?.collections ?? []),
  );

  /** War die Seite schon einmal betreten worden? */
  private readonly visited = signal(false);

  /**
   * Beim erneuten Betreten der Seite wird neu gelesen.
   *
   * `httpResource` feuert nur, wenn sich die Request-URL ändert. Solange die
   * Route-scoped-Instanz den Wegwechsel überlebt (das ist ohne
   * `withAutoCleanupInjectors()` der Fall, und auch mit einer
   * `RouteReuseStrategy`, die Handles behält), zeigte die Seite beim zweiten
   * Besuch den Stand des ersten — der Nutzer glaubte, das Speichern habe
   * nicht gewirkt (R6, `## Nutzerfluss` Schritt 7: die Anzeige zeigt den Stand
   * beim nächsten Aufruf).
   *
   * Im Normalfall zerstört `withAutoCleanupInjectors()` den Dienst beim
   * Verlassen der Route; dann liest der neu erzeugte Dienst ohnehin. Dieser
   * Aufruf ist die Sicherung für den Fall, dass die Instanz überlebt.
   */
  onEnter(): void {
    if (this.visited()) {
      this.reload();
    }
    this.visited.set(true);
  }

  reload(): void {
    if (this.householdId() !== null) {
      this.overview.reload();
    }
  }
}
