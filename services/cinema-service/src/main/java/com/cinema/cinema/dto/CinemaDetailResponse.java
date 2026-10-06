package com.cinema.cinema.dto; // 7.5

import java.util.List; // 7.6

public record CinemaDetailResponse(String id, String name, String address, String city, String timezone, // 7.7
                                   List<AuditoriumResponse> auditoriums) { // 7.8 Danh sách phòng chiếu
}