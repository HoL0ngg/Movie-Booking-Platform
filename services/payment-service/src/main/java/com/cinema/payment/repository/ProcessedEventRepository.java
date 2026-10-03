package com.cinema.payment.repository;

import com.cinema.payment.entity.ProcessedEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {}
