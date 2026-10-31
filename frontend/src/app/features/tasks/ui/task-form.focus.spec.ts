import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { giveElementsGeometry } from '../../../shared/testing/jsdom-focus-geometry';
import { TaskForm } from './task-form';

/**
 * Fokusverhalten des Aufgaben-Overlays (Issue #9).
 *
 * Dasselbe Muster wie im Bestätigungsdialog: initialer Fokus auf dem Panel,
 * Fokusfalle (Tab bleibt im Dialog), Fokus zurück auf das auslösende Element
 * beim Schließen.
 *
 * Der Test steht in einer eigenen Datei, damit er unabhängig von den
 * Formular-Tests bleibt (das Repo nutzt dieses Muster auch sonst).
 */
describe('TaskForm — Fokus (Issue #9)', () => {
  beforeAll(() => {
    // Ohne Geometrie hält der CDK jedes Element für unsichtbar.
    giveElementsGeometry();
  });

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
    // geholt.
    const anchor = compiled(fixture).querySelector('.cdk-focus-trap-anchor') as HTMLElement;
    expect(anchor).not.toBeNull();

    anchor.focus();
    await fixture.whenStable();

    expect(dialog(fixture).contains(document.activeElement)).toBe(true);
  });

  it('returns the focus to the trigger when the dialog closes (AC3)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.focus();
    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    dialog(fixture).dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled(fixture).querySelector('[role="dialog"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
  });
});
