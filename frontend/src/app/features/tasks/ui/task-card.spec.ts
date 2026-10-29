import { TestBed } from '@angular/core/testing';

import { Task, TaskStatus, labelColorIndex } from '../model/task.model';
import { TaskCard } from './task-card';

function makeTask(overrides: Partial<Task> = {}): Task {
  return {
    id: 10,
    title: 'Müll raus',
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

function isoOffset(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

describe('TaskCard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [TaskCard] });
  });

  function render(task: Task) {
    const fixture = TestBed.createComponent(TaskCard);
    fixture.componentRef.setInput('task', task);
    fixture.detectChanges();
    return fixture;
  }

  it('shows title, due date and assignee as text', () => {
    const fixture = render(makeTask({
      dueDate: isoOffset(30),
      assigneeUserId: 'bob',
      assigneeDisplayName: 'Bob Mitglied',
    }));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.task-card__title')?.textContent).toContain('Müll raus');
    expect(compiled.textContent).toContain('FRIST');
    expect(compiled.textContent).toContain('ZUSTÄNDIG');
    expect(compiled.textContent).toContain('Bob Mitglied');
    expect(compiled.textContent).toContain('@bob');
  });

  it('marks overdue tasks with the text badge ÜBERFÄLLIG (AC22)', () => {
    const fixture = render(makeTask({ dueDate: isoOffset(-1) }));
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ÜBERFÄLLIG');
  });

  it('never marks DONE or future dates as overdue (AC22)', () => {
    const done = render(makeTask({ dueDate: isoOffset(-1), status: 'DONE' }));
    expect((done.nativeElement as HTMLElement).textContent).not.toContain('ÜBERFÄLLIG');

    const future = render(makeTask({ dueDate: isoOffset(1) }));
    expect((future.nativeElement as HTMLElement).textContent).not.toContain('ÜBERFÄLLIG');
  });

  it('never marks a task whose due date was removed as overdue (AC22)', () => {
    // Genau die Antwort, die der Server nach dem Entfernen der Frist liefert.
    const removed = render(makeTask({ dueDate: null }));
    expect((removed.nativeElement as HTMLElement).textContent).not.toContain('ÜBERFÄLLIG');
  });

  it('shows NICHT IM HAUSHALT for assignees who are no longer members (AC14)', () => {
    const fixture = render(makeTask({
      assigneeUserId: 'carl',
      assigneeDisplayName: 'Carl Ex',
      assigneeInHousehold: false,
    }));
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('NICHT IM HAUSHALT');
    // Fällt der Anzeigename weg, bleibt zumindest die Kennung lesbar.
    expect(compiled.textContent).toContain('@carl');
  });

  it('gives the same color index to the same normalized label name (AC17/AC20)', () => {
    expect(labelColorIndex('Bad')).toBe(labelColorIndex('bad'));
    expect(labelColorIndex('  BAD  ')).toBe(labelColorIndex('Bad'));

    const fixture = render(makeTask({ labels: ['Bad'] }));
    const chip = (fixture.nativeElement as HTMLElement).querySelector('.task-card__label') as HTMLElement;
    const index = labelColorIndex('Bad');
    expect(chip.style.getPropertyValue('--label-color')).toBe(`var(--hm-label-${index})`);
    // Indizes bleiben im Bereich der Tokens --hm-label-1 … --hm-label-8.
    expect(index).toBeGreaterThanOrEqual(1);
    expect(index).toBeLessThanOrEqual(8);
  });

  it('offers „Verschieben nach …“ with the current column disabled (barrier-free move)', () => {
    const fixture = TestBed.createComponent(TaskCard);
    fixture.componentRef.setInput('task', makeTask({ status: 'DOING' }));
    const moved: TaskStatus[] = [];
    fixture.componentInstance.moved.subscribe((status) => moved.push(status));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const trigger = [...compiled.querySelectorAll('button')].find((button) =>
      button.textContent?.includes('VERSCHIEBEN NACH'),
    );
    trigger?.click();
    fixture.detectChanges();

    const items = [...compiled.querySelectorAll('[role="menuitem"]')].map((item) =>
      item.textContent?.trim(),
    );
    expect(items).toEqual(['Nach TODO', 'Nach DONE']);

    (compiled.querySelectorAll('[role="menuitem"]')[1] as HTMLButtonElement).click();
    expect(moved).toEqual(['DONE']);
  });

  it('emits edit and delete requests from the card actions', () => {
    const fixture = render(makeTask());
    const editRequests: Task[] = [];
    const deleteRequests: Task[] = [];
    fixture.componentInstance.editRequested.subscribe((task) => editRequests.push(task));
    fixture.componentInstance.deleteRequested.subscribe((task) => deleteRequests.push(task));

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')].find((button) => button.textContent?.includes('BEARBEITEN'))?.click();
    [...compiled.querySelectorAll('button')].find((button) => button.textContent?.includes('LÖSCHEN'))?.click();

    expect(editRequests.length).toBe(1);
    expect(deleteRequests.length).toBe(1);
  });
});
