import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { CurrentUser, WorkspaceHousehold } from './workspace.model';

/**
 * Der Workspace: der gerade gewählte Haushalt des angemeldeten Nutzers.
 *
 * App-weit genau eine Instanz (core): der Workspace ist Zustand der App,
 * nicht eines Features — Aufgaben-Tafel, Haushaltsverwaltung und App-Kopf
 * lesen dieselben Signale. Ein Wechsel ist eine Zustandsänderung, kein
 * Seitenwechsel: alle Views reagieren auf `currentHouseholdId`.
 *
 * Geladen wird erst nach der Anmeldung (`ensureLoaded` wird von der
 * Start-Route und dem Workspace-Wähler aufgerufen, nie im Konstruktor).
 */
@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly http = inject(HttpClient);

  /** Die Haushalte, in denen die angemeldete Person ernannt ist. */
  readonly availableHouseholds = signal<WorkspaceHousehold[]>([]);
  /** Der gewählte Haushalt; null ohne Auswahl oder gemerkte Auswahl. */
  readonly currentHouseholdId = signal<number | null>(null);
  /** Die angemeldete Person (für Rollen-Vergleiche wie „Haushaltsleitung“). */
  readonly currentUser = signal<CurrentUser | null>(null);
  /** True, wenn der Dienst beim Laden nicht erreichbar war. */
  readonly loadFailed = signal(false);

  readonly currentHousehold = computed(() =>
    this.availableHouseholds().find((household) => household.id === this.currentHouseholdId()) ?? null,
  );

  private loaded = false;
  private loading: Promise<void> | null = null;

  /** Lädt Haushalte, gemerkten Workspace und angemeldete Person — genau einmal. */
  ensureLoaded(): Promise<void> {
    if (this.loaded) {
      return Promise.resolve();
    }
    if (this.loading) {
      return this.loading;
    }
    this.loading = this.load().then(() => {
      this.loaded = true;
      this.loading = null;
    });
    return this.loading;
  }

  /** Merkt einen neuen Workspace. Ohne Mitgliedschaft lehnt das Backend ab. */
  async selectHousehold(householdId: number): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.put<unknown>(this.workspaceUrl(), { householdId }),
      );
      this.currentHouseholdId.set(householdId);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Nimmt einen gerade angelegten Haushalt auf: er ist bereits Mitglied
   * (die anlegende Person ist Leitung), erscheint also sofort in der Liste
   * und wird zum Workspace.
   */
  async adoptHousehold(household: { id: number; name: string }): Promise<boolean> {
    if (!this.availableHouseholds().some((known) => known.id === household.id)) {
      this.availableHouseholds.update((households) => [...households, household]);
    }
    return this.selectHousehold(household.id);
  }

  private async load(): Promise<void> {
    const households = await firstValueFrom(
      this.http.get<WorkspaceHousehold[]>(`${API_CONFIG.basePath}/households`),
    ).catch(() => null);

    if (households === null) {
      this.loadFailed.set(true);
      return;
    }
    this.availableHouseholds.set(toWorkspaceHouseholds(households));

    const [selected, user] = await Promise.all([
      // 404 = keine gemerkte Auswahl — das ist ein normaler Zustand.
      firstValueFrom(this.http.get<number>(this.workspaceUrl())).catch(() => null),
      firstValueFrom(this.http.get<CurrentUser>(`${API_CONFIG.basePath}/auth/me`)).catch(() => null),
    ]);

    this.currentUser.set(user);
    this.applySelection(selected);
  }

  /** Eine gemerkte Auswahl gilt nur, wenn die Mitgliedschaft noch besteht. */
  private applySelection(selected: number | null): void {
    const valid =
      selected !== null && this.availableHouseholds().some((household) => household.id === selected);
    this.currentHouseholdId.set(valid ? selected : null);
  }

  private workspaceUrl(): string {
    return `${API_CONFIG.basePath}/me/workspace`;
  }
}

function toWorkspaceHouseholds(households: WorkspaceHousehold[]): WorkspaceHousehold[] {
  return households.map(({ id, name }) => ({ id, name }));
}
