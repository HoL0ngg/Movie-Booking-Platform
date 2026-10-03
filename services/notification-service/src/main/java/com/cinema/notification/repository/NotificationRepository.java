package com.cinema.notification.repository;

import com.cinema.notification.entity.NotificationEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {}
