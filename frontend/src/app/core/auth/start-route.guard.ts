import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { WorkspaceService } from '../workspace/workspace-service';

/**
 * Start-Route: löst über den Workspace auf.
 *
 * Mit gewähltem Haushalt → Aufgaben-Tafel, ohne → Haushaltsverwaltung.
 * Der Workspace ist dafür einmalig geladen (nach dem authGuard).
 */
export const startRouteGuard: CanActivateFn = async () => {
  const workspace = inject(WorkspaceService);
  const router = inject(Router);

  await workspace.ensureLoaded();

  return router.createUrlTree(workspace.currentHouseholdId() !== null ? ['/aufgaben'] : ['/haushalte']);
};
