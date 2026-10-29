import { CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { PageHeader } from '../../shared/components/page-header/page-header';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { TasksData } from './data/tasks-data';
import { Task, TaskFormData, TaskStatus } from './model/task.model';
import { TaskCard } from './ui/task-card';
import { TaskForm } from './ui/task-form';

/**
 * Smart Component (Feature-Schicht) des Features „Aufgaben".
 *
 * Die Tafel gehört zum gewählten Haushalt (AC23): wechselt der Workspace,
 * lädt der Data-Service neu — ohne Seitenwechsel. Drag & Drop ist der
 * primäre Weg; jede Bahnwechsel-Aktion hat ein Tastatur-Äquivalent
 * („Verschieben nach …" auf der Karte).
 */
@Component({
  selector: 'app-tasks-page',
  imports: [CdkDropList, RouterLink, PageHeader, ConfirmDialog, TaskCard, TaskForm],
  templateUrl: './tasks-page.html',
  styleUrl: './tasks-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TasksPage {
  protected readonly tasksData = inject(TasksData);
  protected readonly workspace = inject(WorkspaceService);

  // Dialogzustand
  protected readonly formOpen = signal(false);
  protected readonly editingTask = signal<Task | null>(null);
  protected readonly savingForm = signal(false);
  protected readonly formError = signal<string | null>(null);

  // Bestätigungsdialoge
  protected readonly pendingTaskDelete = signal<Task | null>(null);
  protected readonly pendingClearDone = signal(false);
  protected readonly deleting = signal(false);

  // Runende Verschiebungen (Karte deaktiviert, AC6/AC4)
  protected readonly busyTaskId = signal<number | null>(null);

  // Barrierefreiheit: Statuswechsel werden angesagt.
  protected readonly announcement = signal('');
  protected readonly toast = signal<string | null>(null);

  protected readonly loadFailed = computed(() => this.tasksData.tasks.error() != null);

  protected readonly columns = computed(() =>
    (['TODO', 'DOING', 'DONE'] as TaskStatus[]).map((status) => ({
      status,
      tasks: this.tasksData.tasksFor()(status),
    })),
  );

  protected readonly doneCount = computed(() => this.columns()[2].tasks.length);
  protected readonly clearDoneDescription = computed(
    () => `${this.doneCount()} Aufgaben in „DONE“ werden endgültig gelöscht.`,
  );
  protected readonly clearDoneDetail = computed(
    () => 'Aufgaben in „TODO“ und „DOING“ bleiben unverändert.',
  );
  protected readonly isEmptyBoard = computed(
    () => this.columns().every((column) => column.tasks.length === 0),
  );

  protected listId(status: TaskStatus): string {
    return `board-${status}`;
  }

  protected connectedLists(status: TaskStatus): string[] {
    return (['TODO', 'DOING', 'DONE'] as TaskStatus[])
      .filter((other) => other !== status)
      .map((other) => this.listId(other));
  }

  /** Drop in einer Bahn: Ziel-Index ist die neue Position; das Backend normalisiert beide Bahnen. */
  protected onDrop(status: TaskStatus, event: CdkDragDrop<Task[]>): void {
    // Die Daten-verschiebende Arbeit liegt im Data-Service (Zwischenstand bis
    // der Server antwortet). Hier wird nur der Zug gemeldet — ohne diesen
    // Zwischenstand fällt die Karte in die alte Bahn zurück.
    void this.moveTask(event.previousContainer.data[event.previousIndex], status, event.currentIndex);
  }

  /** Tastatur-Alternative: „Verschieben nach …" hängt die Karte ans Ende der Zielbahn. */
  protected moveToColumn(task: Task, status: TaskStatus): void {
    void this.moveTask(task, status, this.tasksData.tasksFor()(status).length);
  }

  private async moveTask(task: Task, status: TaskStatus, position: number): Promise<void> {
    if (task.status === status) {
      const currentTasks = this.tasksData.tasksFor()(status);
      const currentPosition = currentTasks.findIndex((other) => other.id === task.id);
      if (currentPosition === position) {
        return;
      }
    }
    this.busyTaskId.set(task.id);
    try {
      await this.tasksData.move(task.id, status, position);
      this.announcement.set(
        status === task.status
          ? 'Aufgabe verschoben.'
          : `Aufgabe nach ${status} verschoben.`,
      );
    } catch {
      this.announcement.set('Die Aufgabe konnte nicht verschoben werden.');
    } finally {
      this.busyTaskId.set(null);
    }
  }

  protected openCreate(): void {
    this.editingTask.set(null);
    this.formError.set(null);
    this.formOpen.set(true);
  }

  protected openEdit(task: Task): void {
    this.editingTask.set(task);
    this.formError.set(null);
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editingTask.set(null);
    this.savingForm.set(false);
    this.formError.set(null);
  }

  protected async saveForm(form: TaskFormData): Promise<void> {
    this.savingForm.set(true);
    try {
      const editing = this.editingTask();
      if (editing === null) {
        await this.tasksData.create(form);
        this.announcement.set('Aufgabe in TODO angelegt.');
      } else {
        await this.tasksData.update(editing.id, form);
        this.announcement.set('Aufgabe aktualisiert.');
      }
      this.closeForm();
    } catch (error) {
      this.formError.set(this.storageErrorMessage(error));
    } finally {
      this.savingForm.set(false);
    }
  }

  protected async confirmTaskDelete(): Promise<void> {
    const task = this.pendingTaskDelete();
    if (task === null) {
      return;
    }
    this.deleting.set(true);
    try {
      await this.tasksData.remove(task.id);
      this.pendingTaskDelete.set(null);
      this.announcement.set('Aufgabe gelöscht.');
    } catch {
      this.announcement.set('Die Aufgabe konnte nicht gelöscht werden.');
    } finally {
      this.deleting.set(false);
    }
  }

  protected async confirmClearDone(): Promise<void> {
    this.deleting.set(true);
    try {
      const count = await this.tasksData.deleteDone();
      this.pendingClearDone.set(false);
      this.showToast(`${count} Aufgaben gelöscht.`);
      this.announcement.set(`${count} Aufgaben gelöscht.`);
    } catch {
      this.announcement.set(
        'Die erledigten Aufgaben konnten nicht gelöscht werden: Der Dienst ist nicht erreichbar. Es wurde keine Aufgabe gelöscht.',
      );
    } finally {
      this.deleting.set(false);
    }
  }

  protected showClearDone(): void {
    this.pendingClearDone.set(true);
  }

  private showToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => this.toast.set(null), 4000);
  }

  private storageErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return 'Die Aufgabe konnte nicht gespeichert werden: Der Titel fehlt.';
    }
    return 'Die Aufgabe konnte nicht gespeichert werden: Der Dienst ist nicht erreichbar. Deine Eingaben bleiben erhalten.';
  }
}
