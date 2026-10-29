CREATE TABLE household (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE household IS 'Ein Haushalt, in dem Mitglieder Aufgaben und Ausgaben verwalten.';
