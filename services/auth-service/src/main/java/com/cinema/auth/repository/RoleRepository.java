package com.cinema.auth.repository;

import com.cinema.auth.entity.RoleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {}
