-- Serverseitige Merkung des zuletzt gewählten Haushalts pro Person.

CREATE TABLE workspace_selection (
    user_id      TEXT        PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE workspace_selection IS 'Zuletzt gewählter Haushalt einer Person (Workspace-Auswahl, ADR 0004).';
