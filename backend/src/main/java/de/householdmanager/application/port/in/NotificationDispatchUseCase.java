package de.householdmanager.application.port.in;

/**
 * Eingehender Port: die Benachrichtigung über Mülltermine.
 *
 * <p>Implementierung:
 * {@code application.usecase.NotificationDispatchUseCaseImpl}.
 * Der Scheduler-Adapter ({@code adapters.inbound.scheduler.NotificationDispatchJob})
 * löst genau diesen Port aus — der Adapter enthält keine Datums- und keine
 * Wochentagslogik.
 *
 * <p>Der Kontext liest die Termine über den In-Port des Müllkalender-Kontexts
 * ({@link WasteCollectionUseCase}) und greift nicht auf dessen Tabellen zu
 * (ADR 0007).
 */
public interface NotificationDispatchUseCase {

    /**
     * Der Lauf um 18:00 Uhr: die Wochenübersicht der kommenden Woche und
     * die Erinnerung für den Folgetag (AC17–AC23).
     *
     * <p>Am Sonntag kommen <strong>beide</strong> Nachrichten: die
     * Wochenübersicht der Woche, die am Montag beginnt, und die Erinnerung
     * für den Montag (AC20).
     *
     * <p>Ein Haushalt ohne freigeschalteten Chat erhält keine Benachrichtigung
     * (AC25). Ein fehlgeschlagener Chat stoppt die übrigen Chats desselben
     * Haushalts nicht (AC29).
     */
    void dispatch();

    /**
     * Der stündliche Wiederholungslauf: alle Zustellvermerke, die einen
     * weiteren Versuch brauchen, werden erneut versucht (AC26). Nach dem
     * dritten Fehlversuch wird kein weiterer Versuch unternommen (AC27), die
     * Verbindung des Chats bleibt bestehen (AC28).
     */
    void retryFailed();
}
