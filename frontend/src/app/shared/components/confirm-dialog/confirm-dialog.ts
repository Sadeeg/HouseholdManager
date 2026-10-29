import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Bestätigungsdialog-Muster der Design-DNA: destruktive Aktion braucht eine
 * bewusste Bestätigung. Text nennt Zahl und Objekt; ohne Bestätigung ändert
 * sich nichts.
 *
 * Dumb Component: Titel, Beschreibung und Aktion kommen per Input, das
 * Ergebnis per Output. Der Aufrufer führt die Aktion aus.
 */
@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly detail = input<string>();
  readonly actionLabel = input<string>('Endgültig löschen');
  /** True, solange die Aktion läuft („Wird gelöscht …“, Aktionen deaktiviert). */
  readonly busy = input<boolean>(false);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
