package com.cinema.payment.repository;

import com.cinema.payment.entity.ProviderEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProviderEventRepository extends JpaRepository<ProviderEventEntity, UUID> {
    /**
     * return 1 nếu ghi nhận sự kiện mới, 0 nếu (provider, event_identity) đã có
     * (IPN retry / trùng querydr).
     * Dùng ON CONFLICT để không làm hỏng transaction hiện tại.
     */
    @Modifying
    @Query(value = """
            insert into provider_events
              (id, payment_id, provider, event_identity, provider_event_id, payload_digest, result_code, received_at)
            values (:id, :paymentId, :provider, :identity, :providerEventId, :digest, :resultCode, :receivedAt)
            on conflict (provider, event_identity) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
            @Param("paymentId") UUID paymentId,
            @Param("provider") String provider,
            @Param("identity") String identity,
            @Param("providerEventId") String providerEventId,
            @Param("digest") byte[] digest,
            @Param("resultCode") String resultCode,
            @Param("receivedAt") Instant receivedAt);

}
