package com.cinema.movie.repository;

import com.cinema.movie.entity.MovieGenreEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieGenreRepository extends JpaRepository<MovieGenreEntity, MovieGenreEntity.Key> {}
