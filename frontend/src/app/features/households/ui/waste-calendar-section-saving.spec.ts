import { TestBed } from '@angular/core/testing';

import { WasteCalendarSection } from './waste-calendar-section';

/**
 * Der Zustand „Speichern läuft" (Zustandstabelle der `## UX`): das Eingabefeld
 * und alle Aktionen sind gesperrt, der Knopf meldet den Vorgang.
 *
 * <p>Das Component meldet den Wunsch zu speichern per Output; die Sperre
 * entsteht im Schema (`disabled(schema.url, …)`), nicht über ein
 * `[disabled]`-Attribut — Angular verbindet `[disabled]` nicht mit
 * `[formField]`. Der Test prüft die sichtbare Wirkung, nicht die Mechanik.
 */
describe('WasteCalendarSection während des Speicherns', () => {
  function open(saving: boolean) {
    const fixture = TestBed.createComponent(WasteCalendarSection);
    fixture.componentRef.setInput('householdId', 1);
    fixture.componentRef.setInput('householdName', 'Haus A');
    fixture.componentRef.setInput('storedUrl', 'https://entsorger.example/kalender.ics');
    fixture.componentRef.setInput('saving', saving);
    fixture.detectChanges();
    return fixture;
  }

  it('locks the field and all actions while the address is being saved', () => {
    const fixture = open(true);
    const compiled = fixture.nativeElement as HTMLElement;

    expect((compiled.querySelector('input') as HTMLInputElement).disabled).toBe(true);

    const buttons = [...compiled.querySelectorAll('button')] as HTMLButtonElement[];
    expect(buttons.length).toBeGreaterThan(0);
    expect(buttons.every((button) => button.disabled)).toBe(true);
  });

  it('names the running action instead of the plain label', () => {
    const fixture = open(true);
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Wird gespeichert …');
  });

  it('unlocks the field again when the save is done', () => {
    const fixture = open(false);
    const compiled = fixture.nativeElement as HTMLElement;

    expect((compiled.querySelector('input') as HTMLInputElement).disabled).toBe(false);
    expect(compiled.textContent).toContain('Speichern');
    expect(compiled.textContent).not.toContain('Wird gespeichert …');
  });
});
