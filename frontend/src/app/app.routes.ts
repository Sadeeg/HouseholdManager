import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { startRouteGuard } from './core/auth/start-route.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  // Start: löst über den Workspace auf — mit gewähltem Haushalt zur Tafel,
  // ohne zur Haushaltsverwaltung.
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard, startRouteGuard],
  },
  {
    path: 'haushalte',
    canActivate: [authGuard],
    loadChildren: () => import('./features/households/routes').then((m) => m.HOUSEHOLD_ROUTES),
  },
  {
    path: 'aufgaben',
    canActivate: [authGuard],
    loadChildren: () => import('./features/tasks/routes').then((m) => m.TASK_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
