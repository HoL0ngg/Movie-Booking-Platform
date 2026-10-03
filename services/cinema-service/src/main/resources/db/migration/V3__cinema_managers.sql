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
