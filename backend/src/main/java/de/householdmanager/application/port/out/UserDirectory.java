package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.UserAccount;
import java.util.List;
import java.util.Optional;

/**
 * Ausgehender Port: das Verzeichnis der Nutzerkonten.
 *
 * <p>Implementierung: {@code adapters.outbound.nextcloud.NextcloudUserDirectory}
 * (Nextcloud OCS-API). Die Anwendung weiß nicht, woher die Konten kommen —
 * das Verzeichnis ist austauschbar.
 */
public interface UserDirectory {

    Optional<UserAccount> findById(String id);

    /** Volltextsuche; leere Query liefert eine leere Liste. */
    List<UserAccount> search(String query, int limit);
}
