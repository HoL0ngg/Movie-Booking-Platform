package com.cinema.booking.repository;

import com.cinema.booking.entity.IdempotencyKeyEntity;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyEntity.Key> {
    @Modifying
    @Query("delete from IdempotencyKeyEntity k where k.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
