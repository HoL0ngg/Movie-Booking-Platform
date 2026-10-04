package com.cinema.auth.repository; // 11.26

import java.time.Instant; // 11.27
import java.util.Optional; // 11.28
import java.util.UUID; // 11.29

import org.springframework.data.jpa.repository.JpaRepository; // 11.30
import org.springframework.data.jpa.repository.Lock; // 11.31
import org.springframework.data.jpa.repository.Modifying; // 11.32
import org.springframework.data.jpa.repository.Query; // 11.33
import org.springframework.data.repository.query.Param; // 11.34

import com.cinema.auth.entity.RefreshSessionEntity; // 11.35

import jakarta.persistence.LockModeType; // 11.36

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> { // 11.37

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 11.38 SELECT ... FOR UPDATE: 2 request refresh cùng token phải xếp hàng
    Optional<RefreshSessionEntity> findByTokenHash(byte[] tokenHash); // 11.39

    @Modifying(clearAutomatically = true) // 11.40 Query UPDATE; xóa cache JPA sau khi chạy
    @Query("update RefreshSessionEntity s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null") // 11.41 Thu hồi mọi session còn hiệu lực
    int revokeAllActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now); // 11.42 Trả số dòng bị đổi
}