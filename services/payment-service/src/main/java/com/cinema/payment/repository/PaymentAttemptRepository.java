package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentAttemptEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {
    Optional<PaymentAttemptEntity> findByProviderAndMerchantReference(String provider, String merchantReference);

    Optional<PaymentAttemptEntity> findFirstByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    Optional<PaymentAttemptEntity> findFirstByPaymentIdAndStatusOrderByCreatedAtDesc(UUID paymentId, String status);

    /**
     * Attempt còn chờ kết quả đã quá tuổi tối thiểu, cũ nhất trước. Khớp
     * ix_payment_attempts_pending.
     */
    @Query("""
            select a from PaymentAttemptEntity a
            where a.provider = :provider and a.status = 'PROVIDER_PENDING' and a.createdAt < :before
            order by a.createdAt
            """)
    List<PaymentAttemptEntity> findPendingOlderThan(@Param("provider") String provider,
            @Param("before") Instant before, Pageable pageable);
}
