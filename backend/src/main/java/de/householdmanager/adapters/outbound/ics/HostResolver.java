package de.householdmanager.adapters.outbound.ics;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Die Auflösung eines Hostnamens in Adressen.
 *
 * <p>Eigene kleine Schnittstelle, damit der Adapter im Test keine echte
 * Namensauflösung braucht: der Test setzt eine bekannte Adresse (Issue #16).
 */
@FunctionalInterface
interface HostResolver {

    /** Die Adressen des Hostnamens; wirft {@link UnknownHostException}, wenn es keine gibt. */
    InetAddress[] resolve(String host) throws UnknownHostException;
}
