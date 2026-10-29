import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';

import { API_CONFIG } from '../config/api.config';

/** Ein Vorschlag aus dem Nutzerverzeichnis (ohne sensible Felder). */
export interface UserRef {
  readonly id: string;
  readonly displayName: string;
}

/**
 * Personensuche im Nutzerverzeichnis (GET api/users/search).
 *
 * App-weite Infrastruktur wie die Auth: das Verzeichnis kennt keine
 * Features. Die Suche startet ab zwei Zeichen; kürzere Queries liefern
 * bewusst keine Request.
 *
 * Nicht root-provided: jede consuming Komponente erhält ihre eigene
 * Instanz mit eigenem Query-Zustand (providers: [UserSearch]).
 */
@Injectable()
export class UserSearch {
  private readonly http = inject(HttpClient);

  readonly query = signal('');

  private readonly searchResource = httpResource<UserRef[]>(() => {
    const query = this.query().trim();
    if (query.length < 2) {
      return undefined;
    }
    return `${API_CONFIG.basePath}/users/search?q=${encodeURIComponent(query)}`;
  });

  /** Treffer der letzten Suche; leer, solange nicht gesucht wurde. */
  readonly results = computed(() => this.searchResource.value() ?? []);
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
