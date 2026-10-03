package com.cinema.cinema.repository;

import com.cinema.cinema.entity.CinemaEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CinemaRepository extends JpaRepository<CinemaEntity, UUID> {}
