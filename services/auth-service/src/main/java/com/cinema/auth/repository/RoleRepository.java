package com.cinema.auth.repository; // 11.9

import java.util.List; // 11.10
import java.util.Optional; // 11.11
import java.util.UUID; // 11.12

import org.springframework.data.jpa.repository.JpaRepository; // 11.13
import org.springframework.data.jpa.repository.Query; // 11.14
import org.springframework.data.repository.query.Param; // 11.15

import com.cinema.auth.entity.RoleEntity; // 11.16

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> { // 11.17
    Optional<RoleEntity> findByCode(String code); // 11.18

    @Query("select r.code from RoleEntity r where r.id in " // 11.19 JPQL: lấy mã role của 1 user
            + "(select ur.roleId from UserRoleEntity ur where ur.userId = :userId)") // 11.20
    List<String> findCodesByUserId(@Param("userId") UUID userId); // 11.21 :userId gắn với tham số
}