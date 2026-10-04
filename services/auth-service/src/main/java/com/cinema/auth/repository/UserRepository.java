package com.cinema.auth.repository; // 11.1

import java.util.Optional; // 11.2
import java.util.UUID; // 11.3

import org.springframework.data.jpa.repository.JpaRepository; // 11.4

import com.cinema.auth.entity.UserEntity; // 11.5

public interface UserRepository extends JpaRepository<UserEntity, UUID> { // 11.6 CRUD sẵn có
    Optional<UserEntity> findByEmailNormalized(String emailNormalized); // 11.7 Spring tự sinh SQL từ tên method
    boolean existsByEmailNormalized(String emailNormalized); // 11.8
}