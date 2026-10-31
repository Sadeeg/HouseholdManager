import { TestBed } from '@angular/core/testing';

import { WasteCalendarSection } from './waste-calendar-section';
import { WASTE_URL_MAX_LENGTH } from '../model/household.model';

/**
 * AC22 (R18): „Eine ICS-Adresse, die länger ist als die zulässige Länge, wird
 * nicht gespeichert; die Oberfläche nennt die zulässige Länge."
 *
 * <p>Der vorhandene Test `waste-calendar-section.spec.ts` belegt den Zähler
 * und die Fehlermeldung — aber nicht die halbe Aussage „wird nicht
 * gespeichert": dass die Oberfläche eine zu lange Adresse auch gar nicht erst
 * abschickt. Dieser Test schließt diese Lücke: der „Speichern"-Knopf bleibt
 * deaktiviert, und selbst ein erzwungener Klick meldet keine Adresse nach
 * außen.
 */
describe('WasteCalendarSection — Grenze der ICS-Adresse', () => {
  function open(storedUrl: string | null) {
    const fixture = TestBed.createComponent(WasteCalendarSection);
    fixture.componentRef.setInput('householdId', 1);
    fixture.componentRef.setInput('householdName', 'Haus A');
    fixture.componentRef.setInput('storedUrl', storedUrl);
    fixture.detectChanges();
    return fixture;
  }

  function typeInput(
    fixture: ReturnType<typeof TestBed.createComponent>,
    value: string,
  ): HTMLInputElement {
    const input = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return input;
  }

  function saveButton(fixture: ReturnType<typeof TestBed.createComponent>): HTMLButtonElement {
    return (fixture.nativeElement as HTMLElement).querySelector(
      '.hm-button--primary',
    ) as HTMLButtonElement;
  }

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
    const input = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain(`höchstens ${WASTE_URL_MAX_LENGTH} Zeichen`);
    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain(
      `höchstens ${WASTE_URL_MAX_LENGTH} Zeichen`,
    );
    expect(compiled.querySelector('.waste-calendar__counter')?.textContent).toContain(
      `${WASTE_URL_MAX_LENGTH + 1} / ${WASTE_URL_MAX_LENGTH}`,
    );
  });

  /** Genau an der Grenze ist die Adresse noch speicherbar. */
  it('still offers to save an address of exactly the permitted length', () => {
    const fixture = open(null);

    typeInput(fixture, 'x'.repeat(WASTE_URL_MAX_LENGTH));

    expect(saveButton(fixture).disabled).toBe(false);
  });
});
