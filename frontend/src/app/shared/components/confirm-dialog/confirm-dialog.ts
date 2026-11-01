import { CdkTrapFocus } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Bestätigungsdialog-Muster der Design-DNA: destruktive Aktion braucht eine
 * bewusste Bestätigung. Text nennt Zahl und Objekt; ohne Bestätigung ändert
 * sich nichts.
 *
 * Dumb Component: Titel, Beschreibung und Aktion kommen per Input, das
 * Ergebnis per Output. Der Aufrufer führt die Aktion aus.
 *
 * Fokus (Issue #9): das Panel bekommt beim Öffnen den initialen Fokus, der
 * Tab bleibt im Dialog (Fokusfalle), und beim Schließen kehrt der Fokus auf
 * das auslösende Element zurück.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [CdkTrapFocus],
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
  /** Text während der Aktion — zur Aktion passend benennen (z. B. „Wird entfernt …“). */
  readonly busyLabel = input<string>('Wird gelöscht …');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
