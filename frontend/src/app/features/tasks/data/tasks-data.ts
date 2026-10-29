import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import { WorkspaceService } from '../../../core/workspace/workspace-service';
import { Task, TaskFormData, TaskStatus } from '../model/task.model';
import { TaskApi } from './task-api';

/**
 * Reaktiver Lesezugriff auf die Aufgaben des gewählten Haushalts.
 *
 * Route-scoped providers (siehe `routes.ts`): der Zustand lebt nur, solange
 * das Feature aktiv ist. Der Request folgt dem Workspace — wechselt der
 * gewählte Haushalt, lädt die Tafel neu, ohne dass die Seite wechselt
 * (AC23).
 */
@Injectable()
export class TasksData {
  private readonly taskApi = inject(TaskApi);
  private readonly workspace = inject(WorkspaceService);

  readonly householdId = computed(() => this.workspace.currentHouseholdId());

  readonly tasks = httpResource<Task[]>(() => {
    const householdId = this.householdId();
    if (householdId === null) {
      return undefined;
    }
    return `${API_CONFIG.basePath}/households/${householdId}/tasks`;
  });

  /** Die Aufgaben einer Spalte, in Positionsreihenfolge. */
  readonly tasksFor = computed(() => (status: TaskStatus): Task[] =>
    this.board()
      .filter((task) => task.status === status)
      .sort((a, b) => a.position - b.position),
  );

  /** Alle im Haushalt verwendeten Labels in gespeicherter Schreibweise (Vorschläge, AC20). */
  readonly knownLabels = computed(() => {
    const seen = new Map<string, string>();
    for (const task of this.board()) {
      for (const label of task.labels) {
        const key = label.trim().toLowerCase();
        if (!seen.has(key)) {
          seen.set(key, label);
        }
      }
    }
    return [...seen.values()];
  });

  /**
   * Ein Zug, der der Tafel vorgezeigt wird, bis der Server ihn liefert.
   *
   * Ohne diesen Zwischenstand fällt die Karte beim Drop in ihre alte Bahn
   * zurück und taucht dort erst nach dem Neuladen wieder auf. Er gilt,
   * solange der geladene Serverstand identisch ist; sobald eine neue
   * Antwort eintrifft, zählt der Serverstand (Identitätsvergleich der
   * geladenen Liste) — der Zwischenstand ist dann wirkungslos.
   */
  private readonly pendingMove = signal<PendingMove | null>(null);

  private readonly board = computed<Task[]>(() => {
    const tasks = this.tasks.value() ?? [];
    const pending = this.pendingMove();
    if (pending === null || tasks !== pending.serverTasks) {
      return tasks;
    }
    return applyMove(tasks, pending);
  });

  private readonly mutationCount = signal(0);
  /** Zählt erfolgreiche Änderungen — Signale der Seiten können darauf reagieren. */
  readonly mutations = this.mutationCount.asReadonly();

  async create(form: TaskFormData): Promise<Task> {
    const task = await firstValueFrom(this.taskApi.create(this.requiredHouseholdId(), form));
    this.reloadTasks();
    return task;
  }

  async update(taskId: number, form: TaskFormData): Promise<Task> {
    const task = await firstValueFrom(this.taskApi.update(this.requiredHouseholdId(), taskId, form));
    this.reloadTasks();
    return task;
  }

  async move(taskId: number, status: TaskStatus, position: number): Promise<Task> {
    // Zwischenstand zuerst: die Karte bleibt an der Zielstelle liegen, während
    // die Änderung gespeichert wird.
    this.pendingMove.set({ taskId, status, position, serverTasks: this.tasks.value() });
    try {
      const task = await firstValueFrom(
        this.taskApi.move(this.requiredHouseholdId(), taskId, status, position),
      );
      this.reloadTasks();
      return task;
    } catch (error) {
      // Fehlert: der Zwischenstand darf nicht übrig bleiben.
      this.pendingMove.set(null);
      throw error;
    }
  }

  async remove(taskId: number): Promise<void> {
    await firstValueFrom(this.taskApi.delete(this.requiredHouseholdId(), taskId));
    this.reloadTasks();
  }

  /** Entfernt alle Aufgaben der DONE-Spalte; gibt die Anzahl vor dem Löschen zurück. */
  async deleteDone(): Promise<number> {
    const count = this.board().filter((task) => task.status === 'DONE').length;
    await firstValueFrom(this.taskApi.deleteDone(this.requiredHouseholdId()));
    this.reloadTasks();
    return count;
  }

  private reloadTasks(): void {
    this.mutationCount.update((count) => count + 1);
    if (this.householdId() !== null) {
      this.tasks.reload();
    }
  }

  private requiredHouseholdId(): number {
    const householdId = this.householdId();
    if (householdId === null) {
      throw new Error('Kein Haushalt gewählt.');
    }
    return householdId;
  }
}

/** Ein vorgemerkter Zug: Karte, Ziel und der Stand, auf dem er aufsetzt. */
interface PendingMove {
  readonly taskId: number;
  readonly status: TaskStatus;
  readonly position: number;
  /** Die Liste, die der Zwischenstand ersetzt — identische Liste heißt: noch nicht bestätigt. */
  readonly serverTasks: Task[] | undefined;
}

/**
 * Ein Zug, so normalisiert wie im Backend: Zielspalte ohne die Karte, Karte an
 * die Wunschposition (begrenzt) einfügen, beide betroffenen Bahnen neu
 * durchnummerieren. Reine Funktion auf Kopien — die geladene Liste bleibt unangetastet.
 */
function applyMove(tasks: Task[], move: PendingMove): Task[] {
  const moving = tasks.find((task) => task.id === move.taskId);
  if (moving === undefined) {
    return tasks;
  }

  const others = tasks.filter((task) => task.id !== move.taskId);
  const columnWithout = (status: TaskStatus): Task[] =>
    others
      .filter((task) => task.status === status)
      .sort((a, b) => a.position - b.position);

  const target = columnWithout(move.status);
  const source = columnWithout(moving.status);
  const position = Math.max(0, Math.min(move.position, target.length));

  const movedTarget = [
    ...target.slice(0, position),
    { ...moving, status: move.status },
    ...target.slice(position),
  ].map((task, index) => ({ ...task, position: index }));

  // Quellbahn nur neu nummerieren, wenn der Status wechselt (wie im Backend).
  const renumberedSource =
    moving.status === move.status ? [] : source.map((task, index) => ({ ...task, position: index }));

  const untouched = others.filter(
    (task) => task.status !== moving.status && task.status !== move.status,
  );

  return [...untouched, ...renumberedSource, ...movedTarget];
}
