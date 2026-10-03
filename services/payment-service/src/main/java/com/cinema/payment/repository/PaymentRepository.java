package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {}
