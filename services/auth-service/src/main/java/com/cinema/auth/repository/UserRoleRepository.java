package com.cinema.auth.repository;

import com.cinema.auth.entity.UserRoleEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.Key> {}
