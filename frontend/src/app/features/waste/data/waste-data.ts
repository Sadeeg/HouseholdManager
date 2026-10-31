import { httpResource } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';

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

  reload(): void {
    if (this.householdId() !== null) {
      this.overview.reload();
    }
  }
}
