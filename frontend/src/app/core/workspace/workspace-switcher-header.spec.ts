import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { WorkspaceService } from './workspace-service';
import { WorkspaceSwitcher } from './workspace-switcher';

function tick(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

/**
 * Regressionstest für Issue #14 (AC-k1, AC-k2).
 *
 * Die Geometrie selbst ist im/jsdom nicht prüfbar (Position, Breite) — dafür
 * gibt es die Sichtprüfung am laufenden Stack. Prüfbar ist aber der
 * *Deklarierte Vertrag* des Stylesheets, und genau der hat in Runde 1
 * versagt: `app-workspace-switcher` ist ein Flex-Item, deshalb gilt für es
 * `min-width: auto` = min-content des unumbrochenen Haushaltsnamens. Diese
 * Mindestbreite überschritt die `max-width`-Schranke, woraufhin der Kopf ab
 * 768 px überlief (838 px Dokumentbreite). Ohne `min-width: 0` ist die
 * Schranke wirkungslos — das prüft dieser Test.
 *
 * Zweite Hälfte: der Name wird mit Ellipse gekürzt, der Marker „▾" muss
 * daneben stehen bleiben und darf nie mit gekürzt werden.
 */
describe('WorkspaceSwitcher — Kopf darf nicht überbreit werden (Issue #14)', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [WorkspaceSwitcher],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);

    const workspace = TestBed.inject(WorkspaceService);
    const loading = workspace.ensureLoaded();
    httpMock.expectOne('/api/households').flush([{ id: 1, name: 'Haus A' }]);
    await tick();
    httpMock.expectOne('/api/me/workspace').flush(1);
    httpMock.expectOne('/api/auth/me').flush({ id: 'alice', displayName: 'Alice', email: 'a@example.org' });
    await loading;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function render(): HTMLElement {
    const fixture = TestBed.createComponent(WorkspaceSwitcher);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('setzt min-width: 0 am Host, damit max-width als Flex-Item wirkt (AC-k1)', () => {
    const host = render();

    const hostStyle = getComputedStyle(host);
    expect(hostStyle.minWidth).toBe('0px');
    expect(hostStyle.maxWidth).not.toBe('none');
  });

  it('kürzt den Haushaltsnamen mit Ellipse statt ihn überstehen zu lassen (AC-k1, AC-k2)', () => {
    const host = render();

    const name = host.querySelector('.workspace-switcher__name') as HTMLElement;
    const nameStyle = getComputedStyle(name);
    expect(nameStyle.overflow).toBe('hidden');
    expect(nameStyle.textOverflow).toBe('ellipsis');
    expect(nameStyle.whiteSpace).toBe('nowrap');
  });

  it('hält den Marker „▾" außerhalb des gekürzten Namens (AC-k2)', () => {
    const host = render();

    const name = host.querySelector('.workspace-switcher__name') as HTMLElement;
    const marker = host.querySelector('.workspace-switcher__marker') as HTMLElement;
    expect(marker).toBeTruthy();
    expect(marker.textContent?.trim()).toBe('▾');
    // Der Marker darf nicht im gekürzten Element liegen — sonst verschwindet
    // er gemeinsam mit dem Namen.
    expect(name.contains(marker)).toBe(false);
    expect(name.textContent?.includes('▾')).toBe(false);
  });
});
