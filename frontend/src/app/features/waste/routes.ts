import { Routes } from '@angular/router';

import { WastePage } from './waste-page';
import { WasteData } from './data/waste-data';

/**
 * Routen des Features „Mülltermine".
 *
 * Der Data-Service wird route-scoped bereitgestellt: sein Zustand lebt nur,
 * solange das Feature aktiv ist. Der Workspace selbst ist core — er überlebt
 * das Feature und wird vom Workspace-Wähler gesetzt.
 */
export const WASTE_ROUTES: Routes = [
  {
    path: '',
    component: WastePage,
    providers: [WasteData],
  },
];
