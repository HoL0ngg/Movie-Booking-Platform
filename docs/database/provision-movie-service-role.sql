-- Run once, only after owner approval on the hosted database.
-- psql parameter movie_db_password must come from a secret manager, never Git.
-- No existing data, table ownership or other service privileges are changed.
BEGIN;
CREATE ROLE movie_service LOGIN PASSWORD :'movie_db_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
GRANT CONNECT ON DATABASE postgres TO movie_service;
GRANT USAGE ON SCHEMA movie TO movie_service;
GRANT SELECT ON movie.movies, movie.genres, movie.movie_genres TO movie_service;
GRANT INSERT, UPDATE ON movie.movies TO movie_service;
GRANT INSERT, DELETE ON movie.movie_genres TO movie_service;
COMMIT;
