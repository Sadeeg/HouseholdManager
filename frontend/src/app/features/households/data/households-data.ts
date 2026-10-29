import { httpResource } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { HouseholdApi, HOUSEHOLDS_RESOURCE_URL } from './household-api';
import { Household } from '../model/household.model';

/**
 * Reaktiver Lesezugriff auf die Haushalte.
 *
 * `httpResource` ist eager: der Request läuft, sobald die Resource im
 * injizierten Kontext erzeugt wird, und wieder bei jeder Änderung der
 * Request-Funktion.
 *
 * Der Service wird route-scoped bereitgestellt (siehe `routes.ts`): der
 * Zustand lebt damit nur, solange das Feature aktiv ist.
 */
@Injectable()
export class HouseholdsData {
  private readonly householdApi = inject(HouseholdApi);

  readonly households = httpResource<Household[]>(() => HOUSEHOLDS_RESOURCE_URL);

  reload(): void {
    this.households.reload();
  }

  create(name: string) {
    return this.householdApi.create(name);
  }
}
