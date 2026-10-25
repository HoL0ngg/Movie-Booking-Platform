package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByUserIdAndRequestKey(UUID userId, String requestKey);

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    Optional<PaymentEntity> findByReservationId(UUID reservationId);

    Optional<PaymentEntity> findByIdAndUserId(UUID id, UUID userId);

    /**
     * SELECT ... FOR UPDATE: tuần tự hoá IPN / querydr / refund trên cùng một
     * payment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.id = :id")
    Optional<PaymentEntity> lockById(@Param("id") UUID id);
}
