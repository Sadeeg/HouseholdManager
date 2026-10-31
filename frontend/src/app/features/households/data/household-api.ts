import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../../../core/config/api.config';
import { Household, WasteSourceResponse } from '../model/household.model';
import { Member } from '../model/member.model';

const HOUSEHOLDS_URL = `${API_CONFIG.basePath}/households`;

/** Basis-URL der Haushalts-Ressource — eine Quelle der Wahrheit für den Data-Service. */
export const HOUSEHOLDS_RESOURCE_URL = HOUSEHOLDS_URL;

/**
 * Zugriff auf die Haushalts-Ressource des Backends.
 *
 * Gehört zur Daten-Schicht (`data`) des Features: der Client kennt nur
 * seinen Feature-Kontext, nicht das Backend als Ganzes.
 *
 * Lesezugriffe laufen über `httpResource` (siehe `HouseholdsData`),
 * Schreibzugriffe hier über `HttpClient`.
 */
@Injectable({ providedIn: 'root' })
export class HouseholdApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Household[]> {
    return this.http.get<Household[]>(HOUSEHOLDS_URL);
  }

  create(name: string): Observable<Household> {
    return this.http.post<Household>(HOUSEHOLDS_URL, { name });
  }

  listMembers(householdId: number): Observable<Member[]> {
    return this.http.get<Member[]>(`${HOUSEHOLDS_URL}/${householdId}/members`);
  }

  addMember(householdId: number, userId: string): Observable<Member> {
    return this.http.post<Member>(`${HOUSEHOLDS_URL}/${householdId}/members`, { userId });
  }

  removeMember(householdId: number, userId: string): Observable<void> {
    return this.http.delete<void>(`${HOUSEHOLDS_URL}/${householdId}/members/${userId}`);
  }

  /** Quelle setzen: das Backend synchronisiert danach sofort (AC11, R6). */
  saveWasteSource(householdId: number, url: string): Observable<WasteSourceResponse> {
    return this.http.put<WasteSourceResponse>(`${HOUSEHOLDS_URL}/${householdId}/waste/source`, { url });
  }

  /** Quelle entfernen; die gespeicherten Termine bleiben (Annahme 5). */
  removeWasteSource(householdId: number): Observable<void> {
    return this.http.delete<void>(`${HOUSEHOLDS_URL}/${householdId}/waste/source`);
  }
}
