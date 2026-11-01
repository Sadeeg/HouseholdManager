-- Benachrichtigung: freigeschaltete Telegram-Chats und Zustellvermerke.

-- Der Freischalt-Code eines Haushalts. Unveränderlich und dauerhaft gültig:
-- er entsteht beim ersten Anfordern und wird danach nur noch gelesen.
CREATE TABLE telegram_activation_code (
    household_id    BIGINT       PRIMARY KEY REFERENCES household(id) ON DELETE CASCADE,
    code            VARCHAR(32)  NOT NULL UNIQUE,
    -- Die Person, die den Code angefordert hat. Sie ist die freischaltende
    -- Person der Verbindungen, die mit diesem Code entstehen — der Bot kennt
    -- keine Person, nur den Code.
    activated_by    VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL
);

COMMENT ON TABLE telegram_activation_code IS 'Freischalt-Code je Haushalt; erzeugt beim ersten Anfordern, danach unveränderlich.';
COMMENT ON COLUMN telegram_activation_code.code IS 'Der Code: Ziffern, nicht vorhersagbar, eindeutig.';
COMMENT ON COLUMN telegram_activation_code.activated_by IS 'Die Person, die den Code angefordert hat — freischaltende Person der Verbindung.';

-- Die freigeschalteten Chats eines Haushalts.
CREATE TABLE telegram_chat (
    id              BIGSERIAL    PRIMARY KEY,
    household_id    BIGINT       NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    -- Die Chat-Kennung bei Telegram — der Zugriffsweg auf den Chat.
    chat_id         VARCHAR(100) NOT NULL,
    -- Snapshot von Telegram: der Anzeigename und die Art des Chats.
    chat_name       VARCHAR(200) NOT NULL,
    chat_kind       VARCHAR(20)  NOT NULL,
    -- Die freischaltende Person: nur sie darf die Verbindung trennen.
    activated_by    VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    -- Ein Chat wird für einen Haushalt nur einmal freigeschaltet.
    UNIQUE (household_id, chat_id)
);

COMMENT ON TABLE telegram_chat IS 'Ein für einen Haushalt freigeschalteter Telegram-Chat (Gruppe oder Einzelchat).';
COMMENT ON COLUMN telegram_chat.chat_id IS 'Chat-Kennung bei Telegram; der Zugriffsweg auf den Chat.';
COMMENT ON COLUMN telegram_chat.chat_name IS 'Anzeigename des Chats, wie von Telegram geliefert (Snapshot).';
COMMENT ON COLUMN telegram_chat.chat_kind IS 'Chat-Art: GROUP (Gruppe) oder PRIVATE (Einzelchat).';
COMMENT ON COLUMN telegram_chat.activated_by IS 'Die freischaltende Person; nur sie darf die Verbindung trennen.';

-- Deckt die Listenabfrage der Anzeige (je Haushalt, sortiert nach Name).
CREATE INDEX telegram_chat_idx ON telegram_chat (household_id, chat_name);

-- Der Zustellvermerk einer Nachricht: je Nachricht und Ziel-Chat einer.
CREATE TABLE notification_delivery (
    id              BIGSERIAL    PRIMARY KEY,
    household_id    BIGINT       NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    chat_id         VARCHAR(100) NOT NULL,
    content         VARCHAR(4000) NOT NULL,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL,
    last_error      VARCHAR(1000),
    last_attempt_at TIMESTAMPTZ,
    completed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL
);

COMMENT ON TABLE notification_delivery IS 'Zustellvermerk einer Benachrichtigung: Ziel, Inhalt, Versuchszählung und letzter Zustand.';
COMMENT ON COLUMN notification_delivery.attempt_count IS 'Wie oft versucht wurde; höchstens drei Versuche je Nachricht.';
COMMENT ON COLUMN notification_delivery.status IS 'PENDING, DELIVERED oder FAILED.';
COMMENT ON COLUMN notification_delivery.completed IS 'TRUE, wenn kein weiterer Versuch erfolgt (Zustellung gelungen oder drei Fehlversuche).';

-- Deckt den stündlichen Wiederholungslauf (offene Vermerke).
CREATE INDEX notification_delivery_retry_idx ON notification_delivery (completed, status, attempt_count);
