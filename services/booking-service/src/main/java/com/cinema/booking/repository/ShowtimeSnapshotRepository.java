package com.cinema.booking.repository;

import com.cinema.booking.entity.ShowtimeSnapshotEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShowtimeSnapshotRepository extends JpaRepository<ShowtimeSnapshotEntity, UUID> {}
