package com.cinema.payment.repository;

import com.cinema.payment.entity.ProviderEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProviderEventRepository extends JpaRepository<ProviderEventEntity, UUID> {}
