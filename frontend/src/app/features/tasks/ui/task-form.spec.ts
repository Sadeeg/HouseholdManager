import { TestBed } from '@angular/core/testing';

import { MAX_LABEL_NAME_LENGTH, Task, TaskFormData } from '../model/task.model';
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

  it('rejects a label beyond the column limit at the label input and keeps the input (AC1, AC3)', async () => {
    const { fixture } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-title', 'Müll raus');
    fill(element, '#task-label', 'x'.repeat(MAX_LABEL_NAME_LENGTH + 1));
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    const alert = element.querySelector('#task-label-error');
    expect(alert?.textContent).toContain(`höchstens ${MAX_LABEL_NAME_LENGTH}`);
    // Das Label wurde nicht übernommen, die Eingabe steht noch.
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(0);
    expect((element.querySelector('#task-label') as HTMLInputElement).value)
      .toHaveLength(MAX_LABEL_NAME_LENGTH + 1);
    // Die übrige Eingabe bleibt erhalten.
    expect((element.querySelector('#task-title') as HTMLInputElement).value).toBe('Müll raus');
  });

  it('accepts a label at the column limit (AC2)', async () => {
    const { fixture, saved } = await create();
    const element = fixture.nativeElement as HTMLElement;
    const atLimit = 'x'.repeat(MAX_LABEL_NAME_LENGTH);

    fill(element, '#task-title', 'X');
    fill(element, '#task-label', atLimit);
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')).toBeNull();
    const chips = element.querySelectorAll('.task-form__labels .hm-chip');
    expect(chips.length).toBe(1);
    expect(chips[0].textContent).toContain(atLimit);

    clickButton(element, 'Speichern');
    fixture.detectChanges();
    expect(saved[0].labels).toEqual([atLimit]);
  });

  it('drops the label error as soon as the name is corrected (AC4)', async () => {
    const { fixture } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-label', 'x'.repeat(MAX_LABEL_NAME_LENGTH + 1));
    fixture.detectChanges();
    expect(element.querySelector('#task-label-error')).not.toBeNull();

    fill(element, '#task-label', 'Kurz');
    fixture.detectChanges();
    expect(element.querySelector('#task-label-error')).toBeNull();

    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(1);
  });

  it('accepts a label just below the column limit (AC2)', async () => {
    const { fixture, saved } = await create();
    const element = fixture.nativeElement as HTMLElement;
    const justBelow = 'x'.repeat(MAX_LABEL_NAME_LENGTH - 1);

    fill(element, '#task-title', 'X');
    fill(element, '#task-label', justBelow);
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')).toBeNull();
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(1);

    clickButton(element, 'Speichern');
    fixture.detectChanges();
    expect(saved[0].labels).toEqual([justBelow]);
  });

  it('skips an empty label silently, without an error message (AC3)', async () => {
    const { fixture, saved } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-title', 'X');
    fill(element, '#task-label', '   ');
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    // Kein Fehler an der Eingabe — leere Labels bleiben still übersprungen.
    expect(element.querySelector('#task-label-error')).toBeNull();
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(0);

    clickButton(element, 'Speichern');
    fixture.detectChanges();
    expect(saved[0].labels).toEqual([]);
  });

  it('keeps the title and the label input while the error is shown and after the correction (AC4)', async () => {
    const { fixture } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-title', 'Müll raus');
    fill(element, '#task-label', 'x'.repeat(MAX_LABEL_NAME_LENGTH + 1));
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')).not.toBeNull();
    expect((element.querySelector('#task-title') as HTMLInputElement).value).toBe('Müll raus');

    fill(element, '#task-label', 'Kurz');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')).toBeNull();
    // Der Titel ist unberührt, das Formular bleibt bedienbar.
    expect((element.querySelector('#task-title') as HTMLInputElement).value).toBe('Müll raus');
    expect((element.querySelector('#task-label') as HTMLInputElement).value).toBe('Kurz');
  });

  /**
   * Die Grenze des Formulars muss die Grenze der Spalte `label.name`
   * treffen — 60 Zeichen passen in die Spalte, 61 nicht. Die Zahl steht hier
   * bewusst als Literal und nicht als Konstante: so fällt auf, wenn die
   * Dublette im Frontend auseinanderläuft und das Formular einen Namen
   * durchlässt, den das Backend ablehnt.
   */
  it('enforces the limit of the label column — 60 pass, 61 do not (AC1, AC2)', async () => {
    const { fixture } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-label', 'x'.repeat(60));
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')).toBeNull();
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(1);

    fill(element, '#task-label', 'x'.repeat(61));
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')?.textContent).toContain('höchstens 60');
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(1);
  });

  /**
   * Gemessen wird nach dem Trimmen: Leerzeichen drumherum zählen nicht,
   * weil sie nicht gespeichert werden. Sonst wäre das Formular strenger als
   * der Fachkern — oder umgekehrt.
   */
  it('measures the label length after trimming (AC1, AC2)', async () => {
    const { fixture } = await create();
    const element = fixture.nativeElement as HTMLElement;

    fill(element, '#task-label', `  ${'x'.repeat(60)}  `);
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')).toBeNull();
    const chips = element.querySelectorAll('.task-form__labels .hm-chip');
    expect(chips.length).toBe(1);
    expect(chips[0].textContent).toContain('x'.repeat(60));

    fill(element, '#task-label', `  ${'x'.repeat(61)}  `);
    clickButton(element, 'Hinzufügen');
    fixture.detectChanges();

    expect(element.querySelector('#task-label-error')?.textContent).toContain('höchstens 60');
    expect(element.querySelectorAll('.task-form__labels .hm-chip').length).toBe(1);
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
