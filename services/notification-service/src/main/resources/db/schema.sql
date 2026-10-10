-- Reference schema for an empty, service-owned database. Never executed at application startup.
-- Update this snapshot when changing the managed database; do not replay on an existing database.
-- Notification schema, local to cinema_notification.
-- All cross-service UUIDs are logical references, never foreign keys.
CREATE TABLE notifications (
  id uuid NOT NULL,
  source_event_id uuid NOT NULL,
  event_type varchar(80) NOT NULL,
  user_id uuid NOT NULL,
  channel varchar(16) NOT NULL,
  subject text NOT NULL DEFAULT '',
  body text NOT NULL DEFAULT '',
  recipient_address text NOT NULL,
  status varchar(16) NOT NULL,
  attempt_count integer NOT NULL,
  next_attempt_at timestamptz NOT NULL,
  provider_message_id varchar(160),
  last_error_code varchar(80),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  sent_at timestamptz,
  CONSTRAINT pk_notifications PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_notifications_source_event_id_channel ON notifications (source_event_id, channel);
CREATE INDEX ix_notifications_status_next_attempt_at ON notifications (status, next_attempt_at)
WHERE status IN ('PENDING', 'FAILED');
ALTER TABLE notifications
ADD CONSTRAINT ck_notifications_channel CHECK (channel = 'EMAIL');
ALTER TABLE notifications
ADD CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
ALTER TABLE notifications
ADD CONSTRAINT ck_notifications_attempts CHECK (attempt_count >= 0);
ALTER TABLE notifications
ADD CONSTRAINT ck_notifications_sent CHECK (
    status <> 'SENT'
    OR sent_at IS NOT NULL
  );