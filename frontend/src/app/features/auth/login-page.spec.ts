import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { vi } from 'vitest';

import { LoginPage } from './login-page';

describe('LoginPage', () => {
  const initCodeFlow = vi.fn();
  const tryLoginCodeFlow = vi.fn();

  beforeEach(async () => {
    initCodeFlow.mockClear();
    tryLoginCodeFlow.mockClear();

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: OAuthService, useValue: { initCodeFlow, tryLoginCodeFlow, hasValidAccessToken: () => false } },
      ],
    }).compileComponents();
  });

  it('rendert den Anmelde-Button', async () => {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('button')?.textContent).toContain('Mit Nextcloud anmelden');
  });

  it('startet den Code-Flow beim Klick', async () => {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    await fixture.whenStable();

    expect(initCodeFlow).toHaveBeenCalled();
  });

  it('nennt den Hinweis auf das Nextcloud-Konto', async () => {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Nextcloud');
  });
});
