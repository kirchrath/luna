CREATE TABLE core_note_projection
(
    id           UUID PRIMARY KEY,
    core_note_id UUID   NOT NULL UNIQUE,
    version      BIGINT NOT NULL
);

CREATE TABLE note
(
    id                 UUID PRIMARY KEY,
    source             TEXT        NOT NULL,
    source_id          UUID,
    title              TEXT        NOT NULL,
    summary            TEXT        NOT NULL,
    status             TEXT        NOT NULL,
    archived_date      TIMESTAMPTZ,
    edited             BOOLEAN     NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL,
    UNIQUE (source, source_id)
);

CREATE INDEX note_status_created_date_idx ON note (status, created_date);
