package com.cinema.cinema.service; // 8.1

import com.cinema.cinema.dto.AuditoriumResponse; // 8.2
import com.cinema.cinema.dto.CinemaDetailResponse; // 8.3
import com.cinema.cinema.dto.CinemaResponse; // 8.4
import com.cinema.cinema.entity.CinemaEntity; // 8.5
import com.cinema.cinema.exception.CinemaException; // 8.6
import com.cinema.cinema.repository.AuditoriumRepository; // 8.7
import com.cinema.cinema.repository.CinemaRepository; // 8.8
import com.cinema.cinema.repository.SeatRepository; // 8.9
import java.util.List; // 8.10
import java.util.UUID; // 8.11
import org.springframework.http.HttpStatus; // 8.12
import org.springframework.stereotype.Service; // 8.13
import org.springframework.transaction.annotation.Transactional; // 8.14
import org.springframework.util.StringUtils; // 8.15

@Service // 8.16
public class CinemaService { // 8.17

    private final CinemaRepository cinemaRepository; // 8.18
    private final AuditoriumRepository auditoriumRepository; // 8.19
    private final SeatRepository seatRepository; // 8.20

    public CinemaService(CinemaRepository cinemaRepository, AuditoriumRepository auditoriumRepository, SeatRepository seatRepository) { // 8.21
        this.cinemaRepository = cinemaRepository; // 8.22
        this.auditoriumRepository = auditoriumRepository; // 8.23
        this.seatRepository = seatRepository; // 8.24
    }

    @Transactional(readOnly = true) // 8.25 Chỉ đọc
    public List<CinemaResponse> list(String city) { // 8.26
        List<CinemaEntity> found = StringUtils.hasText(city) // 8.27 Có city thì lọc, không thì lấy hết
                ? cinemaRepository.findByCityIgnoreCaseOrderByNameAsc(city.trim()) // 8.28
                : cinemaRepository.findAllByOrderByNameAsc(); // 8.29
        return found.stream() // 8.30
                .map(c -> new CinemaResponse(c.getId().toString(), c.getName(), c.getAddress(), c.getCity(), c.getTimezone())) // 8.31 Entity → DTO
                .toList(); // 8.32
    }

    @Transactional(readOnly = true) // 8.33
    public CinemaDetailResponse get(UUID cinemaId) { // 8.34
        CinemaEntity cinema = cinemaRepository.findById(cinemaId) // 8.35
                .orElseThrow(() -> new CinemaException(HttpStatus.NOT_FOUND, "CINEMA_NOT_FOUND", "Cinema was not found.")); // 8.36
        List<AuditoriumResponse> auditoriums = auditoriumRepository.findByCinemaIdOrderByNameAsc(cinemaId).stream() // 8.37 Các phòng của rạp
                .map(a -> new AuditoriumResponse(a.getId().toString(), a.getName(), // 8.38
                        seatRepository.countActiveByAuditoriumId(a.getId()))) // 8.39 Đếm ghế hoạt động của phòng
                .toList(); // 8.40
        return new CinemaDetailResponse(cinema.getId().toString(), cinema.getName(), cinema.getAddress(), // 8.41
                cinema.getCity(), cinema.getTimezone(), auditoriums); // 8.42
    }
}