-- Incremental patch for the existing Supabase demo database; never replay schema.sql.
-- Read-only preflight on 2026-11-17: all 24 CHECK constraints below were absent,
-- and existing booking/payment rows violated none of their reference predicates.
-- Run once after owner approval. A failure rolls back the whole patch.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

ALTER TABLE booking.showtime_snapshots ADD CONSTRAINT ck_showtime_snapshots_status CHECK (status IN ('PUBLISHED','CANCELLED'));
ALTER TABLE booking.showtime_snapshots ADD CONSTRAINT ck_showtime_snapshots_version CHECK (snapshot_version > 0);
ALTER TABLE booking.showtime_snapshots ADD CONSTRAINT ck_showtime_snapshots_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE booking.showtime_seats ADD CONSTRAINT ck_showtime_seats_price_version CHECK (price_minor >= 0 AND version >= 0);
ALTER TABLE booking.showtime_seats ADD CONSTRAINT ck_showtime_seats_state CHECK (
  (status = 'AVAILABLE' AND reservation_id IS NULL AND booking_id IS NULL AND hold_expires_at IS NULL) OR
  (status = 'HELD' AND reservation_id IS NOT NULL AND booking_id IS NULL AND hold_expires_at IS NOT NULL) OR
  (status = 'PAYMENT_PENDING' AND reservation_id IS NOT NULL AND booking_id IS NOT NULL AND hold_expires_at IS NOT NULL) OR
  (status = 'SOLD' AND reservation_id IS NOT NULL AND booking_id IS NOT NULL AND hold_expires_at IS NULL)
);
ALTER TABLE booking.reservations ADD CONSTRAINT ck_reservations_status CHECK (status IN ('HELD','PAYMENT_PENDING','CONFIRMED','RELEASED','EXPIRED'));
ALTER TABLE booking.reservations ADD CONSTRAINT ck_reservations_expiry CHECK (hold_expires_at > created_at);
ALTER TABLE booking.reservation_seats ADD CONSTRAINT ck_reservation_seats_price CHECK (price_minor >= 0);
ALTER TABLE booking.bookings ADD CONSTRAINT ck_bookings_status CHECK (status IN ('PAYMENT_PENDING','CONFIRMED','CANCELLED','REFUND_PENDING','REFUNDED'));
ALTER TABLE booking.bookings ADD CONSTRAINT ck_bookings_amount CHECK (amount_minor >= 0);
ALTER TABLE booking.bookings ADD CONSTRAINT ck_bookings_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE booking.booking_items ADD CONSTRAINT ck_booking_items_price CHECK (price_minor >= 0);
ALTER TABLE booking.idempotency_keys ADD CONSTRAINT ck_idempotency_keys_expiry CHECK (expires_at > created_at);
ALTER TABLE booking.idempotency_keys ADD CONSTRAINT ck_idempotency_keys_response CHECK ((response_status IS NULL) = (response_body IS NULL));
ALTER TABLE booking.outbox_events ADD CONSTRAINT ck_outbox_events_attempts CHECK (publish_attempts >= 0 AND event_version > 0);
ALTER TABLE payment.payments ADD CONSTRAINT ck_payments_amount CHECK (amount_minor >= 0);
ALTER TABLE payment.payments ADD CONSTRAINT ck_payments_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE payment.payments ADD CONSTRAINT ck_payments_status CHECK (status IN ('REQUESTED','PROVIDER_PENDING','SUCCEEDED','FAILED','REFUND_PENDING','REFUNDED','REFUND_FAILED'));
ALTER TABLE payment.payment_attempts ADD CONSTRAINT ck_payment_attempts_status CHECK (status IN ('REQUESTED','PROVIDER_PENDING','SUCCEEDED','FAILED','UNKNOWN'));
ALTER TABLE payment.provider_events ADD CONSTRAINT ck_provider_events_identity CHECK (btrim(event_identity) <> '');
ALTER TABLE payment.refunds ADD CONSTRAINT ck_refunds_amount CHECK (amount_minor >= 0);
ALTER TABLE payment.refunds ADD CONSTRAINT ck_refunds_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE payment.refunds ADD CONSTRAINT ck_refunds_status CHECK (status IN ('REQUESTED','PROVIDER_PENDING','COMPLETED','FAILED'));
ALTER TABLE payment.outbox_events ADD CONSTRAINT ck_outbox_events_attempts CHECK (publish_attempts >= 0 AND event_version > 0);

COMMIT;
