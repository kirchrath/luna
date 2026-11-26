CREATE TABLE mailbox
(
    id                 UUID PRIMARY KEY,
    account            TEXT        NOT NULL UNIQUE,
    address            TEXT        NOT NULL,
    folder             TEXT        NOT NULL,
    uid_validity       BIGINT,
    last_uid           BIGINT,
    status             TEXT        NOT NULL,
    failures           INTEGER     NOT NULL,
    last_error         TEXT,
    last_polled_date   TIMESTAMPTZ,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL
);

CREATE TABLE email
(
    id                 UUID PRIMARY KEY,
    mailbox_id         UUID        NOT NULL REFERENCES mailbox (id),
    uid_validity       BIGINT      NOT NULL,
    uid                BIGINT      NOT NULL,
    message_id         TEXT,
    sender             TEXT,
    recipients         TEXT[]      NOT NULL,
    subject            TEXT,
    sent_date          TIMESTAMPTZ,
    received_date      TIMESTAMPTZ NOT NULL,
    text               TEXT        NOT NULL,
    text_truncated     BOOLEAN     NOT NULL,
    created_date       TIMESTAMPTZ NOT NULL,
    last_modified_date TIMESTAMPTZ NOT NULL,
    version            BIGINT      NOT NULL,
    UNIQUE (mailbox_id, uid_validity, uid)
);

CREATE TABLE email_attachment
(
    email              UUID    NOT NULL REFERENCES email (id),
    email_key          INTEGER NOT NULL,
    name               TEXT,
    content_type       TEXT    NOT NULL,
    size               BIGINT,
    PRIMARY KEY (email, email_key)
);
