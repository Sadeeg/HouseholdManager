import { TestBed } from '@angular/core/testing';

import { WasteSourcePanel } from './waste-source-panel';

/**
 * Der Stand wird in der Ortszeit des Lesers gezeigt (AC5). Die erwartete Anzeige
 * wird aus dem selben Zeitpunkt abgeleitet, damit der Test nicht von der
 * Zeitzone der Ausführung abhängt.
 */
function standInLocalTime(iso: string): string {
  const when = new Date(iso);
  const two = (value: number): string => String(value).padStart(2, '0');
  return `${two(when.getDate())}.${two(when.getMonth() + 1)}.${when.getFullYear()} ${two(when.getHours())}:${two(when.getMinutes())}`;
}

/**
 * AC5: das Panel zeigt Quelle und Stand — und der Hinweis auf den veralteten
 * Stand nur, wenn die Anwendung ihn als veraltet meldet (R13). R16: ohne
 * erfolgreichen Lauf steht „Noch keine Synchronisation". AC18: die gespeicherte
 * Adresse wird vollständig gezeigt, nicht gekürzt.
 */
describe('WasteSourcePanel', () => {
  function render(sourceUrl: string | null, lastSyncedAt: string | null, stale: boolean) {
    const fixture = TestBed.createComponent(WasteSourcePanel);
    fixture.componentRef.setInput('sourceUrl', sourceUrl);
    fixture.componentRef.setInput('lastSyncedAt', lastSyncedAt);
    fixture.componentRef.setInput('stale', stale);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the source and the stand of the last successful run', () => {
    const compiled = render('https://entsorger.example/kalender.ics', '2026-01-01T03:00:00Z', false);

    expect(compiled.textContent).toContain('Quelle und Stand');
    expect(compiled.textContent).toContain('https://entsorger.example/kalender.ics');
    expect(compiled.textContent).toContain(standInLocalTime('2026-01-01T03:00:00Z'));
    expect(compiled.querySelector('.hm-badge')).toBeNull();
  });

  /** Der veraltete Stand kommt von der Anwendung, nicht aus dem Alter des Zeitstempels. */
  it('shows the stale badge only when the application says so', () => {
    const stale = render('https://entsorger.example/kalender.ics', '2026-01-01T03:00:00Z', true);

    expect(stale.querySelector('.hm-badge--warning')?.textContent).toContain('STAND VERALTET');
  });

  /** R16: ohne erfolgreichen Lauf kein Zeitstempel, sondern der Hinweis. */
  it('names the missing run instead of an empty stand', () => {
    const compiled = render('https://entsorger.example/kalender.ics', null, false);

    expect(compiled.textContent).toContain('Noch keine Synchronisation');
  });

  /** AC18: eine lange Adresse wird nicht abgekürzt. */
  it('shows a long address in full', () => {
    const long = `https://entsorger.example/${'x'.repeat(400)}`;
    const compiled = render(long, null, false);

    expect(compiled.textContent).toContain(long);
  });
});
