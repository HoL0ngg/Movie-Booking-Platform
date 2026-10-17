package com.cinema.movie.repository;

import com.cinema.movie.entity.MovieEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<MovieEntity, UUID> {
    @Override
    List<MovieEntity> findAll();

    List<MovieEntity> findByStatus(String status);
    Optional<MovieEntity> findByIdAndStatus(UUID id, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MovieEntity m where m.id = :id")
    Optional<MovieEntity> findForUpdate(@Param("id") UUID id);
}
