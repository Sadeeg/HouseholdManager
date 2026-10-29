import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'households' },
  {
    path: 'households',
    loadChildren: () => import('./features/households/routes').then((m) => m.HOUSEHOLD_ROUTES),
  },
  { path: '**', redirectTo: 'households' },
];
