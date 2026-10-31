import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { HouseholdsPage } from './households-page';
import { HouseholdsData } from './data/households-data';

/**
 * AC23 (R19): „Bricht ein Mitglied das Pflegen der Quelle ab, bleibt die
 * bisher hinterlegte Quelle unverändert."
 *
 * <p>`waste-calendar-section.spec.ts` belegt die Wirkung im Component: das Feld
 * zeigt nach „Abbrechen" wieder die gespeicherte Adresse. Das ist die halbe
 * Aussage — die Oberfläche könnte trotzdem einen Speichervorgang auslösen, der
 * die Quelle ändert. Dieser Test schließt die Lücke auf der Ebene der Seite
 * (Issue #11, Befund 4): der Beleg ist `HttpTestingController.verify()` —
 * nach „Abbrechen" darf **keine** ungeprüfte Anfrage offen sein, also geht kein
 * PUT (und kein DELETE) an den Server, und die gespeicherte Quelle steht
 * unverändert in der Anzeige.
 */
describe('HouseholdsPage — abgebrochene Pflege der ICS-Adresse', () => {
  const GESPEICHERT = 'https://entsorger.example/kalender.ics';

  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HouseholdsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(WorkspaceService).currentUser.set({ id: 'alice', displayName: 'Alice' });
  });

  afterEach(() => {
    httpMock.verify();
  });

  async function tick(fixture: ReturnType<typeof TestBed.createComponent>): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  /** Öffnet den Bereich „MÜLLKALENDER" am einzigen Haushalt. */
  async function openSection(): Promise<{
    fixture: ReturnType<typeof TestBed.createComponent>;
    section: HTMLElement;
  }> {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();
    httpMock.expectOne('/api/households').flush([
      {
        id: 1,
        name: 'Haushalt Müller',
        createdAt: '2026-01-01T00:00:00Z',
        createdBy: 'alice',
        wasteCalendarUrl: GESPEICHERT,
      },
    ]);
    await tick(fixture);

    const compiled = fixture.nativeElement as HTMLElement;
    [...compiled.querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'MÜLLKALENDER')
      ?.click();
    fixture.detectChanges();

    return { fixture, section: compiled.querySelector('app-waste-calendar-section') as HTMLElement };
  }

  it('sends nothing to the server when the edit is aborted', async () => {
    const { fixture, section } = await openSection();

    const field = section.querySelector('input') as HTMLInputElement;
    field.value = 'https://entsorger.example/anderer.ics';
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const cancel = [...section.querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === 'Abbrechen',
    ) as HTMLButtonElement;
    expect(cancel.disabled).toBe(false);
    cancel.click();
    fixture.detectChanges();
    await tick(fixture);

    // Der Beleg (Issue #11, Befund 4): nach dem Lesen ist nichts offen.
    // `verify()` wirft, wenn die Oberfläche durch das „Abbrechen" trotzdem
    // einen Schreibvorgang ausgelöst hat — ein PUT wäre dann eine ungeprüfte,
    // offene Anfrage. Der Test wäre grün, obwohl der Schreibpfad lief; genau
    // das schließt `verify()` aus.
    httpMock.verify();
  });

  it('shows the unchanged source after the edit was aborted', async () => {
    const { fixture, section } = await openSection();

    const field = section.querySelector('input') as HTMLInputElement;
    field.value = 'https://entsorger.example/anderer.ics';
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const cancel = [...section.querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === 'Abbrechen',
    ) as HTMLButtonElement;
    cancel.click();
    fixture.detectChanges();

    // Das Feld steht wieder auf der gespeicherten Adresse …
    expect((section.querySelector('input') as HTMLInputElement).value).toBe(GESPEICHERT);
    // … und die Anzeige der Quelle nennt weiterhin die gespeicherte Adresse.
    expect(section.querySelector('.waste-calendar__source')?.textContent).toContain(GESPEICHERT);
    // Der Zähler steht wieder auf der Länge der gespeicherten Adresse.
    expect(section.querySelector('.waste-calendar__counter')?.textContent?.trim()).toBe(
      `${GESPEICHERT.length} / 500`,
    );
  });
});
