package com.cinema.cinema.repository;

import com.cinema.cinema.entity.OutboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {}
