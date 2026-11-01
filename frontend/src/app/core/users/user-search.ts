import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';

import { API_CONFIG } from '../config/api.config';

/** Ein Vorschlag aus dem Nutzerverzeichnis (ohne sensible Felder). */
export interface UserRef {
  readonly id: string;
  readonly displayName: string;
}

/** Stabile leere Liste — als `computed`-Ergebnis eine eigene Konstante. */
const NO_USERS: UserRef[] = [];

/**
 * Personensuche im Nutzerverzeichnis (GET api/users/search).
 *
 * App-weite Infrastruktur wie die Auth: das Verzeichnis kennt keine
 * Features. Die Suche startet ab zwei Zeichen; kürzere Queries liefern
 * bewusst keine Request.
 *
 * ## Instanz-Strategie (Issue #4)
 *
 * `core/` ist laut ADR 0002 „genau eine Instanz und app-weit". Das
 * Nutzerverzeichnis ist genau das: Infrastruktur wie die Auth, von jedem
 * Feature gebraucht, ohne Fachwissen über ein Feature. Deshalb ist die
 * Kapsel **root-provided** (`providedIn: 'root'`) — es gibt genau eine
 * `httpResource` für die ganze App: ein Request pro Suchbegriff, nicht einer
 * pro Suchfeld.
 *
 * Der Umzug nach `shared/` wäre die Alternative gewesen. Dagegen spricht die
 * Projektregel in `shared/README.md`: dort landet kein Code, der mit dem
 * Backend spricht.
 *
 * **Geteilter Zustand:** Query und Treffer gehören der einen Instanz, alle
 * Suchfelder sehen denselben Stand. Damit jedes Feld so frisch startet wie
 * zuvor, setzt der `UserPicker` den Zustand beim Anlegen und nach einer
 * Auswahl zurück (`reset()`); Treffer werden erst ab zwei Zeichen gezeigt.
 * Dass nie zwei Suchfelder offen sind, ist eine App-Invariante: auf der Seite
 * „Haushalte" ist genau ein Haushalt aufgeklappt (`expandedHouseholdId` — ein
 * Signal), im Aufgaben-Board genau ein Task-Formular, die Routen sind
 * getrennt. Belegt in `households-page-user-picker.spec.ts`.
 */
@Injectable({ providedIn: 'root' })
export class UserSearch {
  private readonly http = inject(HttpClient);

  /** Suchbegriff — geteilt von allen Suchfeldern der App. */
  readonly query = signal('');

  private readonly searchResource = httpResource<UserRef[]>(() => {
    const query = this.query().trim();
    if (query.length < 2) {
      return undefined;
    }
    return `${API_CONFIG.basePath}/users/search?q=${encodeURIComponent(query)}`;
  });

  /**
   * Treffer der letzten Suche; leer, solange nicht gesucht wurde.
   *
   * Unter zwei Zeichen bleibt die Liste leer — auch wenn die Resource noch
   * einen alten Wert hält: nach `reset()` darf kein Treffer stehen bleiben.
   */
  readonly results = computed<readonly UserRef[]>(() => {
    if (this.query().trim().length < 2) {
      return NO_USERS;
    }
    return this.searchResource.value() ?? NO_USERS;
  });
  readonly searching = computed(() => this.query().trim().length >= 2 && this.searchResource.isLoading());
  readonly failed = computed(
    () => this.query().trim().length >= 2 && this.searchResource.error() != null,
  );

  search(query: string): void {
    this.query.set(query);
  }

  reset(): void {
    this.query.set('');
  }
}
