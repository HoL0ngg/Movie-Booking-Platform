package com.cinema.booking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShowtimeSnapshotRepository extends JpaRepository<ShowtimeSnapshotEntity, UUID> {}
