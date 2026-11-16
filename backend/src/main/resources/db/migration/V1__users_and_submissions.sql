CREATE TABLE brokerage (
    id   uuid PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE app_user (
    id            uuid PRIMARY KEY,
    email         text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    full_name     text NOT NULL,
    role          text NOT NULL CHECK (role IN ('BROKER', 'UNDERWRITER')),
    brokerage_id  uuid REFERENCES brokerage (id),
    -- brokers always work for a brokerage, underwriters work for us
    CHECK ((role = 'BROKER') = (brokerage_id IS NOT NULL))
);

CREATE SEQUENCE submission_ref_seq;

CREATE TABLE submission (
    id                  uuid PRIMARY KEY,
    reference           text NOT NULL UNIQUE,
    brokerage_id        uuid NOT NULL REFERENCES brokerage (id),
    broker_id           uuid NOT NULL REFERENCES app_user (id),
    underwriter_id      uuid REFERENCES app_user (id),
    status              text NOT NULL,
    line_of_business    text NOT NULL,
    insured_name        text NOT NULL,
    insured_country     text,
    insured_industry    text,
    annual_revenue      numeric(16, 2),
    sum_insured         numeric(16, 2),
    currency            varchar(3) NOT NULL DEFAULT 'EUR',
    coverage_start      date,
    coverage_end        date,
    target_premium      numeric(14, 2),
    risk_description    text,
    quoted_premium      numeric(14, 2),
    quote_valid_until   date,
    quote_terms         text,
    policy_number       text UNIQUE,
    closed_reason       text,
    created_at          timestamptz NOT NULL,
    updated_at          timestamptz NOT NULL,
    status_changed_at   timestamptz NOT NULL,
    version             bigint NOT NULL DEFAULT 0
);

CREATE INDEX submission_brokerage_status_idx ON submission (brokerage_id, status);
CREATE INDEX submission_status_idx ON submission (status);

-- every status change, so the broker can see where the time went
CREATE TABLE submission_event (
    id            uuid PRIMARY KEY,
    submission_id uuid NOT NULL REFERENCES submission (id),
    from_status   text,
    to_status     text NOT NULL,
    actor_id      uuid REFERENCES app_user (id),
    note          text,
    created_at    timestamptz NOT NULL
);

CREATE INDEX submission_event_submission_idx ON submission_event (submission_id, created_at);

CREATE TABLE submission_message (
    id            uuid PRIMARY KEY,
    submission_id uuid NOT NULL REFERENCES submission (id),
    author_id     uuid NOT NULL REFERENCES app_user (id),
    body          text NOT NULL,
    created_at    timestamptz NOT NULL
);

CREATE INDEX submission_message_submission_idx ON submission_message (submission_id, created_at);
