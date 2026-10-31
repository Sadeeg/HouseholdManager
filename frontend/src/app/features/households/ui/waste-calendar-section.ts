import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { FormField, form, maxLength, required, submit } from '@angular/forms/signals';

import { WASTE_URL_MAX_LENGTH, WASTE_URL_WARN_LENGTH } from '../model/household.model';

/**
 * Dumb Component (UI-Schicht): Abschnitt „MÜLLKALENDER" eines Haushalts —
 * dasselbe Muster wie der Mitglieder-Abschnitt, für die ICS-Adresse des
 * Entsorgers (AC23).
 *
 * Jedes Mitglied darf die Adresse ändern. Das Component kennt kein Backend:
 * die gespeicherte Adresse kommt per Input, jede Änderung wird per Output
 * gemeldet — der Aufrufer speichert und bestätigt das Entfernen.
 *
 * Die Zeichenlänge wird gezeigt (Muster „Zeichenlänge"): Zählform
 * „128 / 500", ab 90 % der Grenze warnend, darüber fehlerhaft. „Abbrechen"
 * stellt die gespeicherte Adresse wieder her (R19); ohne gespeicherte Adresse
 * ist das Entfernen nicht möglich.
 */
@Component({
  selector: 'app-waste-calendar-section',
  imports: [FormField],
  templateUrl: './waste-calendar-section.html',
  styleUrl: './waste-calendar-section.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WasteCalendarSection {
  readonly householdId = input.required<number>();
  readonly householdName = input.required<string>();
  /** Aktuell gespeicherte Adresse — Bezugspunkt für „Abbrechen" und „Entfernen". */
  readonly storedUrl = input<string | null>(null);
  /** True, solange der Speichern-Request läuft — Aktionen deaktiviert. */
  readonly saving = input<boolean>(false);
  /** Übersetzte Fehlermeldung des Aufrufers; zeigt über den Aktionen. */
  readonly saveError = input<string | null>(null);

  /** Grenze der ICS-Adresse — die Anzeige nennt sie (R18). */
  protected readonly maxLength = WASTE_URL_MAX_LENGTH;

  readonly saveRequested = output<string>();
  readonly removeRequested = output<void>();

  protected readonly model = signal({ url: '' });
  protected readonly savedUrl = signal<string | null>(null);

  protected readonly urlForm = form(this.model, (schema) => {
    required(schema.url, { message: 'Die ICS-Adresse fehlt.' });
    maxLength(schema.url, WASTE_URL_MAX_LENGTH, {
      message: `Die ICS-Adresse darf höchstens ${WASTE_URL_MAX_LENGTH} Zeichen lang sein.`,
    });
  });

  constructor() {
    effect(() => {
      const stored = this.storedUrl();
      this.savedUrl.set(stored);
      this.model.set({ url: stored ?? '' });
    });
  }

  protected readonly length = computed(() => this.model().url.trim().length);

  /** Muster Zeichenlänge: warnen ab 90 % der Grenze, darüber fehlerhaft. */
  protected readonly counterLevel = computed<'normal' | 'warn' | 'danger'>(() => {
    const length = this.length();
    if (length > WASTE_URL_MAX_LENGTH) {
      return 'danger';
    }
    if (length >= WASTE_URL_WARN_LENGTH) {
      return 'warn';
    }
    return 'normal';
  });

  /** Speichern gibt es nur für eine geänderte, nicht leere, nicht zu lange Adresse. */
  protected readonly canSave = computed(
    () =>
      this.length() > 0 &&
      this.length() <= WASTE_URL_MAX_LENGTH &&
      this.model().url.trim() !== (this.savedUrl() ?? '') &&
      !this.saving(),
  );

  protected save(): void {
    submit(this.urlForm, async () => {
      this.saveRequested.emit(this.model().url.trim());
    });
  }

  /** R19: Abbrechen verwirft die Eingabe und stellt die gespeicherte Adresse her. */
  protected cancel(): void {
    this.model.set({ url: this.savedUrl() ?? '' });
  }

  /** Die Adresse kann nur entfernt werden, wenn eine gespeichert ist. */
  protected readonly canRemove = computed(() => this.savedUrl() !== null);
}
