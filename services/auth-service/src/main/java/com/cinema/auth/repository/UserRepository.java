package com.cinema.auth.repository;

import com.cinema.auth.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {}
