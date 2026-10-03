package com.cinema.booking.repository;

import com.cinema.booking.entity.IdempotencyKeyEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyEntity.Key> {}
