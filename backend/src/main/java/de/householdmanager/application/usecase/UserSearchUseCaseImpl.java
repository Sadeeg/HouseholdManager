package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.UserSearchUseCase;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.UserAccount;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert die Personensuche: gibt die Treffer des Nutzerverzeichnisses
 * als Vorschläge weiter — ohne sensible Felder wie die E-Mail.
 */
@Service
@RequiredArgsConstructor
public class UserSearchUseCaseImpl implements UserSearchUseCase {

    /** Obergrenze für Vorschläge — ein Auswahlfeld, keine Trefferliste. */
    private static final int SUGGESTION_LIMIT = 10;

    private final UserDirectory userDirectory;

    @Override
    @Transactional(readOnly = true)
    public List<UserRef> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userDirectory.search(query.trim(), SUGGESTION_LIMIT).stream()
                .map(UserSearchUseCaseImpl::toRef)
                .toList();
    }

    private static UserRef toRef(UserAccount user) {
        return new UserRef(user.id(), user.displayName());
    }
}
