package com.cinema.booking.repository;

import com.cinema.booking.entity.OutboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {}
