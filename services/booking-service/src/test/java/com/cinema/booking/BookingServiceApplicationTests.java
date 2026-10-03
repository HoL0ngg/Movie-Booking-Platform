package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class BookingServiceApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-alpine3.24")
            .withDatabaseName("cinema_booking_test")
            .withInitScript("db/schema.sql");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsWithOwnedDomainSchema() {
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
    }

    @Test
    void seatLockAndUniqueSaleAreEnforcedByPostgres() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID showtime = UUID.randomUUID();
        UUID seat = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID reservation = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO showtime_snapshots
                  (showtime_id, cinema_id, auditorium_id, movie_id, starts_at, sales_close_at,
                   currency, status, snapshot_version, created_at, updated_at)
                VALUES (?, ?, ?, ?, now() + interval '1 day', now() + interval '20 hours',
                        'VND', 'PUBLISHED', 1, now(), now())
                """, showtime, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("""
                INSERT INTO showtime_seats
                  (showtime_id, seat_id, seat_label, seat_type, price_minor, status,
                   version, created_at, updated_at)
                VALUES (?, ?, 'A1', 'STANDARD', 100000, 'AVAILABLE', 0, now(), now())
                """, showtime, seat);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE showtime_seats SET status = 'SOLD' WHERE showtime_id = ? AND seat_id = ?",
                showtime, seat)).isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("""
                INSERT INTO reservations
                  (id, showtime_id, user_id, status, hold_expires_at, created_at, updated_at)
                VALUES (?, ?, ?, 'HELD', now() + interval '10 minutes', now(), now())
                """, reservation, showtime, user);

        try (Connection first = dataSource.getConnection(); Connection second = dataSource.getConnection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            String lockSql = "SELECT status FROM showtime_seats WHERE showtime_id = ? AND seat_id = ? FOR UPDATE";
            try (PreparedStatement lock = first.prepareStatement(lockSql)) {
                lock.setObject(1, showtime);
                lock.setObject(2, seat);
                try (var result = lock.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("AVAILABLE");
                }
            }
            try (PreparedStatement hold = first.prepareStatement("""
                    UPDATE showtime_seats
                    SET status = 'HELD', reservation_id = ?, hold_expires_at = now() + interval '10 minutes',
                        version = version + 1, updated_at = now()
                    WHERE showtime_id = ? AND seat_id = ?
                    """)) {
                hold.setObject(1, reservation);
                hold.setObject(2, showtime);
                hold.setObject(3, seat);
                assertThat(hold.executeUpdate()).isEqualTo(1);
            }
            try (Statement timeout = second.createStatement()) {
                timeout.execute("SET LOCAL lock_timeout = '250ms'");
            }
            assertThatThrownBy(() -> {
                try (PreparedStatement blocked = second.prepareStatement(lockSql)) {
                    blocked.setObject(1, showtime);
                    blocked.setObject(2, seat);
                    blocked.executeQuery();
                }
            }).isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("55P03"));
            second.rollback();
            first.commit();
        }
        assertThat(jdbc.queryForObject(
                "SELECT status FROM showtime_seats WHERE showtime_id = ? AND seat_id = ?",
                String.class, showtime, seat)).isEqualTo("HELD");

        UUID booking = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bookings
                  (id, reservation_id, showtime_id, user_id, payment_id, amount_minor,
                   currency, status, confirmed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 100000, 'VND', 'CONFIRMED', now(), now(), now())
                """, booking, reservation, showtime, user, UUID.randomUUID());
        jdbc.update("""
                UPDATE showtime_seats SET status = 'SOLD', booking_id = ?,
                  hold_expires_at = NULL, version = version + 1, updated_at = now()
                WHERE showtime_id = ? AND seat_id = ?
                """, booking, showtime, seat);
        jdbc.update("UPDATE reservations SET status = 'CONFIRMED', updated_at = now() WHERE id = ?", reservation);
        jdbc.update("""
                INSERT INTO booking_items (id, booking_id, showtime_id, seat_id, price_minor, created_at)
                VALUES (?, ?, ?, ?, 100000, now())
                """, UUID.randomUUID(), booking, showtime, seat);
        UUID secondReservation = UUID.randomUUID();
        UUID secondBooking = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reservations
                  (id, showtime_id, user_id, status, hold_expires_at, created_at, updated_at)
                VALUES (?, ?, ?, 'RELEASED', now() + interval '10 minutes', now(), now())
                """, secondReservation, showtime, UUID.randomUUID());
        jdbc.update("""
                INSERT INTO bookings
                  (id, reservation_id, showtime_id, user_id, payment_id, amount_minor,
                   currency, status, created_at, updated_at)
                SELECT ?, id, showtime_id, user_id, ?, 100000, 'VND', 'CANCELLED', now(), now()
                FROM reservations WHERE id = ?
                """, secondBooking, UUID.randomUUID(), secondReservation);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO booking_items (id, booking_id, showtime_id, seat_id, price_minor, created_at)
                VALUES (?, ?, ?, ?, 100000, now())
                """, UUID.randomUUID(), secondBooking, showtime, seat))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
