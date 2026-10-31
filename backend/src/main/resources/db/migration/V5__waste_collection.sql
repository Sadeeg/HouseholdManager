-- Müllkalender: ICS-Quelle je Haushalt, Termine und Synchronisationsstand.

ALTER TABLE household ADD COLUMN waste_calendar_url VARCHAR(500);
COMMENT ON COLUMN household.waste_calendar_url IS 'ICS-Adresse des Entsorgers; NULL, wenn kein Müllkalender hinterlegt ist.';

CREATE TABLE waste_collection (
    id              BIGSERIAL    PRIMARY KEY,
    household_id    BIGINT       NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    collection_date DATE         NOT NULL,
    waste_type      VARCHAR(200) NOT NULL,
    source_uid      VARCHAR(255),
    UNIQUE (household_id, collection_date, waste_type)
);

COMMENT ON TABLE waste_collection IS 'Ein Mülltermin eines Haushalts aus dem ICS-Feed des Entsorgers.';
COMMENT ON COLUMN waste_collection.waste_type IS 'Tonnenart, unverändert aus der Quelle (SUMMARY).';
COMMENT ON COLUMN waste_collection.source_uid IS 'UID des Ereignisses in der Quelle; NULL, wenn die Quelle keine UID liefert.';

-- Deckt die Listenabfrage der Anzeige (hausweit, sortiert nach Datum).
CREATE INDEX waste_collection_idx ON waste_collection (household_id, collection_date);

CREATE TABLE waste_sync_state (
    household_id    BIGINT      PRIMARY KEY REFERENCES household(id) ON DELETE CASCADE,
    last_synced_at  TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ
);

COMMENT ON TABLE waste_sync_state IS 'Stand der Synchronisation je Haushalt: letzter erfolgreicher Lauf und letzter Versuch.';
