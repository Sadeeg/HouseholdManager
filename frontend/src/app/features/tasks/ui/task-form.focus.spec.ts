import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { TaskForm } from './task-form';

/**
 * Fokusverhalten des Aufgaben-Overlays (Issue #9).
 *
 * Dasselbe Muster wie im Bestätigungsdialog: initialer Fokus auf dem Panel,
 * Fokusfalle (Tab bleibt im Dialog), Fokus zurück auf das auslösende Element
 * beim Schließen.
 *
 * Die Geometrie kommt aus dem Vitest-Setup (`shared/testing/vitest-setup.ts`,
 * registriert in `angular.json`) — nicht mehr aus einem Helfer, den jede Datei
 * selbst aufrufen muss.
 *
 * Der Test steht in einer eigenen Datei, damit er unabhängig von den
 * Formular-Tests bleibt (das Repo nutzt dieses Muster auch sonst).
 */
describe('TaskForm — Fokus (Issue #9)', () => {
  @Component({
    imports: [TaskForm],
    template: `
      <button type="button" class="trigger" (click)="open = true">Aufgabe anlegen</button>
      @if (open) {
        <app-task-form (cancelled)="open = false" />
      }
    `,
  })
  class TriggerHost {
    open = false;
  }

  function setup() {
    TestBed.configureTestingModule({ imports: [TriggerHost] });
    const fixture = TestBed.createComponent(TriggerHost);
    fixture.detectChanges();
    return fixture;
  }

  function compiled(fixture: ReturnType<typeof setup>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function dialog(fixture: ReturnType<typeof setup>): HTMLElement {
    return compiled(fixture).querySelector('[role="dialog"]') as HTMLElement;
  }

  it('puts the initial focus on the panel when it opens (AC1)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.focus();

    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(document.activeElement).toBe(dialog(fixture));
  });

  it('keeps the focus inside the dialog when it tries to leave (AC2)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Die Fokusfalle legt Anker vor und hinter den Dialog: wer über den Rand
    // hinaus tabt, landet auf dem Anker und wird zurück in den Dialog
    // geholt. Der Anker ist nur Teil der Tab-Reihenfolge, solange die Falle
    // aktiv ist — deshalb wird hier der Anker selbst fokussiert und das
    // Rückholziel geprüft, nicht nur, dass ein Anker im Template steht.
    const panelEl = dialog(fixture);
    const anchor = compiled(fixture).querySelector('.cdk-focus-trap-anchor') as HTMLElement;
    expect(anchor).not.toBeNull();
    expect(anchor.tabIndex).toBe(0);              // sonst läuft Tab im Browser über den Rand hinaus
    expect(panelEl.contains(anchor)).toBe(false); // der Anker liegt außerhalb des Panels

    anchor.focus();

    // Rückholziel der vorderen Falle = letzte Aktion im Panel. Wäre die Falle
    // deaktiviert, bliebe der Fokus auf dem Anker und diese Assertion wäre rot.
    const lastAction = [...panelEl.querySelectorAll('.task-form__actions button')].at(-1);
    expect(document.activeElement).toBe(lastAction);
    expect(panelEl.contains(document.activeElement)).toBe(true);
  });

  it('returns the focus to the trigger when the dialog closes (AC3)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.focus();
    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Zwischenzustand, ohne den die Rückkehr-Aussage beliebig wäre: der Fokus
    // muss tatsächlich im Dialog sein, bevor er zurückkehren kann.
    expect(dialog(fixture).contains(document.activeElement)).toBe(true);

    dialog(fixture).dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled(fixture).querySelector('[role="dialog"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
  });

  /**
   * AC4: die Fokusfalle darf Esc nicht blockieren. Dieselbe Fläche wie AC3 —
   * hier ausdrücklich als AC4 benannt, weil die Falle das Schließen können muss.
   */
  it('still closes on Escape — the focus trap does not block it (AC4)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled(fixture).querySelector('.cdk-focus-trap-anchor')).not.toBeNull();

    dialog(fixture).dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled(fixture).querySelector('[role="dialog"]')).toBeNull();
  });
});
