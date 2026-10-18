package com.cinema.booking.inbox;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.cinema.booking.entity.ShowtimeSeatEntity;
import com.cinema.booking.entity.ShowtimeSnapshotEntity;
import com.cinema.booking.messaging.PoisonMessageException;
import com.cinema.booking.messaging.ShowtimeCancelledPayload;
import com.cinema.booking.messaging.ShowtimeSnapshotPayload;
import com.cinema.booking.repository.ShowtimeSeatRepository;
import com.cinema.booking.repository.ShowtimeSnapshotRepository;

import static com.cinema.booking.entity.Statuses.SEAT_AVAILABLE;

/**
 * Đồng bộ showtime_snapshots / showtime_seats từ Cinema service. Chạy trong
 * transaction của processor.
 */
@Service
public class ShowtimeSnapshotSyncService {
    private static final Logger LOG = Logger.getLogger(ShowtimeSnapshotSyncService.class.getName());
    private static final Set<String> VALID_STATUSES = Set.of("PUBLISHED", "CANCELLED"); // ck_showtime_snapshots_status

    private final ShowtimeSnapshotRepository snapshotRepository;
    private final ShowtimeSeatRepository seatRepository;

    public ShowtimeSnapshotSyncService(ShowtimeSnapshotRepository snapshotRepository,
            ShowtimeSeatRepository seatRepository) {
        this.snapshotRepository = snapshotRepository;
        this.seatRepository = seatRepository;
    }

    /**
     * ShowtimePublished. Chỉ áp dụng khi snapshotVersion mới hơn bản đang có => an
     * toàn với event
     * đến sai thứ tự / trùng. Ghế mới được thêm với trạng thái AVAILABLE. Ghế đã
     * có: chỉ cập nhật
     * nhãn/loại/giá khi đang AVAILABLE, không bao giờ đụng tới ghế đang HELD /
     * SOLD.
     *
     * @param statusFromEvent status suy ra từ event type (Cinema không gửi status
     *                        trong payload)
     */
    public void apply(ShowtimeSnapshotPayload p, String statusFromEvent) {
        validate(p);
        Instant now = Instant.now();

        ShowtimeSnapshotEntity snapshot = snapshotRepository.findById(p.showtimeId()).orElse(null);
        if (snapshot != null && snapshot.getSnapshotVersion() >= p.snapshotVersion()) {
            LOG.info(() -> "Bỏ qua snapshot cũ của showtime " + p.showtimeId());
            return;
        }
        if (snapshot == null) {
            snapshot = new ShowtimeSnapshotEntity();
            snapshot.setShowtimeId(p.showtimeId());
            snapshot.setCreatedAt(now);
        }
        snapshot.setCinemaId(p.cinemaId());
        snapshot.setAuditoriumId(p.auditoriumId());
        snapshot.setMovieId(p.movieId());
        snapshot.setStartsAt(parse(p.startsAt()));
        snapshot.setSalesCloseAt(parse(p.salesCloseAt()));
        snapshot.setCurrency(p.currency());
        snapshot.setStatus(p.status() != null ? p.status() : statusFromEvent);
        snapshot.setSnapshotVersion(p.snapshotVersion());
        snapshot.setUpdatedAt(now);
        snapshotRepository.saveAndFlush(snapshot); // cần có row trước vì showtime_seats FK -> snapshot

        if (p.seats() == null || p.seats().isEmpty()) {
            return;
        }
        Map<UUID, ShowtimeSeatEntity> existing = new HashMap<>();
        seatRepository.findAllByShowtimeId(p.showtimeId()).forEach(s -> existing.put(s.getSeatId(), s));

        for (ShowtimeSnapshotPayload.Seat s : p.seats()) {
            ShowtimeSeatEntity seat = existing.get(s.seatId());
            if (seat == null) {
                seat = new ShowtimeSeatEntity();
                seat.setShowtimeId(p.showtimeId());
                seat.setSeatId(s.seatId());
                seat.setStatus(SEAT_AVAILABLE);
                seat.setCreatedAt(now); // version để null: @Version tự khởi tạo = 0
            } else if (!SEAT_AVAILABLE.equals(seat.getStatus())) {
                continue;
            }
            seat.setSeatLabel(s.seatLabel());
            seat.setSeatType(s.seatType());
            seat.setPriceMinor(s.priceMinor());
            seat.setUpdatedAt(now);
            seatRepository.save(seat);
        }
    }

    /**
     * ShowtimeCancelled: payload chỉ có showtimeId, snapshotVersion, reasonCode,
     * cancelledAt.
     */
    public void cancel(ShowtimeCancelledPayload p) {
        if (p.showtimeId() == null || p.snapshotVersion() <= 0) {
            throw new PoisonMessageException("ShowtimeCancelled thiếu showtimeId/snapshotVersion");
        }
        ShowtimeSnapshotEntity snapshot = snapshotRepository.findById(p.showtimeId()).orElse(null);
        if (snapshot == null) {
            LOG.warning(() -> "Nhận ShowtimeCancelled cho showtime chưa có snapshot: " + p.showtimeId());
            return;
        }
        if (snapshot.getSnapshotVersion() >= p.snapshotVersion()) {
            LOG.info(() -> "Bỏ qua ShowtimeCancelled cũ của showtime " + p.showtimeId());
            return;
        }
        snapshot.setStatus("CANCELLED");
        snapshot.setSnapshotVersion(p.snapshotVersion());
        snapshot.setUpdatedAt(Instant.now());
        snapshotRepository.save(snapshot);
    }

    private void validate(ShowtimeSnapshotPayload p) {
        if (p.showtimeId() == null || p.cinemaId() == null || p.auditoriumId() == null || p.movieId() == null
                || p.startsAt() == null || p.salesCloseAt() == null || p.currency() == null
                || p.snapshotVersion() <= 0) {
            throw new PoisonMessageException("Snapshot suất chiếu thiếu field bắt buộc");
        }
        if (p.status() != null && !VALID_STATUSES.contains(p.status())) {
            throw new PoisonMessageException("Status showtime không hợp lệ: " + p.status());
        }
        if (p.seats() != null) {
            for (var s : p.seats()) {
                if (s.seatId() == null || s.seatLabel() == null || s.seatType() == null || s.priceMinor() < 0) {
                    throw new PoisonMessageException("Ghế trong snapshot không hợp lệ");
                }
            }
        }
    }

    private static Instant parse(String iso) {
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException ex) {
            throw new PoisonMessageException("Thời gian không hợp lệ: " + iso, ex);
        }
    }
}