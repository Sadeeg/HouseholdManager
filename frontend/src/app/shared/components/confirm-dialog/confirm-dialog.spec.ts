import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

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
