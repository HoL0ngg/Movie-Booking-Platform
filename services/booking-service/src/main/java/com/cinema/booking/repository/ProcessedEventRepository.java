package com.cinema.booking.repository;

import com.cinema.booking.entity.ProcessedEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {}
