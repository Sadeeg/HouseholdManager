import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { WorkspaceService } from './workspace-service';

/**
 * Workspace-Wähler im App-Kopf: wählt den aktuellen Haushalt.
 *
 * Design-DNA: Auslöser mit Name + „▾", Liste auf --hm-z-dropdown,
 * aktuelle Zeile mit Mono-Label „AKTUELL". Die Auswahl wechselt den
 * Zustand — die Seite wird nicht neu geladen (AC23).
 */
@Component({
  selector: 'app-workspace-switcher',
  imports: [RouterLink],
  templateUrl: './workspace-switcher.html',
  styleUrl: './workspace-switcher.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceSwitcher {
  protected readonly workspace = inject(WorkspaceService);

  protected readonly open = signal(false);
  protected readonly households = this.workspace.availableHouseholds;
  protected readonly currentHousehold = this.workspace.currentHousehold;

  protected readonly hasHouseholds = computed(() => this.households().length > 0);

  protected toggle(): void {
    void this.workspace.ensureLoaded();
    this.open.update((open) => !open);
  }

  protected async select(householdId: number): Promise<void> {
    this.open.set(false);
    if (householdId !== this.workspace.currentHouseholdId()) {
      await this.workspace.selectHousehold(householdId);
    }
  }
}
