-- Haushaltsleitung und Mitgliedschaft.
--
-- created_by wird ohne DEFAULT ergänzt: zulässig, weil die Dev-DB frisch
-- aufgebaut wird (Entscheidung A14 im Issue #1) — die Tabelle ist leer,
-- bevor die Spalte NOT NULL wird.

ALTER TABLE household ADD COLUMN created_by TEXT NOT NULL;
COMMENT ON COLUMN household.created_by IS 'Kennung der Person, die den Haushalt angelegt hat (Haushaltsleitung).';

CREATE TABLE household_member (
    id           BIGSERIAL   PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    user_id      TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (household_id, user_id)
);

COMMENT ON TABLE household_member IS 'Ernannte Zugehörigkeit einer Person zu einem Haushalt.';
