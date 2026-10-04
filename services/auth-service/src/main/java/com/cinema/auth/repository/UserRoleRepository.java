package com.cinema.auth.repository; // 11.22

import org.springframework.data.jpa.repository.JpaRepository; // 11.23

import com.cinema.auth.entity.UserRoleEntity; // 11.24

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.Key> { // 11.25 Khóa kép = Key
}