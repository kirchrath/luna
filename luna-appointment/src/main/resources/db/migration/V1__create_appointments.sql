CREATE TABLE core_appointment_projection
(
    id                  UUID PRIMARY KEY,
    core_appointment_id UUID   NOT NULL UNIQUE,
    version             BIGINT NOT NULL
);

CREATE TABLE appointment
(
    id                 UUID PRIMARY KEY,
    source             TEXT        NOT NULL,
    source_id          UUID,
    title              TEXT        NOT NULL,
    start              TIMESTAMPTZ NOT NULL,
    "end"              TIMESTAMPTZ,
    all_day            BOOLEAN     NOT NULL,
    location           TEXT,
    description        TEXT,
    effective_end      TIMESTAMPTZ NOT NULL,
    status             TEXT        NOT NULL,
    cancelled_date     TIMESTAMPTZ,
    edited             BOOLEAN     NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL,
    UNIQUE (source, source_id)
);

CREATE INDEX appointment_effective_end_idx ON appointment (effective_end, start);
CREATE INDEX appointment_start_idx ON appointment (start);
