package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentAttemptEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {}
