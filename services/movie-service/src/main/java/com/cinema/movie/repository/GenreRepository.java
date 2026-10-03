package com.cinema.movie.repository;

import com.cinema.movie.entity.GenreEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<GenreEntity, UUID> {}
