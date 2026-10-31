import { TestBed } from '@angular/core/testing';

import { WasteCalendarSection } from './waste-calendar-section';
import { WASTE_URL_MAX_LENGTH } from '../model/household.model';

/**
 * AC22 (R18) — die zweite Hälfte der Aussage: die Oberfläche nennt die Grenze
 * **und** kappt eine zu lange Adresse nicht stillschweigend.
 *
 * <p>Der Befund aus dem zweiten Abnahmelauf: `maxLength()` von Angular Signal
 * Forms spiegelt sich als natives `maxlength`-Attribut in das Feld. Der Browser
 * schneidet dann eine zu lange Eingabe — beim Pasten — still ab: der Zähler
 * zeigt eine andere Länge als die, die gespeichert würde, und die gekürzte
 * Adresse landet beim Entsorger. Der Test nimmt die Oberfläche so, wie sie im
 * Browser erscheint: ohne `maxlength`-Attribut, mit ungekürztem Feldinhalt.
 *
 * <p>`waste-calendar-section-limit.spec.ts` prüft die Wirkung (Speichern bleibt
 * gesperrt, die Meldung nennt die Grenze). Dieser Test prüft die Ursache:
 * ohne natives Attribut kann der Browser auch nicht kürzen.
 */
describe('WasteCalendarSection — keine stille Kürzung an der Grenze', () => {
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

  it('renders the field without a native maxlength attribute', () => {
    const fixture = open(null);

    // Ein natives Attribut würde den Browser eine zu lange Eingabe abschneiden
    // lassen — die Grenze bliebe unsichtbar und die gekürzte Adresse würde
    // gespeichert.
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
    input(fixture).dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const counter = compiled.querySelector('.waste-calendar__counter') as HTMLElement;
    const save = compiled.querySelector('.hm-button--primary') as HTMLButtonElement;

    expect(counter.textContent?.trim()).toBe(`${tooLong.length} / ${WASTE_URL_MAX_LENGTH}`);
    expect(counter.classList.contains('waste-calendar__counter--danger')).toBe(true);
    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain(
      `Die ICS-Adresse darf höchstens ${WASTE_URL_MAX_LENGTH} Zeichen lang sein.`,
    );
    expect(save.disabled).toBe(true);
  });
});
