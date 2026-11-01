import { httpResource } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HouseholdApi, HOUSEHOLDS_RESOURCE_URL } from './household-api';
import { Household } from '../model/household.model';
import { Member } from '../model/member.model';
import { TelegramChatList, TelegramCode } from '../model/telegram-chat.model';

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

  // Telegram-Chats: der Abschnitt folgt dem geöffneten Haushalt — ohne
  // offenen Abschnitt läuft kein Request. Code und Chat-Liste werden
  // getrennt gelesen: sie haben eigene Lade- und Fehlerzustände.
  private readonly telegramHouseholdId = signal<number | null>(null);

  /** Öffnet (null: schließt) den Telegram-Abschnitt eines Haushalts. */
  showTelegram(householdId: number | null): void {
    this.telegramHouseholdId.set(householdId);
  }

  readonly telegramCode = httpResource<TelegramCode>(() => {
    const householdId = this.telegramHouseholdId();
    if (householdId === null) {
      return undefined;
    }
    return `${HOUSEHOLDS_RESOURCE_URL}/${householdId}/telegram/code`;
  });

  readonly telegramChats = httpResource<TelegramChatList>(() => {
    const householdId = this.telegramHouseholdId();
    if (householdId === null) {
      return undefined;
    }
    return `${HOUSEHOLDS_RESOURCE_URL}/${householdId}/telegram/chats`;
  });

  /** Lädt Code und Chat-Liste erneut — nach einem Trennen und nach einem Fehler. */
  reloadTelegram(): void {
    if (this.telegramHouseholdId() !== null) {
      this.telegramCode.reload();
      this.telegramChats.reload();
    }
  }

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

  /**
   * Speichert die ICS-Adresse. Das Backend synchronisiert danach sofort — die
   * Antwort wartet auf den Lauf (AC11, R6), deshalb wird die Liste der
   * Haushalte danach neu geladen.
   */
  async saveWasteSource(householdId: number, url: string): Promise<void> {
    await firstValueFrom(this.householdApi.saveWasteSource(householdId, url));
    this.households.reload();
  }

  async removeWasteSource(householdId: number): Promise<void> {
    await firstValueFrom(this.householdApi.removeWasteSource(householdId));
    this.households.reload();
  }

  /**
   * Trennt eine eigene Telegram-Verbindung (AC13). Die Chat-Liste wird
   * danach neu geladen — der getrennte Chat darf nicht mehr erscheinen
   * (AC15).
   */
  async disconnectTelegramChat(householdId: number, chatId: string): Promise<void> {
    await firstValueFrom(this.householdApi.disconnectTelegramChat(householdId, chatId));
    this.telegramChats.reload();
  }
}
