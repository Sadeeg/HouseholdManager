import { httpResource } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HouseholdApi, HOUSEHOLDS_RESOURCE_URL } from './household-api';
import { Household } from '../model/household.model';
import { Member } from '../model/member.model';

/**
 * Reaktiver Lesezugriff auf die Haushalte und ihre Mitglieder.
 *
 * `httpResource` ist eager: der Request läuft, sobald die Resource im
 * injizierten Kontext erzeugt wird, und wieder bei jeder Änderung der
 * Request-Funktion. Die Mitglieder-Resource folgt dem geöffneten Abschnitt —
 * ohne offenen Abschnitt läuft kein Request.
 *
 * Der Service wird route-scoped bereitgestellt (siehe `routes.ts`).
 */
@Injectable()
export class HouseholdsData {
  private readonly householdApi = inject(HouseholdApi);

  readonly households = httpResource<Household[]>(() => HOUSEHOLDS_RESOURCE_URL);

  private readonly membersHouseholdId = signal<number | null>(null);

  /** Öffnet (null: schließt) den Mitglieder-Abschnitt eines Haushalts. */
  showMembers(householdId: number | null): void {
    this.membersHouseholdId.set(householdId);
  }

  reloadMembers(): void {
    if (this.membersHouseholdId() !== null) {
      this.members.reload();
    }
  }

  readonly members = httpResource<Member[]>(() => {
    const householdId = this.membersHouseholdId();
    if (householdId === null) {
      return undefined;
    }
    return `${HOUSEHOLDS_RESOURCE_URL}/${householdId}/members`;
  });

  reload(): void {
    this.households.reload();
  }

  /**
   * Legt einen Haushalt an und lädt die Liste neu.
   *
   * Die anlegende Person ist danach Haushaltsleitung und Mitglied — das
   * Backend nimmt sie in die Mitgliedschaft auf.
   */
  async create(name: string): Promise<Household> {
    const household = await firstValueFrom(this.householdApi.create(name));
    this.households.reload();
    return household;
  }

  async addMember(householdId: number, userId: string): Promise<void> {
    await firstValueFrom(this.householdApi.addMember(householdId, userId));
    this.members.reload();
  }

  async removeMember(householdId: number, userId: string): Promise<void> {
    await firstValueFrom(this.householdApi.removeMember(householdId, userId));
    this.members.reload();
  }
}
