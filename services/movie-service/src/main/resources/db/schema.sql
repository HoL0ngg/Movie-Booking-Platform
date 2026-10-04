-- Reference schema for an empty, service-owned database. Never executed at application startup.
-- Update this snapshot when changing the managed database; do not replay on an existing database.

-- Movie schema, local to cinema_movie.
-- All cross-service UUIDs are logical references, never foreign keys.

CREATE TABLE movies (
  id uuid NOT NULL,
  title text NOT NULL,
  synopsis text,
  poster_url text,
  duration_minutes integer NOT NULL,
  release_date date,
  status varchar(16) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pk_movies PRIMARY KEY (id)
);

CREATE TABLE genres (
  id uuid NOT NULL,
  name text NOT NULL UNIQUE,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_genres PRIMARY KEY (id)
);

CREATE TABLE movie_genres (
  movie_id uuid NOT NULL,
  genre_id uuid NOT NULL,
  created_at timestamptz NOT NULL,
  CONSTRAINT pk_movie_genres PRIMARY KEY (movie_id, genre_id)
);

CREATE INDEX ix_movies_status_release_date ON movies (status, release_date);
CREATE INDEX ix_movie_genres_genre_id_movie_id ON movie_genres (genre_id, movie_id);
ALTER TABLE movie_genres ADD CONSTRAINT fk_movie_genres_movie_id FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE RESTRICT;

ALTER TABLE movie_genres ADD CONSTRAINT fk_movie_genres_genre_id FOREIGN KEY (genre_id) REFERENCES genres (id) ON DELETE RESTRICT;


ALTER TABLE movies ADD CONSTRAINT ck_movies_duration CHECK (duration_minutes > 0);
ALTER TABLE movies ADD CONSTRAINT ck_movies_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'));
ALTER TABLE genres ADD CONSTRAINT ck_genres_name CHECK (btrim(name) <> '');
