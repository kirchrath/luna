CREATE TABLE core_task_projection
(
    id                 UUID PRIMARY KEY,
    core_task_id       UUID        NOT NULL UNIQUE,
    version            BIGINT      NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL
);

CREATE TABLE task
(
    id                 UUID PRIMARY KEY,
    source             TEXT        NOT NULL,
    source_id          UUID,
    title              TEXT        NOT NULL,
    description        TEXT,
    due_date           TIMESTAMPTZ,
    priority           TEXT        NOT NULL,
    status             TEXT        NOT NULL,
    completed_date     TIMESTAMPTZ,
    edited             BOOLEAN     NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL,
    UNIQUE (source, source_id)
);

CREATE INDEX task_status_due_date_idx ON task (status, due_date);
