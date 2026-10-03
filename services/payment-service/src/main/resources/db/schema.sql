-- Reference schema for an empty, service-owned database. Never executed at application startup.
-- Update this snapshot when changing the managed database; do not replay on an existing database.

-- Payment schema, local to cinema_payment.
-- All cross-service UUIDs are logical references, never foreign keys.

CREATE TABLE payments (
  id uuid NOT NULL,
  booking_id uuid NOT NULL UNIQUE,
  reservation_id uuid NOT NULL,
  user_id uuid NOT NULL,
  amount_minor bigint NOT NULL,
  currency varchar(3) NOT NULL,
  request_key varchar(160) NOT NULL,
  request_hash bytea NOT NULL,
  status varchar(24) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_payments PRIMARY KEY (id)
);

CREATE TABLE payment_attempts (
  id uuid NOT NULL,
  payment_id uuid NOT NULL,
  provider varchar(40) NOT NULL,
  merchant_reference varchar(160) NOT NULL,
  provider_transaction_id varchar(160),
  checkout_url text,
  status varchar(24) NOT NULL,
  failure_code varchar(80),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_payment_attempts PRIMARY KEY (id)
);

CREATE TABLE provider_events (
  id uuid NOT NULL,
  payment_id uuid NOT NULL,
  provider varchar(40) NOT NULL,
  event_identity varchar(160) NOT NULL,
  provider_event_id varchar(160),
  payload_digest bytea,
  result_code varchar(80) NOT NULL,
  received_at timestamptz NOT NULL,
  CONSTRAINT pk_provider_events PRIMARY KEY (id)
);

CREATE TABLE refunds (
  id uuid NOT NULL,
  payment_id uuid NOT NULL UNIQUE,
  refund_request_id uuid NOT NULL UNIQUE,
  amount_minor bigint NOT NULL,
  currency varchar(3) NOT NULL,
  request_key varchar(160) NOT NULL,
  request_hash bytea NOT NULL,
  provider varchar(40) NOT NULL,
  merchant_reference varchar(160) NOT NULL,
  provider_refund_id varchar(160),
  status varchar(24) NOT NULL,
  failure_code varchar(80),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_refunds PRIMARY KEY (id)
);

CREATE TABLE outbox_events (
  event_id uuid NOT NULL,
  event_type varchar(80) NOT NULL,
  event_version integer NOT NULL,
  aggregate_id uuid NOT NULL,
  occurred_at timestamptz NOT NULL,
  trace_id varchar(128) NOT NULL,
  payload jsonb NOT NULL,
  publish_attempts integer NOT NULL,
  next_attempt_at timestamptz NOT NULL,
  published_at timestamptz,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_outbox_events PRIMARY KEY (event_id)
);

CREATE TABLE processed_events (
  consumer_name varchar(80) NOT NULL,
  event_id uuid NOT NULL,
  processed_at timestamptz NOT NULL,
  CONSTRAINT pk_processed_events PRIMARY KEY (consumer_name, event_id)
);

CREATE UNIQUE INDEX uq_payments_user_id_request_key ON payments (user_id, request_key);
CREATE UNIQUE INDEX uq_payment_attempts_provider_merchant_reference ON payment_attempts (provider, merchant_reference);
CREATE UNIQUE INDEX uq_payment_attempts_provider_provider_transaction_id ON payment_attempts (provider, provider_transaction_id);
CREATE INDEX ix_payment_attempts_payment_id_created_at ON payment_attempts (payment_id, created_at);
CREATE UNIQUE INDEX uq_provider_events_provider_event_identity ON provider_events (provider, event_identity);
CREATE INDEX ix_provider_events_payment_id_received_at ON provider_events (payment_id, received_at);
CREATE UNIQUE INDEX uq_refunds_provider_merchant_reference ON refunds (provider, merchant_reference);
CREATE UNIQUE INDEX uq_refunds_provider_provider_refund_id ON refunds (provider, provider_refund_id);
CREATE UNIQUE INDEX uq_refunds_provider_request_key ON refunds (provider, request_key);
CREATE INDEX ix_outbox_events_next_attempt_at_created_at ON outbox_events (next_attempt_at, created_at) WHERE published_at IS NULL;
CREATE INDEX ix_processed_events_processed_at ON processed_events (processed_at);
ALTER TABLE payment_attempts ADD CONSTRAINT fk_payment_attempts_payment_id FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT;

ALTER TABLE provider_events ADD CONSTRAINT fk_provider_events_payment_id FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT;

ALTER TABLE refunds ADD CONSTRAINT fk_refunds_payment_id FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT;


ALTER TABLE payments ADD CONSTRAINT ck_payments_amount CHECK (amount_minor >= 0);
ALTER TABLE payments ADD CONSTRAINT ck_payments_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE payments ADD CONSTRAINT ck_payments_status CHECK (status IN ('REQUESTED','PROVIDER_PENDING','SUCCEEDED','FAILED','REFUND_PENDING','REFUNDED','REFUND_FAILED'));
ALTER TABLE payment_attempts ADD CONSTRAINT ck_payment_attempts_status CHECK (status IN ('REQUESTED','PROVIDER_PENDING','SUCCEEDED','FAILED','UNKNOWN'));
ALTER TABLE provider_events ADD CONSTRAINT ck_provider_events_identity CHECK (btrim(event_identity) <> '');
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_amount CHECK (amount_minor >= 0);
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_status CHECK (status IN ('REQUESTED','PROVIDER_PENDING','COMPLETED','FAILED'));

ALTER TABLE outbox_events ADD CONSTRAINT ck_outbox_events_attempts CHECK (publish_attempts >= 0 AND event_version > 0);
