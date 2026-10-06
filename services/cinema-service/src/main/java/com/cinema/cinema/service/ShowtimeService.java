package com.cinema.cinema.service; // 9.1

import java.time.Instant; // 9.2
import java.time.LocalDate; // 9.3
import java.time.ZoneId; // 9.4
import java.util.List; // 9.5
import java.util.UUID; // 9.6

import org.springframework.dao.DataIntegrityViolationException; // 9.7
import org.springframework.http.HttpStatus; // 9.8
import org.springframework.security.oauth2.jwt.Jwt; // 9.9
import org.springframework.stereotype.Service; // 9.10
import org.springframework.transaction.annotation.Transactional; // 9.11

import com.cinema.cinema.dto.CancelShowtimeRequest; // 9.12
import com.cinema.cinema.dto.CreateShowtimeRequest; // 9.13
import com.cinema.cinema.dto.MoneyResponse; // 9.14
import com.cinema.cinema.dto.ShowtimeResponse; // 9.15
import com.cinema.cinema.entity.AuditoriumEntity; // 9.16
import com.cinema.cinema.entity.CinemaEntity; // 9.17
import com.cinema.cinema.entity.SeatEntity; // 9.18
import com.cinema.cinema.entity.ShowtimeEntity; // 9.19
import com.cinema.cinema.exception.CinemaException; // 9.20
import com.cinema.cinema.messaging.EventJson; // 9.21
import com.cinema.cinema.repository.AuditoriumRepository; // 9.22
import com.cinema.cinema.repository.CinemaRepository; // 9.23
import com.cinema.cinema.repository.SeatRepository; // 9.24
import com.cinema.cinema.repository.ShowtimeRepository; // 9.25

@Service // 9.26
public class ShowtimeService { // 9.27

    private static final String DRAFT = "DRAFT"; // 9.28 Khớp CHECK status ở DB
    private static final String PUBLISHED = "PUBLISHED"; // 9.29
    private static final String CANCELLED = "CANCELLED"; // 9.30

    private final ShowtimeRepository showtimeRepository; // 9.31
    private final AuditoriumRepository auditoriumRepository; // 9.32
    private final CinemaRepository cinemaRepository; // 9.33
    private final SeatRepository seatRepository; // 9.34
    private final OutboxService outboxService; // 9.35
    private final CinemaAuthorizationService authorization; // 9.36

    public ShowtimeService(ShowtimeRepository showtimeRepository, AuditoriumRepository auditoriumRepository, // 9.37
                           CinemaRepository cinemaRepository, SeatRepository seatRepository, // 9.38
                           OutboxService outboxService, CinemaAuthorizationService authorization) { // 9.39
        this.showtimeRepository = showtimeRepository; // 9.40
        this.auditoriumRepository = auditoriumRepository; // 9.41
        this.cinemaRepository = cinemaRepository; // 9.42
        this.seatRepository = seatRepository; // 9.43
        this.outboxService = outboxService; // 9.44
        this.authorization = authorization; // 9.45
    }

    @Transactional(readOnly = true) // 9.46
    public List<ShowtimeResponse> listByCinema(UUID cinemaId, LocalDate date, UUID movieId) { // 9.47
        CinemaEntity cinema = cinemaRepository.findById(cinemaId) // 9.48
                .orElseThrow(() -> new CinemaException(HttpStatus.NOT_FOUND, "CINEMA_NOT_FOUND", "Cinema was not found.")); // 9.49
        ZoneId zone = ZoneId.of(cinema.getTimezone()); // 9.50 "Ngày" tính theo múi giờ của rạp
        Instant rangeStart = date.atStartOfDay(zone).toInstant(); // 9.51 0h ngày đó → UTC
        Instant rangeEnd = date.plusDays(1).atStartOfDay(zone).toInstant(); // 9.52 0h ngày kế tiếp
        List<UUID> auditoriumIds = auditoriumRepository.findByCinemaIdOrderByNameAsc(cinemaId).stream() // 9.53
                .map(AuditoriumEntity::getId).toList(); // 9.54 Id các phòng của rạp
        if (auditoriumIds.isEmpty()) { // 9.55 Tránh IN () rỗng
            return List.of(); // 9.56
        }
        return showtimeRepository.findPublishedInRange(auditoriumIds, rangeStart, rangeEnd).stream() // 9.57
                .filter(s -> movieId == null || movieId.equals(s.getMovieId())) // 9.58 Lọc phim nếu có
                .map(s -> toResponse(s, cinemaId)) // 9.59
                .toList(); // 9.60
    }

    @Transactional(readOnly = true) // 9.61
    public ShowtimeResponse get(UUID showtimeId) { // 9.62
        ShowtimeEntity showtime = showtimeRepository.findById(showtimeId) // 9.63
                .filter(s -> !DRAFT.equals(s.getStatus())) // 9.64 Suất nháp công khai coi như không tồn tại
                .orElseThrow(this::showtimeNotFound); // 9.65
        return toResponse(showtime, loadAuditorium(showtime.getAuditoriumId()).getCinemaId()); // 9.66
    }

    @Transactional // 9.67
    public ShowtimeResponse create(Jwt jwt, CreateShowtimeRequest request) { // 9.68
        AuditoriumEntity auditorium = auditoriumRepository.findById(request.auditoriumId()) // 9.69
                .orElseThrow(() -> new CinemaException(HttpStatus.NOT_FOUND, "AUDITORIUM_NOT_FOUND", "Auditorium was not found.")); // 9.70
        authorization.assertCanManage(jwt, auditorium.getCinemaId()); // 9.71 Kiểm tra quyền theo rạp
        if (!request.startsAt().isBefore(request.endsAt()) || request.salesCloseAt().isAfter(request.startsAt())) { // 9.72 Khớp CHECK ck_showtimes_time
            throw new CinemaException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid showtime times."); // 9.73
        }
        ShowtimeEntity showtime = new ShowtimeEntity(UUID.randomUUID(), auditorium.getId(), request.movieId(), // 9.74
                request.startsAt(), request.endsAt(), request.salesCloseAt(), // 9.75
                request.priceMinor(), request.currency(), DRAFT, Instant.now()); // 9.76 Tạo ở trạng thái DRAFT
        showtimeRepository.save(showtime); // 9.77
        return toResponse(showtime, auditorium.getCinemaId()); // 9.78
    }

    @Transactional // 9.79 Cập nhật showtime + ghi outbox trong 1 transaction
    public ShowtimeResponse publish(Jwt jwt, UUID showtimeId) { // 9.80
        ShowtimeEntity showtime = showtimeRepository.findForUpdateById(showtimeId) // 9.81 Khóa dòng (FOR UPDATE)
                .orElseThrow(this::showtimeNotFound); // 9.82
        AuditoriumEntity auditorium = loadAuditorium(showtime.getAuditoriumId()); // 9.83
        authorization.assertCanManage(jwt, auditorium.getCinemaId()); // 9.84
        if (PUBLISHED.equals(showtime.getStatus())) { // 9.85 Đã publish: idempotent, không tạo event mới
            return toResponse(showtime, auditorium.getCinemaId()); // 9.86
        }
        if (CANCELLED.equals(showtime.getStatus())) { // 9.87 Không publish lại suất đã hủy
            throw invalidState("Cancelled showtime cannot be published."); // 9.88
        }
        List<SeatEntity> seats = seatRepository.findActiveByAuditoriumId(auditorium.getId()); // 9.89 Ghế đang hoạt động, đã sắp thứ tự
        if (seats.isEmpty()) { // 9.90 Event yêu cầu seats[] tối thiểu 1 phần tử
            throw invalidState("Auditorium has no active seats."); // 9.91
        }
        Instant now = Instant.now(); // 9.92
        showtime.markPublished(now); // 9.93 status → PUBLISHED
        try { // 9.94
            showtimeRepository.saveAndFlush(showtime); // 9.95 Flush ngay để ràng buộc chống chồng giờ nổ tại đây
        } catch (DataIntegrityViolationException ex) { // 9.96 Vi phạm ex_showtimes_auditorium_overlap
            throw invalidState("Showtime overlaps another published showtime."); // 9.97
        }
        outboxService.record("ShowtimePublished", showtime.getId(), // 9.98 Ghi event cùng transaction
                EventJson.publishedPayload(showtime, auditorium.getCinemaId(), seats), now); // 9.99
        return toResponse(showtime, auditorium.getCinemaId()); // 9.100
    }

    @Transactional // 9.101
    public ShowtimeResponse cancel(Jwt jwt, UUID showtimeId, CancelShowtimeRequest request) { // 9.102
        ShowtimeEntity showtime = showtimeRepository.findForUpdateById(showtimeId) // 9.103
                .orElseThrow(this::showtimeNotFound); // 9.104
        AuditoriumEntity auditorium = loadAuditorium(showtime.getAuditoriumId()); // 9.105
        authorization.assertCanManage(jwt, auditorium.getCinemaId()); // 9.106
        if (CANCELLED.equals(showtime.getStatus())) { // 9.107 Đã hủy: idempotent
            return toResponse(showtime, auditorium.getCinemaId()); // 9.108
        }
        boolean wasPublished = PUBLISHED.equals(showtime.getStatus()); // 9.109 Chỉ suất đã publish mới có booking biết tới
        Instant now = Instant.now(); // 9.110
        showtime.markCancelled(now); // 9.111 status → CANCELLED, snapshot_version + 1
        showtimeRepository.save(showtime); // 9.112
        if (wasPublished) { // 9.113
            outboxService.record("ShowtimeCancelled", showtime.getId(), // 9.114
                    EventJson.cancelledPayload(showtime.getId(), showtime.getSnapshotVersion(), request.reasonCode(), now), now); // 9.115
        }
        return toResponse(showtime, auditorium.getCinemaId()); // 9.116
    }

    private AuditoriumEntity loadAuditorium(UUID auditoriumId) { // 9.117
        return auditoriumRepository.findById(auditoriumId) // 9.118
                .orElseThrow(() -> new IllegalStateException("Auditorium missing for showtime")); // 9.119 FK đảm bảo luôn có → nếu không là lỗi hệ thống (500)
    }

    private CinemaException showtimeNotFound() { // 9.120
        return new CinemaException(HttpStatus.NOT_FOUND, "SHOWTIME_NOT_FOUND", "Showtime was not found."); // 9.121
    }

    private CinemaException invalidState(String message) { // 9.122
        return new CinemaException(HttpStatus.CONFLICT, "INVALID_STATE", message); // 9.123 409 theo api-contracts.md
    }

    private ShowtimeResponse toResponse(ShowtimeEntity s, UUID cinemaId) { // 9.124 Entity → DTO
        return new ShowtimeResponse(s.getId().toString(), s.getMovieId().toString(), cinemaId.toString(), // 9.125
                s.getAuditoriumId().toString(), s.getStartsAt(), s.getEndsAt(), s.getSalesCloseAt(), // 9.126
                new MoneyResponse(s.getPriceMinor(), s.getCurrency()), s.getStatus()); // 9.127
    }
}