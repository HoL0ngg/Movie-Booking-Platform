package com.cinema.cinema.repository; // 14.1

import java.util.List; // 14.2
import java.util.UUID; // 14.3

import org.springframework.data.jpa.repository.JpaRepository; // 14.4

import com.cinema.cinema.entity.CinemaEntity; // 14.5

public interface CinemaRepository extends JpaRepository<CinemaEntity, UUID> { // 14.6
    List<CinemaEntity> findAllByOrderByNameAsc(); // 14.7 Tất cả rạp, sắp theo tên
    List<CinemaEntity> findByCityIgnoreCaseOrderByNameAsc(String city); // 14.8 Lọc city không phân biệt hoa thường
}