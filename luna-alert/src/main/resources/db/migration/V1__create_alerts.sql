CREATE TABLE core_alert_projection
(
    id            UUID PRIMARY KEY,
    core_alert_id UUID   NOT NULL UNIQUE,
    version       BIGINT NOT NULL
);

CREATE TABLE alert
(
    id                 UUID PRIMARY KEY,
    source             TEXT        NOT NULL,
    source_id          UUID,
    title              TEXT        NOT NULL,
    message            TEXT        NOT NULL,
    severity           TEXT        NOT NULL,
    severity_rank      INTEGER     NOT NULL,
    affected_system    TEXT,
    status             TEXT        NOT NULL,
    acknowledged_date  TIMESTAMPTZ,
    resolved_date      TIMESTAMPTZ,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL,
    UNIQUE (source, source_id)
);

CREATE INDEX alert_status_severity_rank_idx ON alert (status, severity_rank, created_date);
