-- Manual upgrade for an existing cinema-service database. Review and back up first.
-- Apply BEFORE deploying the new cinema-service; never replay schema.sql on existing data.
-- No existing seats or showtimes are removed. This script is applied once, transactionally.
-- Existing COUPLE rows have no inferred pair; audit them manually before first publication.
-- Rollback: roll back application deployment and retain this additive schema/data.
BEGIN;
ALTER TABLE seats ADD COLUMN paired_seat_id uuid;
CREATE UNIQUE INDEX uq_seats_auditorium_id_id ON seats (auditorium_id, id);
ALTER TABLE seats ADD CONSTRAINT fk_seats_paired_seat
    FOREIGN KEY (auditorium_id, paired_seat_id) REFERENCES seats (auditorium_id, id)
    DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE seats ADD CONSTRAINT ck_seats_pair_not_self CHECK (paired_seat_id IS NULL OR paired_seat_id <> id);
-- Abort without changing data if pre-existing rows have unsupported seat types.
ALTER TABLE seats ADD CONSTRAINT ck_seats_type CHECK (seat_type IN ('STANDARD', 'VIP', 'COUPLE'));
COMMIT;
