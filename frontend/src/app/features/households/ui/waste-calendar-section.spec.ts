import { TestBed } from '@angular/core/testing';

import { WASTE_URL_MAX_LENGTH } from '../model/household.model';
import { WasteCalendarSection } from './waste-calendar-section';

/**
 * Der Abschnitt „Müllkalender" — alle Fälle an einer Stelle (Issue #15).
 *
 * <p>AC9 (Issue #10): jedes Mitglied darf die ICS-Adresse pflegen. Das Component
 * redet nicht mit dem Backend — es zeigt die gespeicherte Adresse, meldet jede
 * Änderung, zeigt die Zeichenlänge (AC22), stellt die gespeicherte Adresse bei
 * „Abbrechen" wieder her (R19; der Beleg, dass dabei kein Schreibpfad läuft —
 * AC23 — ist auf Seitenebene in `households-page-abort.spec.ts` geführt) und
 * erlaubt das Entfernen nur, wenn eine Adresse gespeichert ist.
 *
 * <p>AC22 (R18): die Grenze der ICS-Adresse — die Oberfläche nennt sie,
 * speichert eine zu lange Adresse nicht und kappt sie nicht stillschweigend;
 * an der Grenze zählt der **getrimmte** Text, weil der getrimmte Text
 * gespeichert wird.
 *
 * <p>Die Fälle waren über mehrere Dateien verteilt (je eine eigene
 * TestBed-Einrichtung, je ein eigener `tick()`-Helfer). Jetzt gibt es eine
 * Datei und eine Einrichtung — jeder Fall behält seinen Test und seinen
 * Namen.
 */
describe('WasteCalendarSection', () => {
  function open(storedUrl: string | null, saving = false) {
    const fixture = TestBed.createComponent(WasteCalendarSection);
    fixture.componentRef.setInput('householdId', 1);
    fixture.componentRef.setInput('householdName', 'Haus A');
    fixture.componentRef.setInput('storedUrl', storedUrl);
    fixture.componentRef.setInput('saving', saving);
    fixture.detectChanges();
    return fixture;
  }

  function input(fixture: ReturnType<typeof TestBed.createComponent>): HTMLInputElement {
    return (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
  }

  function typeInput(
    fixture: ReturnType<typeof TestBed.createComponent>,
    value: string,
  ): HTMLInputElement {
    const field = input(fixture);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return field;
  }

  function saveButton(fixture: ReturnType<typeof TestBed.createComponent>): HTMLButtonElement {
    return (fixture.nativeElement as HTMLElement).querySelector(
      '.hm-button--primary',
    ) as HTMLButtonElement;
  }

  function removeButton(fixture: ReturnType<typeof TestBed.createComponent>): HTMLButtonElement {
    return (fixture.nativeElement as HTMLElement).querySelector(
      '.waste-calendar__remove',
    ) as HTMLButtonElement;
  }

  /** Die Fehlermeldung erscheint erst, wenn das Feld berührt ist. */
  function blurInput(field: HTMLInputElement): void {
    field.dispatchEvent(new Event('blur'));
  }

  function compiled(fixture: ReturnType<typeof TestBed.createComponent>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  // --- Anzeige und Pflege (AC9, AC22, R19) ---------------------------------

  it('prefills the field with the stored address', () => {
    const fixture = open('https://entsorger.example/kalender.ics');

    expect(input(fixture).value).toBe('https://entsorger.example/kalender.ics');
    expect(compiled(fixture).textContent).toContain('38 / 500');
  });

  /** AC22: die Grenze wird genannt und der Zähler wechselt die Farbe. */
  it('counts characters and names the limit', () => {
    const fixture = open('https://entsorger.example/kalender.ics');
    const counter = compiled(fixture).querySelector('.waste-calendar__counter') as HTMLElement;

    typeInput(fixture, 'x'.repeat(128));
    expect(counter.textContent).toContain('128 / 500');
    expect(counter.classList.contains('waste-calendar__counter--warn')).toBe(false);

    typeInput(fixture, 'x'.repeat(460));
    expect(counter.textContent).toContain('460 / 500');
    expect(counter.classList.contains('waste-calendar__counter--warn')).toBe(true);

    blurInput(typeInput(fixture, 'x'.repeat(510)));
    fixture.detectChanges();
    expect(counter.textContent).toContain('510 / 500');
    expect(counter.classList.contains('waste-calendar__counter--danger')).toBe(true);
    expect(compiled(fixture).textContent).toContain('höchstens 500 Zeichen');
  });

  it('reports a change, not the unchanged address', () => {
    const fixture = open('https://entsorger.example/kalender.ics');
    const saved: string[] = [];
    fixture.componentRef.instance.saveRequested.subscribe((url) => saved.push(url));

    const save = saveButton(fixture);
    expect(save.disabled).toBe(true);

    typeInput(fixture, 'https://entsorger.example/anderer.ics');
    expect(save.disabled).toBe(false);
    save.click();

    expect(saved).toEqual(['https://entsorger.example/anderer.ics']);
  });

  /** R19: Abbrechen stellt die gespeicherte Adresse wieder her. */
  it('restores the stored address when editing is aborted', () => {
    const fixture = open('https://entsorger.example/kalender.ics');

    typeInput(fixture, 'https://entsorger.example/anderer.ics');
    const cancelButton = [...compiled(fixture).querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === 'Abbrechen',
    ) as HTMLButtonElement;
    cancelButton.click();
    fixture.detectChanges();

    expect(input(fixture).value).toBe('https://entsorger.example/kalender.ics');
  });

  /** Ohne gespeicherte Adresse gibt es nichts zu entfernen. */
  it('offers removal only for a stored address', () => {
    expect(removeButton(open(null)).disabled).toBe(true);
    expect(removeButton(open('https://entsorger.example/kalender.ics')).disabled).toBe(false);
  });

  // --- Grenze der ICS-Adresse (AC22, R18) -----------------------------------

  it('does not offer to save an address over the limit', () => {
    const fixture = open(null);

    typeInput(fixture, 'x'.repeat(WASTE_URL_MAX_LENGTH + 1));

    expect(saveButton(fixture).disabled).toBe(true);
  });

  it('reports no address to the caller when the limit is exceeded', () => {
    const fixture = open(null);
    const saved: string[] = [];
    fixture.componentRef.instance.saveRequested.subscribe((url) => saved.push(url));

    typeInput(fixture, 'x'.repeat(WASTE_URL_MAX_LENGTH + 1));

    // Auch ein erzwungener Klick auf den (deaktivierten) Knopf darf nichts melden.
    saveButton(fixture).click();
    fixture.detectChanges();

    expect(saved).toEqual([]);
  });

  it('names the permitted length at the field', () => {
    const fixture = open(null);

    typeInput(fixture, 'x'.repeat(WASTE_URL_MAX_LENGTH + 1));
    blurInput(input(fixture));
    fixture.detectChanges();

    const element = compiled(fixture);
    expect(element.textContent).toContain(`höchstens ${WASTE_URL_MAX_LENGTH} Zeichen`);
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      `höchstens ${WASTE_URL_MAX_LENGTH} Zeichen`,
    );
    expect(element.querySelector('.waste-calendar__counter')?.textContent).toContain(
      `${WASTE_URL_MAX_LENGTH + 1} / ${WASTE_URL_MAX_LENGTH}`,
    );
  });

  /** Genau an der Grenze ist die Adresse noch speicherbar. */
  it('still offers to save an address of exactly the permitted length', () => {
    const fixture = open(null);

    typeInput(fixture, 'x'.repeat(WASTE_URL_MAX_LENGTH));

    expect(saveButton(fixture).disabled).toBe(false);
  });

  /**
   * Die zweite Hälfte der Aussage: die Oberfläche kappt eine zu lange Adresse
   * nicht stillschweigend. `maxLength()` von Angular Signal Forms spiegelt sich
   * als natives `maxlength`-Attribut in das Feld — der Browser würde eine zu
   * lange Eingabe beim Pasten abschneiden: der Zähler zeigte eine andere
   * Länge als die, die gespeichert würde.
   */
  it('renders the field without a native maxlength attribute', () => {
    const fixture = open(null);

    expect(input(fixture).hasAttribute('maxlength')).toBe(false);
    expect(input(fixture).getAttribute('maxlength')).toBeNull();
  });

  it('keeps the full length of an address over the limit in the field', () => {
    const fixture = open(null);
    const tooLong = 'https://entsorger.example/' + 'x'.repeat(WASTE_URL_MAX_LENGTH);

    typeInput(fixture, tooLong);

    expect(input(fixture).value).toHaveLength(tooLong.length);
    expect(input(fixture).value.length).toBeGreaterThan(WASTE_URL_MAX_LENGTH);
  });

  it('names the limit, counts in danger and refuses to save', () => {
    const fixture = open(null);
    const tooLong = 'https://entsorger.example/' + 'x'.repeat(WASTE_URL_MAX_LENGTH);

    typeInput(fixture, tooLong);
    blurInput(input(fixture));
    fixture.detectChanges();

    const element = compiled(fixture);
    const counter = element.querySelector('.waste-calendar__counter') as HTMLElement;

    expect(counter.textContent?.trim()).toBe(`${tooLong.length} / ${WASTE_URL_MAX_LENGTH}`);
    expect(counter.classList.contains('waste-calendar__counter--danger')).toBe(true);
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      `Die ICS-Adresse darf höchstens ${WASTE_URL_MAX_LENGTH} Zeichen lang sein.`,
    );
    expect(saveButton(fixture).disabled).toBe(true);
  });

  // --- Die Grenze zählt den getrimmten Text (AC22 an der Grenze) ------------

  const PREFIX = 'https://a.example/';

  /** 500 Zeichen nach dem Trimmen — das Leerzeichen am Ende zählt nicht. */
  const anAddressOfThePermittedLength = PREFIX + 'x'.repeat(WASTE_URL_MAX_LENGTH - PREFIX.length) + ' ';

  it('offers an address that only exceeds the limit in the raw text', () => {
    const fixture = open(null);

    typeInput(fixture, anAddressOfThePermittedLength);

    expect(input(fixture).value).toHaveLength(WASTE_URL_MAX_LENGTH + 1);
    expect(saveButton(fixture).disabled).toBe(false);
  });

  it('counts the trimmed text and shows no field error', () => {
    const fixture = open(null);

    typeInput(fixture, anAddressOfThePermittedLength);
    blurInput(input(fixture));
    fixture.detectChanges();

    const counter = compiled(fixture).querySelector('.waste-calendar__counter') as HTMLElement;

    expect(counter.textContent?.trim()).toBe(`${WASTE_URL_MAX_LENGTH} / ${WASTE_URL_MAX_LENGTH}`);
    expect(counter.classList.contains('waste-calendar__counter--danger')).toBe(false);
    expect(compiled(fixture).querySelector('.waste-calendar__error')).toBeNull();
  });

  /** Gespeichert wird der getrimmte Text — ohne das Leerzeichen am Ende. */
  it('reports the trimmed address to the caller', () => {
    const fixture = open(null);
    const saved: string[] = [];
    fixture.componentRef.instance.saveRequested.subscribe((url) => saved.push(url));

    typeInput(fixture, anAddressOfThePermittedLength);
    saveButton(fixture).click();

    expect(saved).toHaveLength(1);
    expect(saved[0]).toHaveLength(WASTE_URL_MAX_LENGTH);
    expect(saved[0].endsWith(' ')).toBe(false);
  });

  /** Ein Zeichen zu viel nach dem Trimmen bleibt gesperrt. */
  it('refuses an address that exceeds the limit after trimming', () => {
    const fixture = open(null);

    typeInput(fixture, PREFIX + 'x'.repeat(WASTE_URL_MAX_LENGTH - PREFIX.length + 1));

    expect(saveButton(fixture).disabled).toBe(true);
  });

  // --- Zustand „Speichern läuft" (Zustandstabelle der ## UX) ----------------

  it('locks the field and all actions while the address is being saved', () => {
    const fixture = open('https://entsorger.example/kalender.ics', true);

    expect(input(fixture).disabled).toBe(true);

    const buttons = [...compiled(fixture).querySelectorAll('button')] as HTMLButtonElement[];
    expect(buttons.length).toBeGreaterThan(0);
    expect(buttons.every((button) => button.disabled)).toBe(true);
  });

  it('names the running action instead of the plain label', () => {
    const fixture = open('https://entsorger.example/kalender.ics', true);

    expect(compiled(fixture).textContent).toContain('Wird gespeichert …');
  });

  it('unlocks the field again when the save is done', () => {
    const fixture = open('https://entsorger.example/kalender.ics', false);

    expect(input(fixture).disabled).toBe(false);
    expect(compiled(fixture).textContent).toContain('Speichern');
    expect(compiled(fixture).textContent).not.toContain('Wird gespeichert …');
  });
});
