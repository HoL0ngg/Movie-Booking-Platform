package com.cinema.cinema.repository;

import com.cinema.cinema.entity.AuditoriumEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditoriumRepository extends JpaRepository<AuditoriumEntity, UUID> {}
