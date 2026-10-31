import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { giveElementsGeometry } from '../../testing/jsdom-focus-geometry';
import { ConfirmDialog } from './confirm-dialog';

@Component({
  imports: [ConfirmDialog],
  template: `
    <app-confirm-dialog
      title="Aufgabe löschen"
      description="Diese Aufgabe wird endgültig gelöscht."
      detail="Auch ihre Labels, Frist und Zuständige werden entfernt."
      actionLabel="Endgültig löschen"
      [busy]="busy"
      (confirmed)="confirmedCount = confirmedCount + 1"
      (cancelled)="cancelledCount = cancelledCount + 1"
    />
  `,
})
class HostComponent {
  busy = false;
  confirmedCount = 0;
  cancelledCount = 0;
}

describe('ConfirmDialog', () => {
  beforeAll(() => {
    // Ohne Geometrie hält der CDK jedes Element für unsichtbar — und warnt
    // bei jedem Öffnen des Dialogs.
    giveElementsGeometry();
  });

  function setup() {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    return fixture;
  }

  function compiled(fixture: ReturnType<typeof setup>) {
    return fixture.nativeElement as HTMLElement;
  }

  it('names title, object and action — text only, never color alone (AC21)', () => {
    const fixture = setup();
    const dialog = compiled(fixture).querySelector('[role="alertdialog"]');
    expect(dialog?.textContent).toContain('Aufgabe löschen');
    expect(dialog?.textContent).toContain('Diese Aufgabe wird endgültig gelöscht.');
    expect(dialog?.textContent).toContain('Auch ihre Labels, Frist und Zuständige werden entfernt.');
    expect(dialog?.textContent).toContain('Endgültig löschen');
    expect(dialog?.textContent).toContain('Abbrechen');
  });

  it('emits confirmed only on the destructive action', () => {
    const fixture = setup();
    const buttons = compiled(fixture).querySelectorAll('button');
    const action = [...buttons].find((button) => button.textContent?.includes('Endgültig löschen'));
    action?.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.confirmedCount).toBe(1);
    expect(fixture.componentInstance.cancelledCount).toBe(0);
  });

  it('emits cancelled on „Abbrechen“ — without confirmation nothing changes', () => {
    const fixture = setup();
    const cancel = [...compiled(fixture).querySelectorAll('button')].find((button) =>
      button.textContent?.includes('Abbrechen'),
    );
    cancel?.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.cancelledCount).toBe(1);
    expect(fixture.componentInstance.confirmedCount).toBe(0);
  });

  it('disables both actions while busy and shows the loading text', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.busy = true;
    fixture.detectChanges();

    const buttons = [...compiled(fixture).querySelectorAll('.confirm-dialog__actions button')] as HTMLButtonElement[];
    expect(buttons.length).toBe(2);
    expect(buttons.every((button) => button.disabled)).toBe(true);
    expect(compiled(fixture).textContent).toContain('Wird gelöscht …');
  });
});

/**
 * Fokusverhalten des Overlays (Issue #9): initialer Fokus auf dem Panel,
 * Fokusfalle (Tab bleibt im Dialog), Fokus zurück auf das auslösende
 * Element beim Schließen — und `Esc` schließt weiterhin.
 */
describe('ConfirmDialog — Fokus (Issue #9)', () => {
  @Component({
    imports: [ConfirmDialog],
    template: `
      <button type="button" class="trigger" (click)="open = true">Aufgabe löschen</button>
      @if (open) {
        <app-confirm-dialog
          title="Aufgabe löschen"
          description="Diese Aufgabe wird endgültig gelöscht."
          (cancelled)="open = false"
        />
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

  function panel(fixture: ReturnType<typeof setup>): HTMLElement {
    return compiled(fixture).querySelector('[role="alertdialog"]') as HTMLElement;
  }

  it('puts the initial focus on the panel when it opens (AC1)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.focus();
    expect(document.activeElement).toBe(trigger);

    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(document.activeElement).toBe(panel(fixture));
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

    expect(panel(fixture).contains(document.activeElement)).toBe(true);
  });

  it('returns the focus to the trigger when the dialog closes (AC3)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.focus();
    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    panel(fixture).dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled(fixture).querySelector('[role="alertdialog"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
  });

  it('closes with Escape even with the focus trap in place (AC4)', async () => {
    const fixture = setup();
    const trigger = compiled(fixture).querySelector('.trigger') as HTMLButtonElement;
    trigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    panel(fixture).dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(compiled(fixture).querySelector('[role="alertdialog"]')).toBeNull();
  });
});
