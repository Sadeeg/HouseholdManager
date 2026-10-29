import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { UserRef } from '../../../core/users/user-search';
import { UserPicker } from '../../../shared/components/user-picker/user-picker';
import { Member } from '../model/member.model';

/**
 * Dumb Component: Mitglieder-Abschnitt eines Haushalts (AC11).
 *
 * Zeigt die Mitglieder (Leitung zuerst, Badge „HAUSHALTSLEITUNG“) und
 * erlaubt das Ernennen und Entfernen von Personen. Alle Daten kommen per
 * Input, jede Änderung wird per Output gemeldet — der Aufrufer orchestriert
 * und bestätigt das Entfernen.
 *
 * „Ernennen" ist eine ständige Primäraktion neben dem Suchfeld: ohne
 * Auswahl ist sie deaktiviert, nach der Auswahl einer Person freigegeben.
 * So ist der Schritt zum Zuordnen immer sichtbar.
 */
@Component({
  selector: 'app-member-section',
  imports: [UserPicker],
  templateUrl: './member-section.html',
  styleUrl: './member-section.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberSection {
  readonly members = input<readonly Member[] | null>(null);
  readonly loading = input<boolean>(false);
  readonly failed = input<boolean>(false);

  readonly addMember = output<string>();
  readonly removeRequested = output<Member>();
  readonly retry = output<void>();

  /** Angemeldete Person — die Leitung selbst kann nicht entfernt werden. */
  readonly currentUserId = input<string | null>(null);

  protected readonly pendingPerson = signal<UserRef | null>(null);

  protected isPendingMember(person: UserRef): boolean {
    return (this.members() ?? []).some((member) => member.userId === person.id);
  }

  /** „Ernennen" geht nur mit ausgewählter Person, die noch nicht Mitglied ist. */
  protected readonly canAppoint = computed(() => {
    const person = this.pendingPerson();
    return person !== null && !this.isPendingMember(person);
  });

  protected selectPerson(user: UserRef): void {
    this.pendingPerson.set(user);
  }

  /** Gibt die Auswahl auf, ohne zu ernennen. */
  protected clearPerson(): void {
    this.pendingPerson.set(null);
  }

  protected appoint(): void {
    const person = this.pendingPerson();
    if (person === null) {
      return;
    }
    this.addMember.emit(person.id);
    this.pendingPerson.set(null);
  }
}
