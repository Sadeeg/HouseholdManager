import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';

import { UserRef, UserSearch } from '../../../core/users/user-search';

/**
 * Personensuche aus dem Nutzerverzeichnis (AC8, AC11): Suchfeld ab zwei
 * Zeichen, Ergebnisliste mit Name + @login, Auswahl per Output.
 *
 * Die Suche ist app-weite Infrastruktur (core) mit **genau einer** Instanz
 * (Issue #4): alle Felder teilen sich Query und Treffer. Damit ein Feld
 * frisch startet, setzt die Komponente den geteilten Zustand beim Anlegen
 * zurück — sichtbar bleibt derselbe Stand wie zuvor: leeres Feld, keine
 * Treffer. Was mit der Auswahl geschieht, entscheidet der Aufrufer.
 */
@Component({
  selector: 'app-user-picker',
  templateUrl: './user-picker.html',
  styleUrl: './user-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPicker {
  private readonly search = inject(UserSearch);

  readonly selected = output<UserRef>();

  protected readonly query = this.search.query;
  protected readonly results = this.search.results;
  protected readonly searching = this.search.searching;
  protected readonly failed = this.search.failed;

  constructor() {
    // Der geteilte Zustand darf nicht aus einem anderen Suchfeld übernommen werden.
    this.search.reset();
  }

  protected searchNow(value: string): void {
    this.search.search(value);
  }

  protected pick(user: UserRef): void {
    this.search.reset();
    this.selected.emit(user);
  }
}
