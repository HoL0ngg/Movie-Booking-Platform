package com.cinema.auth.repository;

import com.cinema.auth.entity.RefreshSessionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {}
