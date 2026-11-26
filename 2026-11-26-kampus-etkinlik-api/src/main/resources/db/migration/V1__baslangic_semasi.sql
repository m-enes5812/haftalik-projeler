CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('STUDENT', 'CLUB', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE event (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(160)  NOT NULL,
    description  VARCHAR(4000),
    location     VARCHAR(200)  NOT NULL,
    starts_at    TIMESTAMPTZ   NOT NULL,
    ends_at      TIMESTAMPTZ   NOT NULL,
    capacity     INTEGER       NOT NULL CHECK (capacity > 0),
    status       VARCHAR(16)   NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    organizer_id BIGINT        NOT NULL REFERENCES app_user (id),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT event_time_order CHECK (ends_at > starts_at)
);

CREATE INDEX idx_event_status_starts ON event (status, starts_at);

CREATE TABLE ticket (
    id             BIGSERIAL PRIMARY KEY,
    event_id       BIGINT      NOT NULL REFERENCES event (id),
    user_id        BIGINT      NOT NULL REFERENCES app_user (id),
    code           VARCHAR(64) NOT NULL UNIQUE,
    status         VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'USED', 'CANCELLED')),
    issued_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_in_at  TIMESTAMPTZ
);

-- Bir öğrencinin aynı etkinlik için en fazla bir geçerli (iptal edilmemiş) bileti olabilir.
CREATE UNIQUE INDEX uq_ticket_event_user_live ON ticket (event_id, user_id) WHERE status <> 'CANCELLED';
CREATE INDEX idx_ticket_user ON ticket (user_id);
