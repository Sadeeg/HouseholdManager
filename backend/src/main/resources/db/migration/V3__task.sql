-- Aufgaben-Tafel: Aufgabe, Label, Zuordnung Aufgabe↔Label.

CREATE TABLE task (
    id               BIGSERIAL   PRIMARY KEY,
    household_id     BIGINT      NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    content          TEXT,
    status           VARCHAR(10) NOT NULL CHECK (status IN ('TODO', 'DOING', 'DONE')),
    assignee_user_id TEXT,
    due_date         DATE,
    position         INTEGER     NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE task IS 'Eine Aufgabe auf der Tafel eines Haushalts (TODO/DOING/DONE).';
COMMENT ON COLUMN task.position IS 'Reihenfolge innerhalb der Spalte, lückenlos 0..n je (household_id, status).';

-- Deckt das Laden der Tafel (household + status, sortiert nach position).
CREATE INDEX task_board_idx ON task (household_id, status, position);

CREATE TABLE label (
    id              BIGSERIAL    PRIMARY KEY,
    household_id    BIGINT       NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    name            VARCHAR(60)  NOT NULL,
    normalized_name VARCHAR(60)  NOT NULL,
    UNIQUE (household_id, normalized_name)
);

COMMENT ON TABLE label IS 'Freies Label eines Haushalts; Identität ist die normalisierte Schreibweise.';
COMMENT ON COLUMN label.name IS 'Zuerst gespeicherte Schreibweise (wird angezeigt).';
COMMENT ON COLUMN label.normalized_name IS 'lower-case und getrimmt — trägt Farbe und Eindeutigkeit.';

CREATE TABLE task_label (
    task_id  BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES label(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);

COMMENT ON TABLE task_label IS 'Zuordnung von Labels zu Aufgaben.';
