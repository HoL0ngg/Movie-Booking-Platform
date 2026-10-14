package com.cinema.booking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.booking.common.JsonUtils;
import com.cinema.booking.dto.CreateReservationRequest;
import com.cinema.booking.dto.ReservationResponse;
import com.cinema.booking.dto.ReservedSeat;
import com.cinema.booking.entity.*;
import com.cinema.booking.repository.IdempotencyKeyRepository;
import com.cinema.booking.repository.ReservationRepository;
import com.cinema.booking.repository.ReservationSeatRepository;
import com.cinema.booking.repository.ShowtimeSeatRepository;
import com.cinema.booking.repository.ShowtimeSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cinema.booking.exception.ApiException;
import static com.cinema.booking.entity.Statuses.*;

@Service
public class ReservationService {
    private static final String OP_CREATE_RESERVATION = "CREATE_RESERVATION";
    private static final int MAX_SEATS_PER_RESERVATION = 8;

    private final ShowtimeSnapshotRepository snapshotRepository;
    private final ShowtimeSeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final Duration holdTtl;

    public ReservationService(ShowtimeSnapshotRepository snapshotRepository,
            ShowtimeSeatRepository seatRepository,
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            IdempotencyKeyRepository idempotencyRepository,
            ObjectMapper objectMapper,
            @Value("${booking.hold-ttl:PT5M}") Duration holdTtl) {
        this.snapshotRepository = snapshotRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.holdTtl = holdTtl;
    }

    // Giu ghe. Idempotent theo(userId, operation, idempotencyKey)
    @Transactional
    public ReservationResponse create(UUID userId, String idempotencyKey, CreateReservationRequest req) {
        validation(idempotencyKey, req);

        List<UUID> seatIds = req.seatIds().stream().distinct().sorted().toList();
        byte[] hash = sha256(req.showtimeId() + ":" + seatIds);

        var idemId = new IdempotencyKeyEntity.Key();
        idemId.setActorId(userId);
        idemId.setIdempotencyKey(idempotencyKey);
        idemId.setOperation(OP_CREATE_RESERVATION);

        Optional<IdempotencyKeyEntity> existing = idempotencyRepository.findById(idemId);
        if (existing.isPresent()) {
            var e = existing.get();

            if (!Arrays.equals(e.getRequestHash(), hash)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Idempotency-Key đã được dùng với request khác");
            }

            return JsonUtils.fromJson(e.getResponseBody(), ReservationResponse.class);
        }
        Instant now = Instant.now();

        ShowtimeSnapshotEntity showtime = snapshotRepository.findById(req.showtimeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy suất chiếu"));
        if (!SHOWTIME_OPEN.equals(showtime.getStatus()) || !now.isBefore(showtime.getSalesCloseAt())) {
            throw new ApiException(HttpStatus.CONFLICT, "Suất chiếu đã đóng bán vé");
        }
        Instant holdExpiresAt = now.plus(holdTtl);
        var reservation = new ReservationEntity();
        reservation.setId(UUID.randomUUID());
        reservation.setShowtimeId(req.showtimeId());
        reservation.setUserId(userId);
        reservation.setHoldExpiresAt(holdExpiresAt);
        reservation.setStatus(RES_HELD);
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        reservationRepository.saveAndFlush(reservation);

        List<ReservedSeat> reservedSeats = new ArrayList<>();

        long total = 0;
        for (UUID seatId : seatIds) {
            ShowtimeSeatEntity seat = seatRepository.lockSeat(req.showtimeId(), seatId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ghế " + seatId));

            boolean free = SEAT_AVAILABLE.equals(seat.getStatus())
                    || (SEAT_HELD.equals(seat.getStatus())
                            && seat.getHoldExpiresAt() != null
                            && seat.getHoldExpiresAt().isBefore(now));

            if (!free) {
                throw new ApiException(HttpStatus.CONFLICT, "Ghế " + seat.getSeatLabel() + " không còn trống");
            }

            seat.setStatus(SEAT_HELD);
            seat.setReservationId(reservation.getId());
            seat.setBookingId(null);
            seat.setHoldExpiresAt(holdExpiresAt);
            seat.setUpdatedAt(now);

            var rs = new ReservationSeatEntity();
            rs.setReservationId(reservation.getId());
            rs.setSeatId(seatId);
            rs.setShowtimeId(req.showtimeId());
            rs.setPriceMinor(seat.getPriceMinor());
            rs.setCreatedAt(now);
            reservationSeatRepository.save(rs);

            total += seat.getPriceMinor();

            reservedSeats.add(new ReservedSeat(seatId, seat.getSeatLabel(), seat.getPriceMinor()));
        }

        var response = new ReservationResponse(reservation.getId(), req.showtimeId(), RES_HELD,
                holdExpiresAt, total, reservedSeats);

        var idem = new IdempotencyKeyEntity();
        idem.setActorId(userId);
        idem.setOperation(OP_CREATE_RESERVATION);
        idem.setIdempotencyKey(idempotencyKey);
        idem.setRequestHash(hash);
        idem.setResponseStatus(HttpStatus.CREATED.value());
        idem.setResponseBody(JsonUtils.toJson(response));
        idem.setCreatedAt(now);
        idem.setExpiresAt(now.plus(Duration.ofHours(24)));
        idempotencyRepository.save(idem);

        return response;
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(UUID userId, UUID reservationId) {
        var r = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy reservation"));
        // var showtime = snapshotRepository.findById(r.getShowtimeId()).orElseThrow();
        var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(r.getId());
        return toResponse(r, items);
    }

    @Transactional
    public void cancel(UUID userId, UUID reservationId) {
        var r = reservationRepository.lockById(reservationId)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy reservation"));
        if (RES_CANCELLED.equals(r.getStatus())) {
            return; // idempotent
        }
        if (!RES_HELD.equals(r.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Reservation ở trạng thái " + r.getStatus() + ", không thể huỷ");
        }
        releaseSeats(r, RES_CANCELLED);
    }

    @Transactional
    public void expire(UUID reservationId) {
        reservationRepository.lockById(reservationId).ifPresent(r -> {
            // Re-check sau khi có lock: có thể đã confirm/cancel trong lúc chờ
            if (RES_HELD.equals(r.getStatus()) && r.getHoldExpiresAt().isBefore(Instant.now())) {
                releaseSeats(r, RES_EXPIRED);
            }
        });
    }

    private void releaseSeats(ReservationEntity r, String newStatus) {
        Instant now = Instant.now();
        var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(r.getId());
        for (var i : items) {
            seatRepository.lockSeat(r.getShowtimeId(), i.getSeatId()).ifPresent(seat -> {
                // only reset status of seat belonging to this reservation
                if (r.getId().equals(seat.getReservationId()) && SEAT_HELD.equals(seat.getStatus())) {
                    seat.setStatus(SEAT_AVAILABLE);
                    seat.setHoldExpiresAt(null);
                    seat.setReservationId(null);
                    seat.setUpdatedAt(now);
                }
            });
            ;
        }

        r.setStatus(newStatus);
        r.setUpdatedAt(now);
    }

    private void validation(String idempotencyKey, CreateReservationRequest req) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 160) {
            throw new IllegalArgumentException("Idempotency-Key là bắt buộc (tối đa 160 ký tự)");
        }
        if (req == null || req.showtimeId() == null || req.seatIds() == null || req.seatIds().isEmpty()) {
            throw new IllegalArgumentException("showtimeId và seatIds là bắt buộc");
        }

        if (req.seatIds().stream().distinct().count() > MAX_SEATS_PER_RESERVATION) {
            throw new IllegalArgumentException("Tối đa " + MAX_SEATS_PER_RESERVATION + " ghế mỗi lần đặt");

        }
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private ReservationResponse toResponse(ReservationEntity r, List<ReservationSeatEntity> items) {
        Map<UUID, ShowtimeSeatEntity> seatMap = new HashMap<>();
        seatRepository.findAllByShowtimeIdAndSeatIdIn(r.getShowtimeId(),
                items.stream().map(ReservationSeatEntity::getSeatId).toList())
                .forEach(s -> seatMap.put(s.getSeatId(), s));

        long total = 0;
        List<ReservedSeat> reservedSeats = new ArrayList<>();
        for (var i : items) {
            total += i.getPriceMinor();
            reservedSeats
                    .add(new ReservedSeat(i.getSeatId(), seatMap.get(i.getSeatId()).getSeatLabel(), i.getPriceMinor()));
        }
        return new ReservationResponse(r.getId(), r.getShowtimeId(), r.getStatus(), r.getHoldExpiresAt(), total,
                reservedSeats);
    }
}
