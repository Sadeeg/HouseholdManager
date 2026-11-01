import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, maxLength, required, submit } from '@angular/forms/signals';

import { WorkspaceService } from '../../core/workspace/workspace-service';
import { PageHeader } from '../../shared/components/page-header/page-header';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { HouseholdsData } from './data/households-data';
import { Household } from './model/household.model';
import { Member } from './model/member.model';
import { TelegramChat } from './model/telegram-chat.model';
import { HouseholdItem } from './ui/household-item';
import { MemberSection } from './ui/member-section';
import { TelegramChatSection } from './ui/telegram-chat-section';
import { WasteCalendarSection } from './ui/waste-calendar-section';

/**
 * Smart Component (Feature-Schicht) des Features „Haushalte".
 *
 * Holt die Daten über den Data-Service und verteilt sie an die dumb
 * Components der UI-Schicht. Der Mitglieder-Abschnitt (AC11) klappt inline
 * im Eintrag auf und ist nur für die Haushaltsleitung sichtbar — die
 * Leitung ist die Person, die den Haushalt angelegt hat (`createdBy`).
 *
 * Haushalte lassen sich hier anlegen; die anlegende Person ist danach
 * Leitung und Mitglied und der neue Haushalt wird sofort zum Workspace.
 */
@Component({
  selector: 'app-households-page',
  imports: [
    FormField,
    HouseholdItem,
    MemberSection,
    PageHeader,
    ConfirmDialog,
    TelegramChatSection,
    WasteCalendarSection,
  ],
  templateUrl: './households-page.html',
  styleUrl: './households-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HouseholdsPage {
  protected readonly householdsData = inject(HouseholdsData);
  protected readonly workspace = inject(WorkspaceService);

  protected readonly expandedHouseholdId = signal<number | null>(null);
  protected readonly pendingMemberRemove = signal<{ member: Member; householdId: number } | null>(null);
  protected readonly removing = signal(false);
  protected readonly toast = signal<string | null>(null);

  // Müllkalender: jedes Mitglied darf die Quelle pflegen (AC23) — deshalb ist
  // der Abschnitt für jeden geöffneten Haushalt erreichbar, nicht nur für die Leitung.
  protected readonly expandedWasteHouseholdId = signal<number | null>(null);
  protected readonly pendingWasteRemove = signal<number | null>(null);
  protected readonly savingSource = signal(false);
  protected readonly sourceError = signal<string | null>(null);

  // Anlegen
  protected readonly createOpen = signal(false);
  protected readonly creating = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly nameModel = signal({ name: '' });
  protected readonly nameForm = form(this.nameModel, (schema) => {
    required(schema.name, { message: 'Der Name fehlt.' });
    maxLength(schema.name, 120, { message: 'Der Name ist zu lang.' });
  });

  protected readonly members = this.householdsData.members;

  // Telegram-Chats: der Abschnitt klappt inline auf wie der Müllkalender.
  // Code und Chat-Liste werden getrennt gelesen — sie haben eigene Lade-
  // und Fehlerzustände und bleiben unabhängig nutzbar.
  protected readonly expandedTelegramHouseholdId = signal<number | null>(null);
  protected readonly pendingChatDisconnect = signal<{ chat: TelegramChat; householdId: number } | null>(
    null,
  );
  protected readonly disconnectingChat = signal(false);
  protected readonly disconnectError = signal<string | null>(null);

  protected readonly telegramCode = this.householdsData.telegramCode;
  protected readonly telegramChats = this.householdsData.telegramChats;

  /**
   * Der Code für die Anzeige — `null`, solange er nicht da ist.
   *
   * <p>Das Lesen von `value()` wirft im Fehlerzustand der Resource. Die
   * Bedingung prüft deshalb zuerst den Fehler: im Fehlerfall gibt es
   * keinen Wert, und der Fehler wird ohnehin über die Fehlerzeile
   * dargestellt.
   */
  protected readonly telegramCodeValue = computed(() => {
    const resource = this.telegramCode;
    if (resource.error()) {
      return null;
    }
    return resource.value()?.code ?? null;
  });

  /** Ist der Bot konfiguriert? Ohne Token lässt sich kein Chat freischalten. */
  protected readonly telegramBotConfigured = computed(() => {
    const resource = this.telegramCode;
    if (resource.error()) {
      return true;
    }
    return resource.value()?.botConfigured ?? true;
  });

  /**
   * Die Chats für die Anzeige — `null`, solange sie nicht da sind.
   *
   * <p>Wie beim Code: im Fehlerzustand gibt es keinen Wert.
   */
  protected readonly telegramChatsValue = computed(() => {
    const resource = this.telegramChats;
    if (resource.error()) {
      return null;
    }
    return resource.value()?.chats ?? null;
  });

  protected readonly removeDescription = computed(() => {
    const pending = this.pendingMemberRemove();
    if (pending === null) {
      return '';
    }
    const { member } = pending;
    return `${member.displayName ?? member.userId} wird aus diesem Haushalt entfernt. Der Zugang zu den Aufgaben endet; Aufgaben und Zuweisungen bleiben erhalten.`;
  });

  protected readonly sourceRemoveDescription = computed(() => {
    const householdId = this.pendingWasteRemove();
    if (householdId === null) {
      return '';
    }
    return 'Die hinterlegte ICS-Adresse wird entfernt.';
  });

  /** Der Dialog nennt den Chat beim Namen — der Nutzer muss erkennen, was er trennt. */
  protected readonly chatDisconnectDescription = computed(() => {
    const pending = this.pendingChatDisconnect();
    if (pending === null) {
      return '';
    }
    return `Der Telegram-Chat „${pending.chat.chatName}" wird von diesem Haushalt getrennt. Der Chat erhält keine weiteren Benachrichtigungen.`;
  });

  protected isHouseholdAdmin(household: Household): boolean {
    const user = this.workspace.currentUser();
    return user !== null && user.id === household.createdBy;
  }

  protected toggleMembers(householdId: number): void {
    if (this.expandedHouseholdId() === householdId) {
      this.expandedHouseholdId.set(null);
      this.householdsData.showMembers(null);
    } else {
      this.expandedHouseholdId.set(householdId);
      this.householdsData.showMembers(householdId);
    }
  }

  /** Öffnet (bzw. schließt) das Formular zum Anlegen. */
  protected toggleCreate(): void {
    this.createError.set(null);
    this.createOpen.update((open) => !open);
  }

  /**
   * Legt den Haushalt an und übernimmt ihn als Workspace: die anlegende
   * Person ist danach Leitung und Mitglied, die Tafel ist sofort erreichbar.
   */
  protected saveHousehold(): void {
    submit(this.nameForm, async () => {
      this.creating.set(true);
      this.createError.set(null);
      try {
        const household = await this.householdsData.create(this.nameModel().name.trim());
        await this.workspace.adoptHousehold({ id: household.id, name: household.name });
        this.nameModel.set({ name: '' });
        this.createOpen.set(false);
        this.showToast('Haushalt angelegt.');
      } catch {
        this.createError.set(
          'Der Haushalt konnte nicht angelegt werden. Bitte später erneut versuchen.',
        );
      } finally {
        this.creating.set(false);
      }
    });
  }

  protected async addMember(householdId: number, userId: string): Promise<void> {
    try {
      await this.householdsData.addMember(householdId, userId);
      this.showToast('Person ernannt.');
    } catch {
      this.showToast('Die Person konnte nicht ernannt werden.');
    }
  }

  protected async confirmMemberRemove(): Promise<void> {
    const pending = this.pendingMemberRemove();
    if (pending === null) {
      return;
    }
    this.removing.set(true);
    try {
      await this.householdsData.removeMember(pending.householdId, pending.member.userId);
      this.pendingMemberRemove.set(null);
    } catch {
      this.showToast('Die Person konnte nicht entfernt werden.');
    } finally {
      this.removing.set(false);
    }
  }

  /** Öffnet (bzw. schließt) den Müllkalender-Abschnitt. */
  protected toggleWasteCalendar(householdId: number): void {
    this.sourceError.set(null);
    this.expandedWasteHouseholdId.update((open) => (open === householdId ? null : householdId));
  }

  /**
   * Speichert die ICS-Adresse. Das Backend synchronisiert danach sofort, und
   * die Antwort wartet auf den Lauf — die Liste der Haushalte wird neu
   * geladen, damit die geänderte Adresse sichtbar bleibt (AC11).
   */
  protected async saveSource(householdId: number, url: string): Promise<void> {
    this.savingSource.set(true);
    this.sourceError.set(null);
    try {
      await this.householdsData.saveWasteSource(householdId, url);
      this.showToast('Müllkalender gespeichert.');
    } catch {
      this.sourceError.set(
        'Die ICS-Adresse konnte nicht gespeichert werden: Der Dienst ist nicht erreichbar. Deine Eingabe bleibt erhalten.',
      );
    } finally {
      this.savingSource.set(false);
    }
  }

  protected async confirmSourceRemove(): Promise<void> {
    const householdId = this.pendingWasteRemove();
    if (householdId === null) {
      return;
    }
    this.savingSource.set(true);
    try {
      await this.householdsData.removeWasteSource(householdId);
      this.pendingWasteRemove.set(null);
      this.showToast('Müllkalender entfernt.');
    } catch {
      this.sourceError.set('Der Müllkalender konnte nicht entfernt werden.');
    } finally {
      this.savingSource.set(false);
    }
  }

  /** Öffnet (bzw. schließt) den Telegram-Abschnitt. */
  protected toggleTelegramChats(householdId: number): void {
    this.disconnectError.set(null);
    const isOpen = this.expandedTelegramHouseholdId() === householdId;
    this.expandedTelegramHouseholdId.set(isOpen ? null : householdId);
    // Ohne offenen Abschnitt läuft kein Request.
    this.householdsData.showTelegram(isOpen ? null : householdId);
  }

  /** Lädt Code und Chat-Liste erneut — nach einem Ladefehler. */
  protected reloadTelegram(): void {
    this.householdsData.reloadTelegram();
  }

  /**
   * Trennt eine eigene Verbindung. Die Bestätigung passiert im Dialog — erst
   * danach wird getrennt (AC13).
   */
  protected async confirmChatDisconnect(): Promise<void> {
    const pending = this.pendingChatDisconnect();
    if (pending === null) {
      return;
    }
    this.disconnectingChat.set(true);
    this.disconnectError.set(null);
    try {
      await this.householdsData.disconnectTelegramChat(pending.householdId, pending.chat.chatId);
      this.pendingChatDisconnect.set(null);
      this.showToast('Telegram-Chat getrennt.');
    } catch {
      // Die Zeile bleibt stehen, der Dialog schließt (Zustandstabelle).
      this.pendingChatDisconnect.set(null);
      this.disconnectError.set(
        'Die Verbindung konnte nicht getrennt werden. Bitte später erneut versuchen.',
      );
    } finally {
      this.disconnectingChat.set(false);
    }
  }

  private showToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => this.toast.set(null), 4000);
  }
}
