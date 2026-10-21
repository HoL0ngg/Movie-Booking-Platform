package com.cinema.payment.repository;

import com.cinema.payment.entity.RefundEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
    Optional<RefundEntity> findByPaymentId(UUID paymentId);

    /**
     * Refund đến hạn xử lý: REQUESTED xử lý ngay; PROVIDER_PENDING (kết quả chưa
     * chắc) thử lại sau khoảng retryBefore.
     */
    @Query("""
            select r from RefundEntity r
            where (r.status = 'REQUESTED' and r.updatedAt <= :now)
               or (r.status = 'PROVIDER_PENDING' and r.updatedAt <= :retryBefore)
            order by r.updatedAt
            """)
    List<RefundEntity> findDue(@Param("now") Instant now, @Param("retryBefore") Instant retryBefore,
            Pageable pageable);

    /**
     * "Lease" tối ưu: chỉ một instance thắng (updated_at còn khớp giá trị đã đọc)
     * và được gọi VNPay.
     * 
     * @return 1 nếu claim thành công
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefundEntity r set r.updatedAt = :now, r.status = 'PROVIDER_PENDING'
            where r.id = :id and r.updatedAt = :seen and r.status in ('REQUESTED', 'PROVIDER_PENDING')
            """)
    int claim(@Param("id") UUID id, @Param("seen") Instant seen, @Param("now") Instant now);
}
