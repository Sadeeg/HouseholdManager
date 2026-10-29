import { Routes } from '@angular/router';

import { TasksPage } from './tasks-page';
import { TasksData } from './data/tasks-data';

/**
 * Routen des Features „Aufgaben".
 *
 * Der Data-Service wird route-scoped bereitgestellt: sein Zustand lebt nur,
 * solange das Feature aktiv ist. Der Workspace selbst ist core — er überlebt
 * das Feature und wird vom Workspace-Wähler gesetzt.
 */
export const TASK_ROUTES: Routes = [
  {
    path: '',
    component: TasksPage,
    providers: [TasksData],
  },
];
