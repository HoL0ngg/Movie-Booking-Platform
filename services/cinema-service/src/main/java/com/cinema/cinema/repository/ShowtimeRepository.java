package com.cinema.cinema.repository;

import com.cinema.cinema.entity.ShowtimeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShowtimeRepository extends JpaRepository<ShowtimeEntity, UUID> {}
