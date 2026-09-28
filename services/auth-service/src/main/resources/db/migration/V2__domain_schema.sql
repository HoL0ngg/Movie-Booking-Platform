-- Auth schema, local to cinema_auth.
-- All cross-service UUIDs are logical references, never foreign keys.

CREATE TABLE users (
  id uuid NOT NULL,
  email text NOT NULL,
  email_normalized text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  status varchar(16) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE roles (
  id uuid NOT NULL,
  code varchar(40) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE user_roles (
  user_id uuid NOT NULL,
  role_id uuid NOT NULL,
  granted_at timestamptz NOT NULL,
  CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_sessions (
  id uuid NOT NULL,
  user_id uuid NOT NULL,
  token_hash bytea NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_refresh_sessions PRIMARY KEY (id)
);

CREATE INDEX ix_user_roles_role_id ON user_roles (role_id);
CREATE INDEX ix_refresh_sessions_user_id_expires_at ON refresh_sessions (user_id, expires_at);
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT;

ALTER TABLE refresh_sessions ADD CONSTRAINT fk_refresh_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;


ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','DISABLED'));
ALTER TABLE users ADD CONSTRAINT ck_users_email CHECK (email_normalized = lower(btrim(email)) AND email_normalized <> '');
ALTER TABLE roles ADD CONSTRAINT ck_roles_code CHECK (btrim(code) <> '');
ALTER TABLE refresh_sessions ADD CONSTRAINT ck_refresh_sessions_expiry CHECK (expires_at > created_at);
