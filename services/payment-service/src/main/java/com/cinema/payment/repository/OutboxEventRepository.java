package com.cinema.payment.repository;

import com.cinema.payment.entity.OutboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {}
