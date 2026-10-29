import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HouseholdsPage } from './households-page';
import { HouseholdsData } from './data/households-data';

describe('HouseholdsPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HouseholdsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('renders the loaded households', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();

    httpMock.expectOne('/api/households').flush([
      { id: 1, name: 'Haushalt Müller', createdAt: '2026-01-01T00:00:00Z' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const items = (fixture.nativeElement as HTMLElement).querySelectorAll('app-household-item');
    expect(items.length).toBe(1);
    expect(items[0].textContent).toContain('Haushalt Müller');
  });

  it('shows a hint when there is no household yet', async () => {
    const fixture = TestBed.createComponent(HouseholdsPage);
    fixture.detectChanges();

    httpMock.expectOne('/api/households').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.state')?.textContent).toContain('noch kein Haushalt');
  });
});
