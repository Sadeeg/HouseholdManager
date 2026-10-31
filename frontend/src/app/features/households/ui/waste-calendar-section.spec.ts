import { TestBed } from '@angular/core/testing';

import { WasteCalendarSection } from './waste-calendar-section';

/**
 * AC23: jedes Mitglied darf die ICS-Adresse pflegen. Das Component redet nicht
 * mit dem Backend — es zeigt die gespeicherte Adresse, meldet jede Änderung,
 * zeigt die Zeichenlänge (AC18), stellt die gespeicherte Adresse bei
 * „Abbrechen" wieder her (R19) und erlaubt das Entfernen nur, wenn eine
 * Adresse gespeichert ist.
 */
describe('WasteCalendarSection', () => {
  function open(storedUrl: string | null) {
    const fixture = TestBed.createComponent(WasteCalendarSection);
    fixture.componentRef.setInput('householdId', 1);
    fixture.componentRef.setInput('householdName', 'Haus A');
    fixture.componentRef.setInput('storedUrl', storedUrl);
    fixture.detectChanges();
    return fixture;
  }

  function typeInput(fixture: ReturnType<typeof TestBed.createComponent>, value: string): HTMLInputElement {
    const input = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return input;
  }

  /** Die Fehlermeldung erscheint erst, wenn das Feld berührt ist. */
  function blurInput(input: HTMLInputElement): void {
    input.dispatchEvent(new Event('blur'));
  }

  it('prefills the field with the stored address', () => {
    const fixture = open('https://entsorger.example/kalender.ics');
    const compiled = fixture.nativeElement as HTMLElement;

    expect((compiled.querySelector('input') as HTMLInputElement).value).toBe(
      'https://entsorger.example/kalender.ics',
    );
    expect(compiled.textContent).toContain('38 / 500');
  });

  /** AC18: die Grenze wird genannt und der Zähler wechselt die Farbe. */
  it('counts characters and names the limit', () => {
    const fixture = open('https://entsorger.example/kalender.ics');
    const compiled = fixture.nativeElement as HTMLElement;
    const counter = compiled.querySelector('.waste-calendar__counter') as HTMLElement;

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
    expect(compiled.textContent).toContain('höchstens 500 Zeichen');
  });

  it('reports a change, not the unchanged address', () => {
    const fixture = open('https://entsorger.example/kalender.ics');
    const saved: string[] = [];
    fixture.componentRef.instance.saveRequested.subscribe((url) => saved.push(url));

    const saveButton = (fixture.nativeElement as HTMLElement).querySelector(
      '.hm-button--primary',
    ) as HTMLButtonElement;
    expect(saveButton.disabled).toBe(true);

    typeInput(fixture, 'https://entsorger.example/anderer.ics');
    expect(saveButton.disabled).toBe(false);
    saveButton.click();

    expect(saved).toEqual(['https://entsorger.example/anderer.ics']);
  });

  /** R19: Abbrechen stellt die gespeicherte Adresse wieder her. */
  it('restores the stored address when editing is aborted', () => {
    const fixture = open('https://entsorger.example/kalender.ics');
    const compiled = fixture.nativeElement as HTMLElement;

    typeInput(fixture, 'https://entsorger.example/anderer.ics');
    const cancelButton = [...compiled.querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === 'Abbrechen',
    ) as HTMLButtonElement;
    cancelButton.click();
    fixture.detectChanges();

    expect((compiled.querySelector('input') as HTMLInputElement).value).toBe(
      'https://entsorger.example/kalender.ics',
    );
  });

  /** Ohne gespeicherte Adresse gibt es nichts zu entfernen. */
  it('offers removal only for a stored address', () => {
    const without = open(null);
    const removeButton = (without.nativeElement as HTMLElement).querySelector(
      '.waste-calendar__remove',
    ) as HTMLButtonElement;
    expect(removeButton.disabled).toBe(true);

    const withSource = open('https://entsorger.example/kalender.ics');
    const enabled = (withSource.nativeElement as HTMLElement).querySelector(
      '.waste-calendar__remove',
    ) as HTMLButtonElement;
    expect(enabled.disabled).toBe(false);
  });
});
