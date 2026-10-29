import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard],
    loadChildren: () => import('./features/households/routes').then((m) => m.HOUSEHOLD_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
