package com.cinema.movie.repository;

import com.cinema.movie.entity.MovieEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<MovieEntity, UUID> {
    @Override
    List<MovieEntity> findAll();

    List<MovieEntity> findByStatus(String status);
    Optional<MovieEntity> findByIdAndStatus(UUID id, String status);
}
