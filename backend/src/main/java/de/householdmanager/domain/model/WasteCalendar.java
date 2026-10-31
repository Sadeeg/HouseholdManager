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
     * R13 und AC20: der Stand ist veraltet, wenn ein Versuch nach dem letzten
     * erfolgreichen Lauf liegt. Ohne erfolgreichen Lauf gibt es keinen Stand,
     * der veraltet sein könnte — die Anzeige nennt dann „Noch keine
     * Synchronisation“ (R16).
     */
    public boolean isStale() {
        return lastSyncedAt != null && lastAttemptAt != null && lastAttemptAt.isAfter(lastSyncedAt);
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
        return normalized;
    }
}
