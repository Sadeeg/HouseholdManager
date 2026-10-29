import { Routes } from '@angular/router';

import { HouseholdsPage } from './households-page';
import { HouseholdsData } from './data/households-data';

/**
 * Routen des Features „Haushalte".
 *
 * Der Data-Service wird route-scoped bereitgestellt: sein Zustand (die
 * httpResource) lebt damit nur, solange das Feature aktiv ist, und wird beim
 * erneuten Betreten der Route neu geladen.
 */
export const HOUSEHOLD_ROUTES: Routes = [
  {
    path: '',
    component: HouseholdsPage,
    providers: [HouseholdsData],
  },
];
