import { TestBed } from '@angular/core/testing';

import { Task, TaskFormData } from '../model/task.model';
import { TaskForm } from './task-form';

const INITIAL_TASK: Task = {
  id: 7,
  title: 'Alte Aufgabe',
  content: 'alter **Inhalt**',
  status: 'DOING',
  position: 0,
  assigneeUserId: 'bob',
  assigneeDisplayName: 'Bob',
  assigneeInHousehold: true,
  dueDate: '2030-05-04',
  labels: ['Bad'],
};

describe('TaskForm', () => {
  async function create(
    inputs: { initial?: Task | null; labelSuggestions?: string[] } = {},
  ) {
    TestBed.configureTestingModule({ imports: [TaskForm] });
    const fixture = TestBed.createComponent(TaskForm);
    fixture.componentRef.setInput('initial', inputs.initial ?? null);
    fixture.componentRef.setInput('labelSuggestions', inputs.labelSuggestions ?? []);
    const saved: TaskFormData[] = [];
    fixture.componentInstance.saved.subscribe((form) => saved.push(form));
    fixture.detectChanges();
    await fixture.whenStable();
    return { fixture, saved };
  }

  function fill(el: HTMLElement, selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement | HTMLTextAreaElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('change'));
    input.dispatchEvent(new Event('blur'));
  }

  function clickButton(el: HTMLElement, label: string): void {
    const button = [...el.querySelectorAll('button')].find((candidate) =>
      candidate.textContent?.includes(label),
    );
    button?.click();
  }

  it('rejects an empty title with an inline error and keeps the entered content (AC18)', async () => {
    const { fixture, saved } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-content', 'Nur Inhalt, kein Titel');
    clickButton(element, 'Speichern');
    fixture.detectChanges();

    expect(element.textContent).toContain('Der Titel fehlt.');
    expect(saved.length).toBe(0);
    // Der Dialog bleibt offen und der Inhalt ist noch da.
    expect((element.querySelector('#task-content') as HTMLTextAreaElement).value)
      .toBe('Nur Inhalt, kein Titel');
  });

  it('creates a task with title and verbatim markdown (AC2)', async () => {
    const { fixture, saved } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-title', 'Müll raus');
    fill(element, '#task-content', '# Tonnen bereitstellen\n- blau **zuerst**');
    clickButton(element, 'Speichern');
    fixture.detectChanges();

    expect(saved.length).toBe(1);
    expect(saved[0].title).toBe('Müll raus');
    expect(saved[0].content).toBe('# Tonnen bereitstellen\n- blau **zuerst**');
    expect(saved[0].assigneeUserId).toBeNull();
    expect(saved[0].dueDate).toBeNull();
    expect(saved[0].labels).toEqual([]);
  });

  it('renders the markdown preview of the same content (AC3)', async () => {
    const { fixture } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-content', '# Überschrift\n- Punkt');
    clickButton(element, 'Vorschau');
    fixture.detectChanges();

    expect(element.querySelector('app-markdown-view h3')?.textContent).toContain('Überschrift');
    expect(element.querySelector('app-markdown-view li')?.textContent).toContain('Punkt');
  });

  it('prefills every field for editing and saves the changes (AC5)', async () => {
    const { fixture, saved } = await create({ initial: INITIAL_TASK });
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Aufgabe bearbeiten');
    expect((element.querySelector('#task-title') as HTMLInputElement).value).toBe('Alte Aufgabe');
    expect((element.querySelector('#task-content') as HTMLTextAreaElement).value).toBe('alter **Inhalt**');
    expect((element.querySelector('#task-due') as HTMLInputElement).value).toBe('04.05.2030');
    expect(element.textContent).toContain('Bad'); // Label-Chip vorbelegt
    expect(element.textContent).toContain('@bob'); // Zuständige vorbelegt

    fill(element, '#task-title', 'Neuer Titel');
    clickButton(element, 'Änderungen speichern');
    fixture.detectChanges();

    expect(saved.length).toBe(1);
    expect(saved[0].title).toBe('Neuer Titel');
    expect(saved[0].content).toBe('alter **Inhalt**');
    expect(saved[0].labels).toEqual(['Bad']);
    expect(saved[0].assigneeUserId).toBe('bob');
    expect(saved[0].dueDate).toBe('2030-05-04');
  });

  it('merges label spellings case-insensitively — „bad“ joins „Bad“, no second chip (AC20)', async () => {
    const { fixture, saved } = await create({ labelSuggestions: ['Bad'] });
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-title', 'X');
    fill(element, '#task-label', 'bad');
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    const chips = element.querySelectorAll('.task-form__labels .hm-chip');
    expect(chips.length).toBe(1);
    expect(chips[0].textContent).toContain('Bad');

    clickButton(element, 'Speichern');
    fixture.detectChanges();
    expect(saved[0].labels).toEqual(['Bad']);
  });

  it('rejects an invalid due date and hints at overdue dates', async () => {
    const { fixture, saved } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-title', 'X');
    fill(element, '#task-due', '31.02.2030');
    clickButton(element, 'Speichern');
    fixture.detectChanges();

    expect(element.textContent).toContain('Das Datum ist ungültig. Bitte im Format TT.MM.JJJJ eingeben.');
    expect(saved.length).toBe(0);

    fill(element, '#task-due', '01.01.2000');
    fixture.detectChanges();
    expect(element.textContent).toContain('Die Aufgabe wird als überfällig gekennzeichnet.');
  });
});
