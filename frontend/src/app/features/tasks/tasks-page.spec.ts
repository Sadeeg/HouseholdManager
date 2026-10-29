import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { TasksPage } from './tasks-page';
import { TasksData } from './data/tasks-data';
import { Task } from './model/task.model';

function taskJson(overrides: Partial<Task> = {}): Task {
  return {
    id: 10,
    title: 'Aufgabe',
    content: null,
    status: 'TODO',
    position: 0,
    assigneeUserId: null,
    assigneeDisplayName: null,
    assigneeInHousehold: true,
    dueDate: null,
    labels: [],
    ...overrides,
  };
}

describe('TasksPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TasksPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        TasksData,
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);

    const workspace = TestBed.inject(WorkspaceService);
    workspace.availableHouseholds.set([
      { id: 1, name: 'Haus A' },
      { id: 2, name: 'Haus B' },
    ]);
    workspace.currentUser.set({ id: 'alice', displayName: 'Alice' });
    workspace.currentHouseholdId.set(1);
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function open(tasks: Task[]) {
    const fixture = TestBed.createComponent(TasksPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households/1/tasks').flush(tasks);
    await tick(fixture);
    return fixture;
  }

  /**
   * Zone-stabile Fortsetzung ohne whenStable: ein Makrotask reicht, damit
   * nach einem Flush die Fortsetzung (Reload-Request) läuft — whenStable
   * würde auf den noch nicht geflushzten Reload warten.
   */
  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  function cardsIn(compiled: HTMLElement, status: string): number {
    return compiled.querySelectorAll(`[data-status="${status}"] app-task-card`).length;
  }

  function clickWhere(compiled: HTMLElement, where: (el: Element) => boolean, within?: Element | null): void {
    const scope = within ?? compiled;
    const target = [...scope.querySelectorAll('button')].find((b) => where(b as Element));
    target?.click();
  }

  it('renders the three columns of the current household, empty columns included (AC1)', async () => {
    const fixture = await open([taskJson(), taskJson({ id: 11, status: 'DONE', title: 'Fertig' })]);
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelectorAll('.board__column').length).toBe(3);
    expect(compiled.querySelector('[data-status="TODO"]')?.textContent).toContain('Aufgabe');
    expect(cardsIn(compiled, 'DONE')).toBe(1);
    // Spaltenköpfe tragen die Anzahl als Text.
    expect(compiled.querySelector('[data-status="DOING"] .board__column-count')?.textContent?.trim()).toBe('0');
  });

  it('shows „Keine Aufgaben“ in empty columns (AC10)', async () => {
    const fixture = await open([taskJson()]);
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelectorAll('.board__empty').length).toBe(2);
    expect(compiled.querySelector('[data-status="DOING"]')?.textContent).toContain('Keine Aufgaben');
  });

  it('shows the board hint with create action when everything is empty (AC10)', async () => {
    const fixture = await open([]);
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.board__hint')?.textContent).toContain('Noch keine Aufgaben in diesem Haushalt.');
    expect(compiled.querySelector('.board__hint')?.textContent).toContain('Aufgabe anlegen');
  });

  it('moves a card via „Verschieben nach …“ and persists the move (AC4, keyboard path)', async () => {
    const fixture = await open([taskJson()]);
    const compiled = fixture.nativeElement as HTMLElement;

    clickWhere(compiled, (el) => (el.textContent ?? '').includes('VERSCHIEBEN NACH'));
    fixture.detectChanges();
    clickWhere(compiled, (el) => (el.textContent ?? '').includes('Nach DONE'),
      compiled.querySelector('.task-card__move-menu'));

    const moveRequest = httpMock.expectOne('/api/households/1/tasks/10/move', 'PATCH');
    expect(moveRequest.request.body).toEqual({ status: 'DONE', position: 0 });
    moveRequest.flush(taskJson({ status: 'DONE', position: 0 }));
    await tick(fixture);

    httpMock.expectOne('/api/households/1/tasks').flush([taskJson({ status: 'DONE', position: 0 })]);
    await tick(fixture);

    expect(cardsIn(fixture.nativeElement as HTMLElement, 'DONE')).toBe(1);
    expect(cardsIn(fixture.nativeElement as HTMLElement, 'TODO')).toBe(0);
    expect(compiled.querySelector('.board__announce')?.textContent).toContain('Aufgabe nach DONE verschoben.');
  });

  it('keeps a dropped card in the target column while the move is stored (AC4)', async () => {
    const fixture = await open([taskJson(), taskJson({ id: 11, status: 'DOING', title: 'Andere' })]);
    const page = fixture.componentInstance as TasksPage;
    const compiled = fixture.nativeElement as HTMLElement;

    (page as unknown as { onDrop: (status: string, event: unknown) => void }).onDrop('DONE', {
      previousContainer: { data: [taskJson()] },
      previousIndex: 0,
      container: { data: [] },
      currentIndex: 0,
    });
    fixture.detectChanges();

    // Kein Zurückfallen in die alte Bahn: die Karte steht in DONE, bevor die
    // Antwort des Servers eintrifft.
    expect(cardsIn(compiled, 'DONE')).toBe(1);
    expect(cardsIn(compiled, 'TODO')).toBe(0);

    const moveRequest = httpMock.expectOne('/api/households/1/tasks/10/move', 'PATCH');
    expect(moveRequest.request.body).toEqual({ status: 'DONE', position: 0 });
    moveRequest.flush(taskJson({ status: 'DONE', position: 0 }));
    await tick(fixture);

    httpMock.expectOne('/api/households/1/tasks').flush([
      taskJson({ id: 11, status: 'DOING', title: 'Andere' }),
      taskJson({ status: 'DONE', position: 0 }),
    ]);
    await tick(fixture);

    expect(cardsIn(compiled, 'DONE')).toBe(1);
    expect(cardsIn(compiled, 'DOING')).toBe(1);
    expect(cardsIn(compiled, 'TODO')).toBe(0);
  });

  it('reorders within a column on drop and persists the new position (AC24)', async () => {
    const fixture = await open([
      taskJson({ id: 10, position: 0 }),
      taskJson({ id: 11, position: 1, title: 'Zweite' }),
    ]);
    const page = fixture.componentInstance as TasksPage;
    const data = [taskJson({ id: 10, position: 0 }), taskJson({ id: 11, position: 1, title: 'Zweite' })];

    (page as unknown as { onDrop: (status: string, event: unknown) => void }).onDrop('TODO', {
      previousContainer: { data },
      previousIndex: 0,
      container: { data },
      currentIndex: 1,
    });

    // Zwischenstand: die neue Reihenfolge ist sofort sichtbar, nicht erst nach dem Neuladen.
    const compiled = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    const cards = compiled.querySelectorAll('[data-status="TODO"] app-task-card');
    expect(cards[0].textContent).toContain('Zweite');

    const moveRequest = httpMock.expectOne('/api/households/1/tasks/10/move', 'PATCH');
    expect(moveRequest.request.body).toEqual({ status: 'TODO', position: 1 });
    moveRequest.flush(taskJson({ position: 1 }));
    await tick(fixture);
    httpMock.expectOne('/api/households/1/tasks').flush([data[1], taskJson({ position: 1 })]);
    await tick(fixture);
  });

  it('keeps the card in its column when the move cannot be stored (AC19)', async () => {
    const fixture = await open([taskJson()]);
    const compiled = fixture.nativeElement as HTMLElement;

    clickWhere(compiled, (el) => (el.textContent ?? '').includes('VERSCHIEBEN NACH'));
    fixture.detectChanges();
    clickWhere(compiled, (el) => (el.textContent ?? '').includes('Nach DOING'),
      compiled.querySelector('.task-card__move-menu'));

    httpMock.expectOne('/api/households/1/tasks/10/move', 'PATCH').flush(
      null,
      { status: 500, statusText: 'Server Error' },
    );
    await tick(fixture);

    expect(cardsIn(compiled, 'TODO')).toBe(1);
    expect(cardsIn(compiled, 'DOING')).toBe(0);
    expect(compiled.querySelector('.board__announce')?.textContent).toContain(
      'Die Aufgabe konnte nicht verschoben werden.',
    );
  });

  it('asks before deleting and deletes only after confirmation (AC21)', async () => {
    const fixture = await open([taskJson()]);
    const compiled = fixture.nativeElement as HTMLElement;

    clickWhere(compiled, (el) => (el.textContent ?? '').includes('LÖSCHEN'));
    fixture.detectChanges();
    expect(compiled.textContent).toContain('Diese Aufgabe wird endgültig gelöscht.');

    // Abbrechen: es geht kein DELETE raus, die Karte bleibt.
    clickWhere(compiled, (el) => (el.textContent ?? '').includes('Abbrechen'),
      compiled.querySelector('[role="alertdialog"]'));
    fixture.detectChanges();
    expect(cardsIn(compiled, 'TODO')).toBe(1);

    clickWhere(compiled, (el) => (el.textContent ?? '').includes('LÖSCHEN'));
    fixture.detectChanges();
    clickWhere(compiled, (el) => (el.textContent ?? '').includes('Endgültig löschen'),
      compiled.querySelector('[role="alertdialog"]'));

    httpMock.expectOne('/api/households/1/tasks/10', 'DELETE').flush({});
    await tick(fixture);
    httpMock.expectOne('/api/households/1/tasks').flush([]);
    await tick(fixture);

    expect(cardsIn(compiled, 'TODO')).toBe(0);
  });

  it('offers „Erledigte löschen“ only when DONE has tasks (AC15)', async () => {
    const fixture = await open([taskJson()]);
    expect(
      [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')]
        .some((b) => (b.textContent ?? '').includes('Erledigte löschen')),
    ).toBe(false);

    TestBed.inject(TasksData).tasks.reload();
    await tick(fixture);
    httpMock.expectOne('/api/households/1/tasks').flush([
      taskJson(),
      taskJson({ id: 11, status: 'DONE', title: 'Fertig 1' }),
    ]);
    await tick(fixture);

    expect(
      [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')]
        .some((b) => (b.textContent ?? '').includes('Erledigte löschen')),
    ).toBe(true);
  });

  it('deletes only DONE tasks after confirmation and reports the count (AC15)', async () => {
    const fixture = await open([
      taskJson(),
      taskJson({ id: 11, status: 'DONE', title: 'Fertig 1' }),
      taskJson({ id: 12, status: 'DONE', title: 'Fertig 2', position: 1 }),
    ]);
    const compiled = fixture.nativeElement as HTMLElement;

    clickWhere(compiled, (el) => (el.textContent ?? '').includes('Erledigte löschen'),
      compiled.querySelector('.page-header__actions'));
    fixture.detectChanges();

    // Die Bestätigung nennt Zahl und Objekt.
    expect(compiled.querySelector('[role="alertdialog"]')?.textContent).toContain(
      '2 Aufgaben in „DONE“ werden endgültig gelöscht.',
    );

    clickWhere(compiled, (el) => (el.textContent ?? '').includes('Endgültig löschen'),
      compiled.querySelector('[role="alertdialog"]'));
    httpMock.expectOne('/api/households/1/tasks?status=DONE', 'DELETE').flush({});
    await tick(fixture);
    httpMock.expectOne('/api/households/1/tasks').flush([taskJson()]);
    await tick(fixture);

    expect(cardsIn(compiled, 'DONE')).toBe(0);
    expect(compiled.querySelector('.toast')?.textContent).toContain('2 Aufgaben gelöscht.');
  });

  it('reloads the board when the workspace changes — without a page reload (AC23)', async () => {
    const fixture = await open([taskJson()]);

    TestBed.inject(WorkspaceService).currentHouseholdId.set(2);
    fixture.detectChanges();

    httpMock.expectOne('/api/households/2/tasks').flush([
      taskJson({ id: 20, title: 'Anderes Haus' }),
    ]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(cardsIn(compiled, 'TODO')).toBe(1);
    expect(compiled.textContent).toContain('Anderes Haus');
  });

  it('shows the overdue badge in the board context (AC22)', async () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const iso = yesterday.toISOString().slice(0, 10);

    const fixture = await open([
      taskJson({ dueDate: iso }),
      taskJson({ id: 11, status: 'DONE', dueDate: iso, title: 'Erledigt' }),
    ]);
    const compiled = fixture.nativeElement as HTMLElement;

    const todoCard = compiled.querySelector('[data-status="TODO"] .task-card');
    const doneCard = compiled.querySelector('[data-status="DONE"] .task-card');
    expect(todoCard?.textContent).toContain('ÜBERFÄLLIG');
    expect(doneCard?.textContent).not.toContain('ÜBERFÄLLIG');
  });
});
