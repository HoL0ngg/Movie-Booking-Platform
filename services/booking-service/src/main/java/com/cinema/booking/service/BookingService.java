package com.cinema.booking.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cinema.booking.exception.ApiException;
import com.cinema.booking.dto.BookingResponse;
import com.cinema.booking.dto.ReservedSeat;
import com.cinema.booking.entity.BookingEntity;
import com.cinema.booking.entity.BookingItemEntity;
import com.cinema.booking.entity.ReservationSeatEntity;
import com.cinema.booking.entity.ShowtimeSeatEntity;
import com.cinema.booking.repository.*;

import static com.cinema.booking.entity.Statuses.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeSnapshotRepository showtimeSnapshotRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;

    public BookingService(ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            ShowtimeSnapshotRepository showtimeSnapshotRepository,
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository) {
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeSnapshotRepository = showtimeSnapshotRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

    // Confirm booking after paying successfully
    @Transactional
    public BookingResponse confirm(UUID userId, UUID reservationId, UUID paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("PaymentId is required");
        }
        var reservation = reservationRepository.lockById(reservationId)
                .filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy reservation"));
        var existing = bookingRepository.findByReservationId(reservationId);
        if (existing.isPresent()) {
            if (!existing.get().getPaymentId().equals(paymentId)) {
                throw new ApiException(HttpStatus.CONFLICT, "Reservation đã được xác nhận với payment khác");
            }
            return toResponse(existing.get());
        }

        Instant now = Instant.now();
        if (!RES_HELD.equals(reservation.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation ở trạng thái " + reservation.getStatus());
        }
        if (!reservation.getHoldExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation đã hết hạn giữ ghế");
        }
        var showtime = showtimeSnapshotRepository.findById(reservation.getShowtimeId()).orElseThrow();
        var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(reservationId);

        var booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setReservationId(reservationId);
        booking.setShowtimeId(reservation.getShowtimeId());
        booking.setUserId(userId);
        booking.setPaymentId(paymentId);
        booking.setAmountMinor(items.stream().mapToLong(ReservationSeatEntity::getPriceMinor).sum());
        booking.setCurrency(showtime.getCurrency());
        booking.setStatus(BOOKING_CONFIRMED);
        booking.setConfirmedAt(now);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        bookingRepository.saveAndFlush(booking);

        for (var i : items) {
            ShowtimeSeatEntity seat = showtimeSeatRepository.lockSeat(reservation.getShowtimeId(), i.getSeatId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ghế"));
            if (!reservationId.equals(seat.getReservationId()) || !SEAT_HELD.equals(seat.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Ghế " + seat.getSeatLabel() + " không còn thuộc reservation này");
            }

            seat.setStatus(SEAT_BOOKED);
            seat.setBookingId(booking.getId());
            seat.setHoldExpiresAt(null);
            seat.setUpdatedAt(now);

            var bi = new BookingItemEntity();
            bi.setId(UUID.randomUUID());
            bi.setBookingId(booking.getId());
            bi.setShowtimeId(reservation.getShowtimeId());
            bi.setSeatId(i.getSeatId());
            bi.setPriceMinor(i.getPriceMinor()); // chốt giá lúc giữ ghế
            bi.setCreatedAt(now);
            bookingItemRepository.save(bi);
        }

        reservation.setStatus(RES_CONFIRMED);
        reservation.setUpdatedAt(now);

        // TODO: ghi outbox_events (vd. BookingConfirmed v1) trong cùng transaction này

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse get(UUID userId, UUID bookingId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy booking"));
    }

    public BookingResponse toResponse(BookingEntity b) {
        var items = bookingItemRepository.findAllByBookingIdOrderBySeatId(b.getId());
        Map<UUID, String> labels = new HashMap<>();
        showtimeSeatRepository.findAllByShowtimeIdAndSeatIdIn(b.getShowtimeId(),
                items.stream().map(BookingItemEntity::getSeatId).toList())
                .forEach(s -> labels.put(s.getSeatId(), s.getSeatLabel()));

        List<ReservedSeat> seats = items.stream()
                .map(i -> new ReservedSeat(i.getSeatId(), labels.get(i.getSeatId()), i.getPriceMinor()))
                .toList();

        return new BookingResponse(b.getId(), b.getReservationId(), b.getShowtimeId(), b.getStatus(),
                b.getAmountMinor(), b.getCurrency(), b.getConfirmedAt(), seats);
    }
}
