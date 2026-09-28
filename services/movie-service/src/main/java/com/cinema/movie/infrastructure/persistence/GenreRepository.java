package com.cinema.movie.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<GenreEntity, UUID> {}
