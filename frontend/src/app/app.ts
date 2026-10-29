import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { WorkspaceService } from './core/workspace/workspace-service';
import { WorkspaceSwitcher } from './core/workspace/workspace-switcher';

/**
 * App-Shell: Kopf mit Produktname, Navigation und Workspace-Wähler.
 *
 * Der Navigationspunkt „AUFGABEN" erscheint nur mit gewähltem Workspace
 * (AC12/AC23) — der Punkt „HAUSHALTE" ist immer sichtbar.
 */
@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, WorkspaceSwitcher],
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly workspace = inject(WorkspaceService);

  protected readonly hasWorkspace = computed(() => this.workspace.currentHouseholdId() !== null);
}
