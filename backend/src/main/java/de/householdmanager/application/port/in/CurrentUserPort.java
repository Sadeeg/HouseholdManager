package de.householdmanager.application.port.in;

import de.householdmanager.domain.model.UserAccount;
import java.util.Optional;

/**
 * Eingehender Port: der angemeldete Nutzer der aktuellen Anfrage.
 *
 * <p>Implementierung: {@code adapters.inbound.security.SecurityContextCurrentUser}.
 * Die Anwendung weiß nicht, woher die Identität kommt (Session, Token,
 * Header) — nur, dass es sie gibt.
 */
public interface CurrentUserPort {

    /** Der angemeldete Nutzer, oder {@code empty}, wenn niemand angemeldet ist. */
    Optional<UserAccount> currentUser();
}
