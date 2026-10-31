import { Injectable } from '@angular/core';

import { API_CONFIG } from '../../../core/config/api.config';

/**
 * Basis-URL der Müllterm-Ressource eines Haushalts.
 *
 * Gehört zur Daten-Schicht des Features: der Client kennt nur seinen
 * Feature-Kontext. Lesen läuft über `httpResource` (siehe `WasteData`); die
 * Schreibzugriffe auf die Quelle gehören zum Haushalt und liegen im
 * Household-API-Client des Features „Haushalte" — dieses Feature darf ein
 * anderes Feature nicht benutzen (Modulgrenze).
 */
export const WASTE_RESOURCE_PATH = 'households';

@Injectable({ providedIn: 'root' })
export class WasteApi {
  /** Lese-URL der Übersicht für den given Haushalt. */
  overviewUrl(householdId: number): string {
    return `${API_CONFIG.basePath}/${WASTE_RESOURCE_PATH}/${householdId}/waste`;
  }
}
