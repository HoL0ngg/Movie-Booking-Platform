package com.cinema.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.cinema.cinema.dto.CancelShowtimeRequest;
import com.cinema.cinema.dto.CreateShowtimeRequest;
import com.cinema.cinema.dto.ShowtimeResponse;
import com.cinema.cinema.exception.CinemaException;
import com.cinema.cinema.service.ShowtimeService;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AdminShowtimeTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.cinema.cinema.messaging.OutboxRelay outboxRelay;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-alpine3.24")
            .withDatabaseName("cinema_showtime_test")
            .withInitScript("db/schema.sql");

    private static final Jwt ADMIN = jwt("ADMIN");
    private static final Instant START = Instant.parse("2035-01-01T10:00:00Z");

    @Autowired
    private ShowtimeService showtimes;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;

    private UUID cinemaId;
    private UUID auditoriumId;
    private UUID seatId;

    @BeforeEach
    void seedRoom() {
        cinemaId = UUID.randomUUID();
        auditoriumId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cinemas (id, name, address, city, timezone, created_at, updated_at)
                VALUES (?, 'Schedule test', '1 Main St', 'Hanoi', 'Asia/Ho_Chi_Minh', now(), now())
                """, cinemaId);
        jdbc.update("""
                INSERT INTO auditoriums (id, cinema_id, name, created_at, updated_at)
                VALUES (?, ?, 'Screen 1', now(), now())
                """, auditoriumId, cinemaId);
        jdbc.update("""
                INSERT INTO seats (id, auditorium_id, row_label, seat_number, seat_type, is_active, created_at, updated_at)
                VALUES (?, ?, 'A', 1, 'STANDARD', true, now(), now())
                """, seatId, auditoriumId);
    }

    @Test
    void adminListIncludesDraftPublishedAndCancelledWithinCinema() {
        ShowtimeResponse draft = showtimes.create(ADMIN, request(auditoriumId, START));
        ShowtimeResponse published = showtimes.create(ADMIN, request(auditoriumId, START.plusSeconds(10800)));
        showtimes.publish(ADMIN, UUID.fromString(published.id()));
        ShowtimeResponse cancelled = showtimes.create(ADMIN, request(auditoriumId, START.plusSeconds(21600)));
        showtimes.cancel(ADMIN, UUID.fromString(cancelled.id()), new CancelShowtimeRequest("ADMIN_CANCELLED"));

        assertThat(showtimes.listForAdmin(ADMIN, cinemaId)).extracting(ShowtimeResponse::status)
                .containsExactly("DRAFT", "PUBLISHED", "CANCELLED");
        assertThat(showtimes.listForAdmin(ADMIN, null)).extracting(ShowtimeResponse::id)
                .contains(draft.id(), published.id(), cancelled.id());
        assertThat(showtimes.listByCinema(cinemaId, START.atZone(java.time.ZoneOffset.UTC).toLocalDate(), null))
                .extracting(ShowtimeResponse::id).containsExactly(published.id());
        assertThatThrownBy(() -> showtimes.listForAdmin(jwt("CINEMA_MANAGER"), null))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Administrator");
        assertThatThrownBy(() -> showtimes.get(UUID.fromString(draft.id())))
                .isInstanceOf(CinemaException.class).hasMessageContaining("not found");
    }

    @Test
    void publicScheduleAcrossCinemasUsesEachCinemaTimezoneAndFiltersByMovie() {
        UUID newYorkCinema = UUID.randomUUID();
        UUID newYorkRoom = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cinemas (id, name, address, city, timezone, created_at, updated_at)
                VALUES (?, 'Schedule test NY', '2 Main St', 'New York', 'America/New_York', now(), now())
                """, newYorkCinema);
        jdbc.update("""
                INSERT INTO auditoriums (id, cinema_id, name, created_at, updated_at)
                VALUES (?, ?, 'Screen 1', now(), now())
                """, newYorkRoom, newYorkCinema);
        jdbc.update("""
                INSERT INTO seats (id, auditorium_id, row_label, seat_number, seat_type, is_active, created_at, updated_at)
                VALUES (?, ?, 'A', 1, 'STANDARD', true, now(), now())
                """, UUID.randomUUID(), newYorkRoom);
        // 02:00Z is 09:00 on 1 Jan in Hanoi but 21:00 on 31 Dec in New York.
        Instant earlyUtc = Instant.parse("2035-01-01T02:00:00Z");
        ShowtimeResponse hanoi = showtimes.create(ADMIN, request(auditoriumId, earlyUtc));
        showtimes.publish(ADMIN, UUID.fromString(hanoi.id()));
        ShowtimeResponse newYork = showtimes.create(ADMIN, request(newYorkRoom, earlyUtc));
        showtimes.publish(ADMIN, UUID.fromString(newYork.id()));
        ShowtimeResponse draft = showtimes.create(ADMIN, request(auditoriumId, START));
        ShowtimeResponse cancelled = showtimes.create(ADMIN, request(auditoriumId, START.plusSeconds(10800)));
        showtimes.publish(ADMIN, UUID.fromString(cancelled.id()));
        showtimes.cancel(ADMIN, UUID.fromString(cancelled.id()), new CancelShowtimeRequest("ADMIN_CANCELLED"));

        java.time.LocalDate jan1 = java.time.LocalDate.of(2035, 1, 1);
        assertThat(showtimes.listPublished(jan1, null)).extracting(ShowtimeResponse::id)
                .contains(hanoi.id()).doesNotContain(newYork.id(), draft.id(), cancelled.id());
        assertThat(showtimes.listPublished(java.time.LocalDate.of(2034, 12, 31), null))
                .extracting(ShowtimeResponse::id).contains(newYork.id()).doesNotContain(hanoi.id());
        assertThat(showtimes.listPublished(jan1, UUID.fromString(hanoi.movieId())))
                .extracting(ShowtimeResponse::id, ShowtimeResponse::cinemaId, ShowtimeResponse::status)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(hanoi.id(), cinemaId.toString(), "PUBLISHED"));
        assertThat(showtimes.listPublished(jan1, UUID.randomUUID())).isEmpty();
    }

    @Test
    void onlyDraftsCanChangeAndAuditoriumIsImmutable() {
        ShowtimeResponse draft = showtimes.create(ADMIN, request(auditoriumId, START));
        UUID id = UUID.fromString(draft.id());
        CreateShowtimeRequest edited = request(auditoriumId, START.plusSeconds(3600));
        ShowtimeResponse result = showtimes.updateDraft(ADMIN, id, edited);
        assertThat(result.movieId()).isEqualTo(edited.movieId().toString());
        assertThat(result.startsAt()).isEqualTo(edited.startsAt());
        assertThat(result.price().amountMinor()).isEqualTo(edited.priceMinor());
        assertThatThrownBy(() -> showtimes.updateDraft(ADMIN, id, request(UUID.randomUUID(), START)))
                .isInstanceOf(CinemaException.class).hasMessageContaining("auditorium");
        showtimes.publish(ADMIN, id);
        assertThatThrownBy(() -> showtimes.updateDraft(ADMIN, id, request(auditoriumId, START)))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Only draft");
        showtimes.cancel(ADMIN, id, new CancelShowtimeRequest("ADMIN_CANCELLED"));
        assertThatThrownBy(() -> showtimes.updateDraft(ADMIN, id, request(auditoriumId, START)))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Only draft");
        assertThat(jdbc.queryForObject("SELECT starts_at FROM showtimes WHERE id = ?", java.sql.Timestamp.class, id)
                .toInstant()).isEqualTo(edited.startsAt());
    }

    @Test
    void invalidTimesRollbackAndPublicationOutboxIsIdempotent() {
        ShowtimeResponse draft = showtimes.create(ADMIN, request(auditoriumId, START));
        UUID id = UUID.fromString(draft.id());
        CreateShowtimeRequest invalid = new CreateShowtimeRequest(auditoriumId, UUID.randomUUID(), START,
                START, START, 100000L, "VND");
        assertThatThrownBy(() -> showtimes.create(ADMIN, invalid))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Invalid showtime times");
        assertThatThrownBy(() -> showtimes.updateDraft(ADMIN, id, invalid))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Invalid showtime times");
        assertThat(showtimes.listForAdmin(ADMIN, cinemaId)).hasSize(1);
        showtimes.publish(ADMIN, id);
        showtimes.publish(ADMIN, id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = 'ShowtimePublished'",
                Long.class, id)).isEqualTo(1);
        UUID overlapping = UUID.fromString(showtimes.create(ADMIN, request(auditoriumId, START.plusSeconds(1800))).id());
        assertThatThrownBy(() -> showtimes.publish(ADMIN, overlapping))
                .isInstanceOf(CinemaException.class).hasMessageContaining("overlaps");
        assertThat(jdbc.queryForObject("SELECT status FROM showtimes WHERE id = ?", String.class, overlapping))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE aggregate_id = ?", Long.class, overlapping))
                .isZero();
        showtimes.cancel(ADMIN, id, new CancelShowtimeRequest("ADMIN_CANCELLED"));
        showtimes.cancel(ADMIN, id, new CancelShowtimeRequest("ADMIN_CANCELLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = 'ShowtimeCancelled'",
                Long.class, id)).isEqualTo(1);
    }

    @Test
    void publishingWaitsForRoomLockAndReadsCommittedSeats() throws Exception {
        UUID id = UUID.fromString(showtimes.create(ADMIN, request(auditoriumId, START)).id());
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            Future<?> publication = new TransactionTemplate(transactions).execute(status -> {
                jdbc.queryForObject("SELECT id FROM auditoriums WHERE id = ? FOR UPDATE", UUID.class, auditoriumId);
                Future<?> future = worker.submit(() -> showtimes.publish(ADMIN, id));
                awaitRoomLockWait();
                jdbc.update("UPDATE seats SET seat_type = 'VIP' WHERE id = ?", seatId);
                return future;
            });
            publication.get(10, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT payload::text FROM outbox_events WHERE aggregate_id = ? AND event_type = 'ShowtimePublished'",
                String.class, id)).contains("VIP");
        assertThat(jdbc.queryForObject("SELECT status FROM showtimes WHERE id = ?", String.class, id))
                .isEqualTo("PUBLISHED");
    }

    private void awaitRoomLockWait() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Long waiting = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND pid <> pg_backend_pid()
                      AND wait_event_type = 'Lock' AND query LIKE '%auditoriums%'
                    """, Long.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new AssertionError("Publishing did not wait for the locked auditorium");
    }

    private static CreateShowtimeRequest request(UUID auditorium, Instant start) {
        return new CreateShowtimeRequest(auditorium, UUID.randomUUID(), start, start.plusSeconds(7200),
                start.minusSeconds(900), 100000L, "VND");
    }

    private static Jwt jwt(String role) {
        return Jwt.withTokenValue("test-only").header("alg", "none").subject(UUID.randomUUID().toString())
                .claim("roles", List.of(role)).build();
    }
}
