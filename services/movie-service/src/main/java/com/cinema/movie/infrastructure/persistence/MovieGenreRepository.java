package com.cinema.movie.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieGenreRepository extends JpaRepository<MovieGenreEntity, MovieGenreEntity.Key> {}
