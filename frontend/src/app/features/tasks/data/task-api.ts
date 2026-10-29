import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import { Task, TaskFormData, TaskStatus } from '../model/task.model';

/**
 * Zugriff auf die Aufgaben-Ressource des Backends (Daten-Schicht).
 *
 * Lesezugriffe laufen über `httpResource` (siehe `TasksData`),
 * Schreibzugriffe hier über `HttpClient`.
 */
@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);

  create(householdId: number, form: TaskFormData): Observable<Task> {
    return this.http.post<Task>(this.tasksUrl(householdId), this.toPayload(form));
  }

  update(householdId: number, taskId: number, form: TaskFormData): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl(householdId)}/${taskId}`, this.toPayload(form));
  }

  /** Verschieben: Ziel-Spalte und Ziel-Position; das Backend normalisiert beide Bahnen. */
  move(householdId: number, taskId: number, status: TaskStatus, position: number): Observable<Task> {
    return this.http.patch<Task>(`${this.tasksUrl(householdId)}/${taskId}/move`, { status, position });
  }

  delete(householdId: number, taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.tasksUrl(householdId)}/${taskId}`);
  }

  /** Löscht alle Aufgaben der DONE-Spalte. */
  deleteDone(householdId: number): Observable<void> {
    return this.http.delete<void>(this.tasksUrl(householdId), { params: { status: 'DONE' } });
  }

  private toPayload(form: TaskFormData): unknown {
    return {
      title: form.title,
      content: form.content,
      assigneeUserId: form.assigneeUserId,
      dueDate: form.dueDate,
      labels: form.labels,
    };
  }

  private tasksUrl(householdId: number): string {
    return `${API_CONFIG.basePath}/households/${householdId}/tasks`;
  }
}
