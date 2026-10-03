package com.cinema.movie.repository;

import com.cinema.movie.entity.MovieEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<MovieEntity, UUID> {}
