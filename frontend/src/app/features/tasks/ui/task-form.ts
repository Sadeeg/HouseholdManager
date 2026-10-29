import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { FormField, form, maxLength, required, submit, validate } from '@angular/forms/signals';

import { UserRef } from '../../../core/users/user-search';
import { MarkdownView } from '../../../shared/components/markdown/markdown-view';
import { UserPicker } from '../../../shared/components/user-picker/user-picker';
import { Task, TaskFormData, formatDueDate, isoDate, normalizeLabel, parseDueDate } from '../model/task.model';

/**
 * Dialog „Aufgabe anlegen" / „Aufgabe bearbeiten" (UI-Schicht, dumb).
 *
 * Einziges Pflichtfeld ist der Titel (AC18). Zuständige kommen nur aus dem
 * Nutzerverzeichnis (AC8). Labels werden groß-/kleinschreibungsunabhängig
 * zusammengeführt: „bad" wird zu „Bad", wenn „Bad" schon existiert (AC20).
 * Ausgegeben werden die Formulardaten — der Aufrufer speichert.
 */
@Component({
  selector: 'app-task-form',
  imports: [FormField, MarkdownView, UserPicker],
  templateUrl: './task-form.html',
  styleUrl: './task-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskForm {
  /** Vorbelegung zum Bearbeiten; leer zum Anlegen. */
  readonly initial = input<Task | null>(null);
  /** True, solange der Speichern-Request läuft — Button deaktiviert. */
  readonly saving = input<boolean>(false);
  /** Übersetzte Fehlermeldung des Aufrufers; zeigt über den Aktionen. */
  readonly saveError = input<string | null>(null);
  /** Labels des Haushalts in gespeicherter Schreibweise (Vorschläge). */
  readonly labelSuggestions = input<readonly string[]>([]);

  readonly saved = output<TaskFormData>();
  readonly cancelled = output<void>();

  protected readonly model = signal({ title: '', content: '', dueDate: '' });
  protected readonly assignee = signal<UserRef | null>(null);
  protected readonly labels = signal<string[]>([]);
  protected readonly newLabel = signal('');
  protected readonly previewOpen = signal(false);

  protected readonly taskForm = form(this.model, (schema) => {
    required(schema.title, { message: 'Der Titel fehlt.' });
    maxLength(schema.title, 200, { message: 'Der Titel ist zu lang.' });
    validate(schema.dueDate, ({ value }) => {
      const raw = value().trim();
      if (raw === '' || parseDueDate(raw) !== null) {
        return undefined;
      }
      return {
        kind: 'date',
        message: 'Das Datum ist ungültig. Bitte im Format TT.MM.JJJJ eingeben.',
      };
    });
  });

  /** Die Frist liegt vor heute — Hinweis, kein Fehler (AC22). */
  protected readonly dueDateInPast = computed(() => {
    const iso = parseDueDate(this.model().dueDate);
    return iso !== null && iso < isoDate(new Date());
  });

  protected readonly knownLabels = computed(() => {
    const seen = new Map<string, string>();
    for (const label of [...this.labelSuggestions(), ...this.labels()]) {
      const key = normalizeLabel(label);
      if (!seen.has(key)) {
        seen.set(key, label);
      }
    }
    return seen;
  });

  constructor() {
    effect(() => {
      const task = this.initial();
      if (task === null) {
        return;
      }
      this.model.set({
        title: task.title,
        content: task.content ?? '',
        dueDate: task.dueDate === null ? '' : formatDueDate(task.dueDate),
      });
      this.labels.set([...task.labels]);
      if (task.assigneeUserId !== null) {
        this.assignee.set({
          id: task.assigneeUserId,
          displayName: task.assigneeDisplayName ?? task.assigneeUserId,
        });
      }
    });
  }

  protected selectAssignee(user: UserRef): void {
    this.assignee.set(user);
  }

  protected clearAssignee(): void {
    this.assignee.set(null);
  }

  /** Fügt ein Label hinzu; existierende Normalisierung führt zum gespeicherten Namen (AC20). */
  protected addLabel(): void {
    const raw = this.newLabel().trim();
    if (raw === '') {
      return;
    }
    const stored = this.knownLabels().get(normalizeLabel(raw)) ?? raw;
    if (!this.labels().some((label) => normalizeLabel(label) === normalizeLabel(stored))) {
      this.labels.update((labels) => [...labels, stored]);
    }
    this.newLabel.set('');
  }

  protected removeLabel(label: string): void {
    this.labels.update((labels) => labels.filter((existing) => existing !== label));
  }

  protected save(): void {
    submit(this.taskForm, async () => {
      const { title, content, dueDate } = this.model();
      this.saved.emit({
        title: title.trim(),
        content: content.trim() === '' ? null : content,
        assigneeUserId: this.assignee()?.id ?? null,
        dueDate: parseDueDate(dueDate),
        labels: this.labels(),
      });
    });
  }
}
