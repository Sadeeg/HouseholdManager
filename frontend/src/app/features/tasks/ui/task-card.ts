import { CdkDrag } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { Task, TaskStatus, formatDueDate, isOverdue, labelColorIndex } from '../model/task.model';

/**
 * Dumb Component: eine Kanban-Karte.
 *
 * Nicht als Ganzes klickbar — sie ist die Drag-Fläche; Aktionen sind
 * explizite Buttons (Design-DNA). Status wird nicht über Farbe gezeigt;
 * DONE-Titel ist nur gedämpft. Kennzeichen („ÜBERFÄLLIG",
 * „NICHT IM HAUSHALT") sind Text-Badges.
 */
@Component({
  selector: 'app-task-card',
  imports: [CdkDrag],
  templateUrl: './task-card.html',
  styleUrl: './task-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCard {
  readonly task = input.required<Task>();
  /** True, solange die Karte gerade verschoben/gelöscht wird — Aktionen deaktiviert. */
  readonly busy = input<boolean>(false);

  readonly editRequested = output<Task>();
  readonly deleteRequested = output<Task>();
  readonly moved = output<TaskStatus>();

  protected readonly moveMenuOpen = signal(false);
  protected readonly overdue = computed(() => isOverdue(this.task()));
  protected readonly otherStatuses = computed(() =>
    (['TODO', 'DOING', 'DONE'] as TaskStatus[]).filter((status) => status !== this.task().status),
  );

  protected colorIndex(label: string): number {
    return labelColorIndex(label);
  }

  protected dueDateLabel(iso: string): string {
    return formatDueDate(iso);
  }

  protected toggleMoveMenu(): void {
    this.moveMenuOpen.update((open) => !open);
  }

  protected moveTo(status: TaskStatus): void {
    this.moveMenuOpen.set(false);
    this.moved.emit(status);
  }
}
