package de.householdmanager.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * Fachmodell des Müllkalenders eines Haushalts: die Quelle und ihr Stand.
 *
 * <p>Gehört zum Fachkern: keine Framework-Annotationen, keine JPA-Importe.
 * Die Quelle ist höchstens eine und optional (R3); ohne Quelle ist das Feld
 * null. Der Stand besteht aus dem Zeitpunkt des letzten erfolgreichen Laufs
 * und des letzten Versuchs — die Anzeige nennt ihn unverändert (AC5).
 */
@Getter
@Builder(toBuilder = true)
public class WasteCalendar {

    /** Grenze der Spalte {@code household.waste_calendar_url}. */
    public static final int MAX_URL_LENGTH = 500;

    /** Konvention für einen ICS-Link. Sie wird bei der Prüfung auf {@code https://} gesetzt. */
    private static final String WEBCAL_PREFIX = "webcal://";

    /** R5: das Fenster eines Laufs — sechs Wochen ab dem Tag des Laufs. */
    public static final int SYNC_WINDOW_DAYS = 42;

    private final long householdId;

    /** ICS-Adresse des Entsorgers; null, wenn kein Müllkalender hinterlegt ist. */
    private final String sourceUrl;

    /** Zeitpunkt des letzten erfolgreichen Laufs; null, wenn nie erfolgreich synchronisiert wurde. */
    private final Instant lastSyncedAt;

    /** Zeitpunkt des letzten Versuchs — auch eines fehlgeschlagenen. */
    private final Instant lastAttemptAt;

    /**
     * R13, R17 und AC6: der Stand ist veraltet, wenn ein Versuch nach dem letzten
     * erfolgreichen Lauf liegt. Gab es noch keinen erfolgreichen Lauf, ist ein
     * fehlgeschlagener Versuch bereits veraltet — die Anzeige weist den
     * Fehlschlag aus, statt eine leere Quelle zu behaupten. Ohne jeden Versuch
     * gibt es keinen veralteten Stand; die Anzeige nennt dann „Noch keine
     * Synchronisation“ (R16).
     */
    public boolean isStale() {
        if (lastAttemptAt == null) {
            return false;
        }
        return lastSyncedAt == null || lastAttemptAt.isAfter(lastSyncedAt);
    }

    /** Der Stand eines Haushalts, aus dem noch kein Lauf hervorgegangen ist. */
    public static WasteCalendar withoutRun(long householdId) {
        return WasteCalendar.builder().householdId(householdId).build();
    }

    /**
     * Fachregel R18 und AC22: die Adresse ist Pflicht, bleibt innerhalb der
     * zulässigen Länge und muss eine http- oder https-Adresse sein. Die
     * Erreichbarkeit wird nicht geprüft (Annahme 4) — ein nicht erreichbarer
     * Entsorger zeigt sich als fehlgeschlagener Lauf.
     *
     * <p>{@code webcal://} ist die Konvention für einen ICS-Link: dieselbe Adresse, nur als
     * Abonnement bezeichnet. Ein Abruf kann sie nicht lesen, deshalb wird sie bei der
     * Prüfung auf {@code https://} gesetzt — gespeichert und gezeigt wird die tatsächlich
     * abrufbare Adresse. Andere Schemata bleiben abgelehnt.
     */
    public static String validatedSourceUrl(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidWasteCalendarUrlException("Die ICS-Adresse fehlt.");
        }

        String normalized = trimmed.startsWith(WEBCAL_PREFIX)
                ? "https://" + trimmed.substring(WEBCAL_PREFIX.length())
                : trimmed;

        if (normalized.length() > MAX_URL_LENGTH) {
            throw new InvalidWasteCalendarUrlException(
                    "Die ICS-Adresse darf höchstens " + MAX_URL_LENGTH + " Zeichen lang sein.");
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new InvalidWasteCalendarUrlException(
                    "Die ICS-Adresse muss mit http:// oder https:// beginnen.");
        }
        if (isInternalTarget(hostOf(normalized))) {
            throw new InvalidWasteCalendarUrlException(
                    "Die ICS-Adresse darf nicht auf ein internes Ziel zeigen.");
        }
        return normalized;
    }

    /**
     * Der Host einer Adresse — ohne Port und Pfad. {@code null}, wenn die
     * Adresse nicht gelesen werden kann (dann greift keine Literal-Prüfung).
     */
    public static String hostOf(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (IllegalArgumentException unreadable) {
            return null;
        }
    }

    /**
     * R19 (Issue #16): die Quelle darf nicht auf ein internes Ziel zeigen —
     * sonst könnte der Server sich selbst oder das Netz des Betreibers
     * abfragen. Geprüft wird der Host als IP-Literal; ein Hostname, der auf
     * eine interne Adresse auflöst, wird beim Abruf abgewiesen (der Adapter
     * prüft die aufgelöste Adresse).
     *
     * <p>Als intern gelten: 0.0.0.0/8, 10.0.0.0/8, 100.64.0.0/10,
     * 127.0.0.0/8, 169.254.0.0/16, 172.16.0.0/12, 192.168.0.0/16, dazu ::,
     * ::1, fc00::/7, fe80::/10, der IPv4-kompatible Bereich ::/96 und der
     * IPv4-mapped-Bereich ::ffff:0:0/96.
     */
    public static boolean isInternalTarget(String host) {
        if (host == null) {
            return false;
        }
        String value = host.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        // Der Scope-Anteil (fe80::1%2) gehört nicht zur Adresse.
        int scope = value.indexOf('%');
        if (scope >= 0) {
            value = value.substring(0, scope);
        }
        if (value.isEmpty()) {
            return false;
        }
        if (isIpv4(value)) {
            return isInternalIpv4(value);
        }
        if (value.indexOf(':') >= 0) {
            return isInternalIpv6(value);
        }
        return false;
    }

    private static boolean isIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int index = 0; index < part.length(); index++) {
                if (!Character.isDigit(part.charAt(index))) {
                    return false;
                }
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInternalIpv4(String value) {
        String[] parts = value.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        if (first == 0 || first == 10 || first == 127) {
            return true;
        }
        if (first == 172 && second >= 16 && second <= 31) {
            return true;
        }
        if (first == 192 && second == 168) {
            return true;
        }
        if (first == 169 && second == 254) {
            return true;
        }
        return first == 100 && second >= 64 && second <= 127;
    }

    private static boolean isInternalIpv6(String value) {
        // IPv4-mapped (::ffff:192.168.0.1) und IPv4-compatible (::192.168.0.1)
        int lastColon = value.lastIndexOf(':');
        if (lastColon >= 0 && value.indexOf('.', lastColon) > lastColon) {
            String embedded = value.substring(lastColon + 1);
            if (isIpv4(embedded)) {
                return isInternalIpv4(embedded);
            }
        }
        int[] groups = parseIpv6(value);
        if (groups == null) {
            return false;
        }
        if (groups[0] == 0 && groups[1] == 0 && groups[2] == 0 && groups[3] == 0
                && groups[4] == 0 && groups[5] == 0xffff) {
            // ::ffff:a.b.c.d — die eingebettete IPv4-Adresse zählt
            return isInternalIpv4(ipv4Of(groups[6], groups[7]));
        }
        if (groups[0] == 0 && groups[1] == 0 && groups[2] == 0 && groups[3] == 0
                && groups[4] == 0 && groups[5] == 0) {
            // ::/96 — IPv4-kompatibel: die letzten 32 Bit sind eine
            // IPv4-Adresse (::127.0.0.1 wird vom JDK als
            // 0:0:0:0:0:0:7f00:1 geliefert). :: und ::1 liegen darin.
            return isInternalIpv4(ipv4Of(groups[6], groups[7]));
        }
        if ((groups[0] & 0xfe00) == 0xfc00) {
            return true; // fc00::/7 — unique local
        }
        return (groups[0] & 0xffc0) == 0xfe80; // fe80::/10 — link local
    }

    /** Die letzten 32 Bit einer IPv6-Adresse als IPv4-Adresse. */
    private static String ipv4Of(int high, int low) {
        return (high >> 8) + "." + (high & 0xff) + "." + (low >> 8) + "." + (low & 0xff);
    }

    /** Die acht Gruppen einer IPv6-Adresse; {@code null}, wenn unlesbar. */
    private static int[] parseIpv6(String value) {
        int doubleColon = value.indexOf("::");
        int[] groups = new int[8];
        if (doubleColon < 0) {
            int[] parsed = parseGroups(value);
            return parsed != null && parsed.length == 8 ? parsed : null;
        }
        int[] head = parseGroups(value.substring(0, doubleColon));
        int[] tail = parseGroups(value.substring(doubleColon + 2));
        if (head == null || tail == null) {
            return null;
        }
        int missing = 8 - head.length - tail.length;
        if (missing < 1) {
            return null; // "::" muss mindestens eine Gruppe ersetzen
        }
        System.arraycopy(head, 0, groups, 0, head.length);
        System.arraycopy(tail, 0, groups, 8 - tail.length, tail.length);
        return groups;
    }

    /** Die Gruppen eines IPv6-Teils (ohne „::"); leer bei leerem Text. */
    private static int[] parseGroups(String part) {
        if (part.isEmpty()) {
            return new int[0];
        }
        String[] parts = part.split(":", -1);
        int[] groups = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            String group = parts[index];
            if (group.isEmpty() || group.length() > 4) {
                return null;
            }
            try {
                groups[index] = Integer.parseInt(group, 16);
            } catch (NumberFormatException unreadable) {
                return null;
            }
        }
        return groups;
    }
}
