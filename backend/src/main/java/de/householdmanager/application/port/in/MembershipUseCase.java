package de.householdmanager.application.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Eingehender Port: Mitgliedschaft in einem Haushalt.
 *
 * <p>Die Haushaltsleitung (Ersteller/in des Haushalts) ernennt und entfernt
 * Mitglieder aus dem Nutzerverzeichnis. Alle Mitgliedschafts-Prüfungen der
 * anderen Use Cases laufen über diesen Port.
 *
 * <p>Implementierung: {@code application.usecase.MembershipUseCaseImpl}.
 */
public interface MembershipUseCase {

    /** Mitglied zur Anzeige: Kennung plus aufgelöster Name (null, wenn nicht auflösbar). */
    record MemberView(String userId, String displayName, boolean householdAdmin) {
    }

    /** Mitglieder des Haushalts, Haushaltsleitung zuerst. */
    List<MemberView> listMembers(String currentUserId, long householdId);

    /**
     * Der Anzeigename einer Person — leer, wenn er sich nicht auflösen lässt.
     *
     * <p>Für Fälle, in denen nur einzelne Personen benannt werden müssen:
     * das spart die Auflösung aller Mitglieder.
     */
    Optional<String> displayNameOf(String userId);

    /** Ernennt eine Person des Nutzerverzeichnisses zum Mitglied (nur Haushaltsleitung). */
    MemberView addMember(String currentUserId, long householdId, String userId);

    /** Entfernt den Zugang eines Mitglieds; Aufgaben und Zuweisungen bleiben erhalten. */
    void removeMember(String currentUserId, long householdId, String userId);

    /** Prüft die Mitgliedschaft; wirft {@code MembershipRequiredException}, wenn nicht Mitglied. */
    void requireMembership(long householdId, String userId);

    /** Reine Abfrage ohne Nebenwirkung: ist die Person Mitglied des Haushalts? */
    boolean isMember(long householdId, String userId);
}
