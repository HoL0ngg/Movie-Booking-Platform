-- Reference schema for an empty, service-owned database. Never executed at application startup.
-- Update this snapshot when changing the managed database; do not replay on an existing database.

-- Booking schema, local to cinema_booking.
-- All cross-service UUIDs are logical references, never foreign keys.

CREATE TABLE showtime_snapshots (
  showtime_id uuid NOT NULL,
  cinema_id uuid NOT NULL,
  auditorium_id uuid NOT NULL,
  movie_id uuid NOT NULL,
  starts_at timestamptz NOT NULL,
  sales_close_at timestamptz NOT NULL,
  currency varchar(3) NOT NULL,
  status varchar(16) NOT NULL,
  snapshot_version bigint NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_showtime_snapshots PRIMARY KEY (showtime_id)
);

CREATE TABLE showtime_seats (
  showtime_id uuid NOT NULL,
  seat_id uuid NOT NULL,
  seat_label varchar(32) NOT NULL,
  seat_type varchar(24) NOT NULL,
  price_minor bigint NOT NULL,
  status varchar(20) NOT NULL,
  reservation_id uuid,
  booking_id uuid,
  hold_expires_at timestamptz,
  version bigint NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_showtime_seats PRIMARY KEY (showtime_id, seat_id)
);

CREATE TABLE reservations (
  id uuid NOT NULL,
  showtime_id uuid NOT NULL,
  user_id uuid NOT NULL,
  status varchar(20) NOT NULL,
  hold_expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_reservations PRIMARY KEY (id)
);

CREATE TABLE reservation_seats (
  reservation_id uuid NOT NULL,
  showtime_id uuid NOT NULL,
  seat_id uuid NOT NULL,
  price_minor bigint NOT NULL,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_reservation_seats PRIMARY KEY (reservation_id, seat_id)
);

CREATE TABLE bookings (
  id uuid NOT NULL,
  reservation_id uuid NOT NULL UNIQUE,
  showtime_id uuid NOT NULL,
  user_id uuid NOT NULL,
  payment_id uuid NOT NULL UNIQUE,
  amount_minor bigint NOT NULL,
  currency varchar(3) NOT NULL,
  status varchar(20) NOT NULL,
  confirmed_at timestamptz,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_bookings PRIMARY KEY (id)
);

CREATE TABLE booking_items (
  id uuid NOT NULL,
  booking_id uuid NOT NULL,
  showtime_id uuid NOT NULL,
  seat_id uuid NOT NULL,
  price_minor bigint NOT NULL,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_booking_items PRIMARY KEY (id)
);

CREATE TABLE idempotency_keys (
  actor_id uuid NOT NULL,
  operation varchar(32) NOT NULL,
  idempotency_key varchar(160) NOT NULL,
  request_hash bytea NOT NULL,
  response_status integer,
  response_body jsonb,
  created_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  CONSTRAINT pk_idempotency_keys PRIMARY KEY (actor_id, operation, idempotency_key)
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

CREATE INDEX ix_showtime_seats_hold_expires_at_showtime_id_seat_id ON showtime_seats (hold_expires_at, showtime_id, seat_id) WHERE status IN ('HELD','PAYMENT_PENDING');
CREATE INDEX ix_showtime_seats_reservation_id ON showtime_seats (reservation_id);
CREATE UNIQUE INDEX uq_reservations_id_showtime_id ON reservations (id, showtime_id);
CREATE UNIQUE INDEX uq_reservations_id_showtime_id_user_id ON reservations (id, showtime_id, user_id);
CREATE INDEX ix_reservations_status_hold_expires_at ON reservations (status, hold_expires_at);
CREATE INDEX ix_reservations_user_id_created_at ON reservations (user_id, created_at);
CREATE INDEX ix_reservation_seats_reservation_id_showtime_id ON reservation_seats (reservation_id, showtime_id);
CREATE INDEX ix_reservation_seats_showtime_id_seat_id ON reservation_seats (showtime_id, seat_id);
CREATE UNIQUE INDEX uq_bookings_id_showtime_id ON bookings (id, showtime_id);
CREATE INDEX ix_bookings_user_id_created_at ON bookings (user_id, created_at);
CREATE UNIQUE INDEX uq_booking_items_booking_id_seat_id ON booking_items (booking_id, seat_id);
CREATE UNIQUE INDEX uq_booking_items_showtime_id_seat_id ON booking_items (showtime_id, seat_id);
CREATE INDEX ix_idempotency_keys_expires_at ON idempotency_keys (expires_at);
CREATE INDEX ix_outbox_events_next_attempt_at_created_at ON outbox_events (next_attempt_at, created_at) WHERE published_at IS NULL;
CREATE INDEX ix_processed_events_processed_at ON processed_events (processed_at);
ALTER TABLE showtime_seats ADD CONSTRAINT fk_showtime_seats_showtime_id FOREIGN KEY (showtime_id) REFERENCES showtime_snapshots (showtime_id) ON DELETE RESTRICT;

ALTER TABLE reservations ADD CONSTRAINT fk_reservations_showtime_id FOREIGN KEY (showtime_id) REFERENCES showtime_snapshots (showtime_id) ON DELETE RESTRICT;

ALTER TABLE showtime_seats ADD CONSTRAINT fk_showtime_seats_reservation_id_showtime_id FOREIGN KEY (reservation_id, showtime_id) REFERENCES reservations (id, showtime_id) ON DELETE RESTRICT;

ALTER TABLE showtime_seats ADD CONSTRAINT fk_showtime_seats_booking_id_showtime_id FOREIGN KEY (booking_id, showtime_id) REFERENCES bookings (id, showtime_id) ON DELETE RESTRICT;

ALTER TABLE reservation_seats ADD CONSTRAINT fk_reservation_seats_reservation_id_showtime_id FOREIGN KEY (reservation_id, showtime_id) REFERENCES reservations (id, showtime_id) ON DELETE RESTRICT;

ALTER TABLE reservation_seats ADD CONSTRAINT fk_reservation_seats_showtime_id_seat_id FOREIGN KEY (showtime_id, seat_id) REFERENCES showtime_seats (showtime_id, seat_id) ON DELETE RESTRICT;

ALTER TABLE bookings ADD CONSTRAINT fk_bookings_reservation_id_showtime_id_user_id FOREIGN KEY (reservation_id, showtime_id, user_id) REFERENCES reservations (id, showtime_id, user_id) ON DELETE RESTRICT;

ALTER TABLE booking_items ADD CONSTRAINT fk_booking_items_booking_id_showtime_id FOREIGN KEY (booking_id, showtime_id) REFERENCES bookings (id, showtime_id) ON DELETE RESTRICT;

ALTER TABLE booking_items ADD CONSTRAINT fk_booking_items_showtime_id_seat_id FOREIGN KEY (showtime_id, seat_id) REFERENCES showtime_seats (showtime_id, seat_id) ON DELETE RESTRICT;


ALTER TABLE showtime_snapshots ADD CONSTRAINT ck_showtime_snapshots_status CHECK (status IN ('PUBLISHED','CANCELLED'));
ALTER TABLE showtime_snapshots ADD CONSTRAINT ck_showtime_snapshots_version CHECK (snapshot_version > 0);
ALTER TABLE showtime_snapshots ADD CONSTRAINT ck_showtime_snapshots_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE showtime_seats ADD CONSTRAINT ck_showtime_seats_price_version CHECK (price_minor >= 0 AND version >= 0);
ALTER TABLE showtime_seats ADD CONSTRAINT ck_showtime_seats_state CHECK (
  (status = 'AVAILABLE' AND reservation_id IS NULL AND booking_id IS NULL AND hold_expires_at IS NULL) OR
  (status = 'HELD' AND reservation_id IS NOT NULL AND booking_id IS NULL AND hold_expires_at IS NOT NULL) OR
  (status = 'PAYMENT_PENDING' AND reservation_id IS NOT NULL AND booking_id IS NOT NULL AND hold_expires_at IS NOT NULL) OR
  (status = 'SOLD' AND reservation_id IS NOT NULL AND booking_id IS NOT NULL AND hold_expires_at IS NULL)
);
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_status CHECK (status IN ('HELD','PAYMENT_PENDING','CONFIRMED','RELEASED','EXPIRED'));
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_expiry CHECK (hold_expires_at > created_at);
ALTER TABLE reservation_seats ADD CONSTRAINT ck_reservation_seats_price CHECK (price_minor >= 0);
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_status CHECK (status IN ('PAYMENT_PENDING','CONFIRMED','CANCELLED','REFUND_PENDING','REFUNDED'));
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_amount CHECK (amount_minor >= 0);
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE booking_items ADD CONSTRAINT ck_booking_items_price CHECK (price_minor >= 0);
ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_expiry CHECK (expires_at > created_at);
ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_response CHECK ((response_status IS NULL) = (response_body IS NULL));

ALTER TABLE outbox_events ADD CONSTRAINT ck_outbox_events_attempts CHECK (publish_attempts >= 0 AND event_version > 0);
