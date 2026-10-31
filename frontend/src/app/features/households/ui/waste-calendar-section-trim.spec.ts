import { TestBed } from '@angular/core/testing';

import { WasteCalendarSection } from './waste-calendar-section';
import { WASTE_URL_MAX_LENGTH } from '../model/household.model';

/**
 * AC22 (R18) an der Grenze — getrimmt gemessen.
 *
 * <p>Die Oberfläche misst die Länge des Textes, der gespeichert wird: der
 * Validator, der Zähler und die Speichern-Aktion zählen den getrimmten Text.
 * Eine Adresse, die im Feld ein Zeichen länger ist, weil sie am Ende ein
 * Leerzeichen trägt, ist deshalb speicherbar — sie wird ohne das Leerzeichen
 * gespeichert.
 *
 * <p>`waste-calendar-section-limit.spec.ts` belegt den Fall „deutlich zu
 * lang" (501 Zeichen ohne Leerzeichen → gesperrt). Dieser Test belegt die
 * Grenze selbst: 500 Zeichen nach dem Trimmen bleiben speicherbar, 501 Zeichen
 * nach dem Trimmen nicht. Ohne diesen Test bleibt offen, ob Zähler, Validator
 * und Speichern-Aktion an der Grenze dasselbe messen.
 */
describe('WasteCalendarSection — die Grenze zählt den getrimmten Text', () => {
  function open(storedUrl: string | null) {
    const fixture = TestBed.createComponent(WasteCalendarSection);
    fixture.componentRef.setInput('householdId', 1);
    fixture.componentRef.setInput('householdName', 'Haus A');
    fixture.componentRef.setInput('storedUrl', storedUrl);
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
    input(fixture).dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const counter = compiled.querySelector('.waste-calendar__counter') as HTMLElement;

    expect(counter.textContent?.trim()).toBe(`${WASTE_URL_MAX_LENGTH} / ${WASTE_URL_MAX_LENGTH}`);
    expect(counter.classList.contains('waste-calendar__counter--danger')).toBe(false);
    expect(compiled.querySelector('.waste-calendar__error')).toBeNull();
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
});
