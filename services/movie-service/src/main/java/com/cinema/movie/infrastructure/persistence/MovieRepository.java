package com.cinema.movie.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<MovieEntity, UUID> {}
