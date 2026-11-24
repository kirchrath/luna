CREATE TABLE inbound_event
(
    id                 UUID PRIMARY KEY,
    source             TEXT        NOT NULL,
    data               JSONB       NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);
