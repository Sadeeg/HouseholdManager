/**
 * Modell des Workspaces (core, app-weit).
 *
 * Bewusst schmaler Sicht auf Haushalte und angemeldete Person: der
 * Workspace-Wähler und die Start-Routen brauchen nur diese Felder.
 * Feature-Modelle bleiben davon unberührt.
 */
export interface WorkspaceHousehold {
  readonly id: number;
  readonly name: string;
}

export interface CurrentUser {
  readonly id: string;
  readonly displayName: string;
}
