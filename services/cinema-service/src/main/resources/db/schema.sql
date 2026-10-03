-- Reference schema for an empty, service-owned database. Never executed at application startup.
-- Update this snapshot when changing the managed database; do not replay on an existing database.

-- Cinema schema, local to cinema_cinema.
-- All cross-service UUIDs are logical references, never foreign keys.

CREATE TABLE cinemas (
  id uuid NOT NULL,
  name text NOT NULL,
  address text NOT NULL,
  city text NOT NULL,
  timezone text NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_cinemas PRIMARY KEY (id)
);

CREATE TABLE auditoriums (
  id uuid NOT NULL,
  cinema_id uuid NOT NULL,
  name text NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_auditoriums PRIMARY KEY (id)
);

CREATE TABLE seats (
  id uuid NOT NULL,
  auditorium_id uuid NOT NULL,
  row_label varchar(12) NOT NULL,
  seat_number integer NOT NULL,
  seat_type varchar(24) NOT NULL,
  is_active boolean NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_seats PRIMARY KEY (id)
);

CREATE TABLE showtimes (
  id uuid NOT NULL,
  auditorium_id uuid NOT NULL,
  movie_id uuid NOT NULL,
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NOT NULL,
  sales_close_at timestamptz NOT NULL,
  price_minor bigint NOT NULL,
  currency varchar(3) NOT NULL,
  status varchar(16) NOT NULL,
  snapshot_version bigint NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_showtimes PRIMARY KEY (id)
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

CREATE INDEX ix_cinemas_city ON cinemas (city);
CREATE UNIQUE INDEX uq_auditoriums_cinema_id_name ON auditoriums (cinema_id, name);
CREATE UNIQUE INDEX uq_seats_auditorium_id_row_label_seat_number ON seats (auditorium_id, row_label, seat_number);
CREATE INDEX ix_showtimes_auditorium_id_starts_at ON showtimes (auditorium_id, starts_at);
CREATE INDEX ix_showtimes_movie_id_starts_at ON showtimes (movie_id, starts_at);
CREATE INDEX ix_showtimes_status_starts_at ON showtimes (status, starts_at);
CREATE INDEX ix_outbox_events_next_attempt_at_created_at ON outbox_events (next_attempt_at, created_at) WHERE published_at IS NULL;
ALTER TABLE auditoriums ADD CONSTRAINT fk_auditoriums_cinema_id FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE RESTRICT;

ALTER TABLE seats ADD CONSTRAINT fk_seats_auditorium_id FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id) ON DELETE RESTRICT;

ALTER TABLE showtimes ADD CONSTRAINT fk_showtimes_auditorium_id FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id) ON DELETE RESTRICT;


CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE seats ADD CONSTRAINT ck_seats_number CHECK (seat_number > 0);
ALTER TABLE showtimes ADD CONSTRAINT ck_showtimes_time CHECK (starts_at < ends_at AND sales_close_at <= starts_at);
ALTER TABLE showtimes ADD CONSTRAINT ck_showtimes_price CHECK (price_minor >= 0);
ALTER TABLE showtimes ADD CONSTRAINT ck_showtimes_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE showtimes ADD CONSTRAINT ck_showtimes_version CHECK (snapshot_version > 0);
ALTER TABLE showtimes ADD CONSTRAINT ck_showtimes_status CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED'));
ALTER TABLE showtimes ADD CONSTRAINT ex_showtimes_auditorium_overlap
  EXCLUDE USING gist (auditorium_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&)
  WHERE (status = 'PUBLISHED');

ALTER TABLE outbox_events ADD CONSTRAINT ck_outbox_events_attempts CHECK (publish_attempts >= 0 AND event_version > 0);

-- Cinema-scoped manager assignments. Auth user IDs are logical references only.
CREATE TABLE cinema_managers (
    cinema_id uuid NOT NULL,
    user_id uuid NOT NULL,
    assigned_at timestamptz NOT NULL,
    CONSTRAINT pk_cinema_managers PRIMARY KEY (cinema_id, user_id),
    CONSTRAINT fk_cinema_managers_cinema_id FOREIGN KEY (cinema_id)
        REFERENCES cinemas (id) ON DELETE RESTRICT
);

CREATE INDEX ix_cinema_managers_user_id ON cinema_managers (user_id);
