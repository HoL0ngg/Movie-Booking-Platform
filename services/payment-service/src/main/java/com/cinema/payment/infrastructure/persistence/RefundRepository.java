package com.cinema.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {}
