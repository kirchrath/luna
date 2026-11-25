CREATE TABLE task_element
(
    id                 UUID PRIMARY KEY,
    outbound_event_id  UUID        NOT NULL UNIQUE REFERENCES outbound_event (id),
    title              TEXT        NOT NULL,
    description        TEXT,
    due_date           TIMESTAMPTZ,
    priority           TEXT        NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);

CREATE TABLE alert_element
(
    id                 UUID PRIMARY KEY,
    outbound_event_id  UUID        NOT NULL UNIQUE REFERENCES outbound_event (id),
    title              TEXT        NOT NULL,
    message            TEXT        NOT NULL,
    severity           TEXT        NOT NULL,
    affected_system    TEXT,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);

CREATE TABLE appointment_element
(
    id                 UUID PRIMARY KEY,
    outbound_event_id  UUID        NOT NULL UNIQUE REFERENCES outbound_event (id),
    title              TEXT        NOT NULL,
    "start"            TIMESTAMPTZ NOT NULL,
    "end"              TIMESTAMPTZ,
    all_day            BOOLEAN     NOT NULL,
    location           TEXT,
    description        TEXT,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);

CREATE TABLE invoice_element
(
    id                 UUID PRIMARY KEY,
    outbound_event_id  UUID        NOT NULL UNIQUE REFERENCES outbound_event (id),
    issuer             TEXT        NOT NULL,
    invoice_number     TEXT,
    amount             NUMERIC     NOT NULL,
    currency           TEXT        NOT NULL,
    due_date           TIMESTAMPTZ,
    description        TEXT,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);

CREATE TABLE contract_element
(
    id                    UUID PRIMARY KEY,
    outbound_event_id     UUID        NOT NULL UNIQUE REFERENCES outbound_event (id),
    counterparty          TEXT        NOT NULL,
    title                 TEXT        NOT NULL,
    start_date            TIMESTAMPTZ,
    end_date              TIMESTAMPTZ,
    cancellation_deadline TIMESTAMPTZ,
    description           TEXT,
    created_date          TIMESTAMPTZ NOT NULL,
    last_modified_date    TIMESTAMPTZ NOT NULL,
    version               BIGINT      NOT NULL
);

CREATE TABLE note_element
(
    id                 UUID PRIMARY KEY,
    outbound_event_id  UUID        NOT NULL UNIQUE REFERENCES outbound_event (id),
    title              TEXT        NOT NULL,
    summary            TEXT        NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);
