import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';

import { UserRef, UserSearch } from '../../../core/users/user-search';

/**
 * Personensuche aus dem Nutzerverzeichnis (AC8, AC11): Suchfeld ab zwei
 * Zeichen, Ergebnisliste mit Name + @login, Auswahl per Output.
 *
 * Die Suche ist app-weite Infrastruktur (core); die Instanz gehört zur
 * Komponente — jedes offene Suchfeld hat seinen eigenen Query-Zustand.
 * Was mit der Auswahl geschieht, entscheidet der Aufrufer.
 */
@Component({
  selector: 'app-user-picker',
  providers: [UserSearch],
  templateUrl: './user-picker.html',
  styleUrl: './user-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPicker {
  private readonly search = inject(UserSearch);

  readonly selected = output<UserRef>();

  protected readonly results = this.search.results;
  protected readonly searching = this.search.searching;
  protected readonly failed = this.search.failed;

  protected query = '';

  protected searchNow(value: string): void {
    this.query = value;
    this.search.search(value);
  }

  protected pick(user: UserRef): void {
    this.search.reset();
    this.query = '';
    this.selected.emit(user);
  }
}
