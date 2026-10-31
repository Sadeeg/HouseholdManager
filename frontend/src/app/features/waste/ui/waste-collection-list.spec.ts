import { TestBed } from '@angular/core/testing';

import { WasteCollectionList } from './waste-collection-list';
import { WasteTerm, sortTerms } from '../model/waste.model';

const term = (date: string, wasteType: string): WasteTerm => ({ date, wasteType });

/**
 * AC4: die Liste zeigt die Termine in Reihenfolge der Anwendung — aufsteigend
 * nach Datum, bei gleichem Datum nach Tonnenart. R12: ein Termin ist ganztägig,
 * die Zeile trägt keine Uhrzeit.
 */
describe('WasteCollectionList', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WasteCollectionList],
    });
  });

  function render(terms: WasteTerm[]): HTMLElement {
    const fixture = TestBed.createComponent(WasteCollectionList);
    fixture.componentRef.setInput('terms', terms);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function rowsIn(compiled: HTMLElement): string[] {
    return [...compiled.querySelectorAll('.waste-list__row')].map((row) => row.textContent ?? '');
  }

  it('renders terms in the order the application delivers them', () => {
    const compiled = render([term('2026-01-09', 'Restmüll'), term('2026-01-20', 'Papier')]);
    const rows = rowsIn(compiled);

    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('Restmüll');
    expect(rows[1]).toContain('Papier');
  });

  /** AC4 und Annahme 8: bei gleichem Datum entscheidet die Tonnenart. */
  it('orders by waste type when the date is the same', () => {
    const sorted = sortTerms([term('2026-01-09', 'Restmüll'), term('2026-01-09', 'Bio'), term('2026-01-05', 'Glas')]);

    expect(sorted.map((t) => t.wasteType)).toEqual(['Glas', 'Bio', 'Restmüll']);
  });

  /** R12: die Zeile nennt nur den Tag, keine Uhrzeit — und der gespeicherte Tag, nicht der Zeitzone-fremde. */
  it('shows a term as a whole day without a clock time', () => {
    const compiled = render([term('2026-01-09', 'Restmüll')]);
    const row = compiled.querySelector('.waste-list__row')!;
    const time = row.querySelector('time');

    expect(time?.getAttribute('datetime')).toBe('2026-01-09');
    expect(time?.textContent?.trim()).toBe('09.01.2026');
  });

  it('renders no rows for an empty window', () => {
    const compiled = render([]);
    expect(rowsIn(compiled)).toHaveLength(0);
  });
});
