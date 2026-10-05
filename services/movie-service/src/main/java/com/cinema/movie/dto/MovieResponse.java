package com.cinema.movie.dto; // 4.1

import java.time.LocalDate; // 4.2
import java.util.List; // 4.3
import java.util.UUID; // 4.4

public record MovieResponse(UUID id, String title, String synopsis, String posterUrl, // 4.5 Record = DTO bất biến
                            Integer durationMinutes, LocalDate releaseDate, String status, // 4.6
                            List<String> genres) {} // 4.7 MỚI: tên các thể loại (rỗng nếu chưa gán)