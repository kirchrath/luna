ALTER TABLE inbound_event
    ADD COLUMN status   TEXT    NOT NULL DEFAULT 'RECEIVED',
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX inbound_event_status_created_date_idx ON inbound_event (status, created_date);

CREATE TABLE outbound_event
(
    id                 UUID PRIMARY KEY,
    inbound_event_id   UUID        NOT NULL REFERENCES inbound_event (id),
    type               TEXT        NOT NULL,
    status             TEXT        NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);

CREATE INDEX outbound_event_inbound_event_id_idx ON outbound_event (inbound_event_id);
