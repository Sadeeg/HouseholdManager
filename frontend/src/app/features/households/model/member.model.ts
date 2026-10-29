/**
 * Mitglied eines Haushalts (clientseitige Sicht des MemberView).
 * `householdAdmin` markiert die Haushaltsleitung.
 */
export interface Member {
  readonly userId: string;
  readonly displayName: string | null;
  readonly householdAdmin: boolean;
}
