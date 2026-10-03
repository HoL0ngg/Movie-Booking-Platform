package com.cinema.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;
import com.cinema.cinema.repository.CinemaManagerRepository;

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
class CinemaServiceApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-alpine3.24")
            .withDatabaseName("cinema_cinema_test")
            .withInitScript("db/schema.sql");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CinemaManagerRepository cinemaManagers;

    @Test
    void contextLoadsWithOwnedDomainSchema() {
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
    }

    @Test
    void managerAssignmentsAreUniqueAndScopedToExistingCinemas() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID firstCinema = UUID.randomUUID();
        UUID secondCinema = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        for (UUID cinema : new UUID[]{firstCinema, secondCinema}) {
            jdbc.update("""
                    INSERT INTO cinemas (id, name, address, city, timezone, created_at, updated_at)
                    VALUES (?, 'Manager test', '1 Main St', 'Hanoi', 'Asia/Ho_Chi_Minh', now(), now())
                    """, cinema);
        }
        String assign = "INSERT INTO cinema_managers (cinema_id, user_id, assigned_at) VALUES (?, ?, now())";
        jdbc.update(assign, firstCinema, user);
        jdbc.update(assign, secondCinema, user);
        assertThat(cinemaManagers.existsByCinemaIdAndUserId(firstCinema, user)).isTrue();
        assertThat(cinemaManagers.existsByCinemaIdAndUserId(secondCinema, user)).isTrue();
        assertThat(cinemaManagers.existsByCinemaIdAndUserId(firstCinema, UUID.randomUUID())).isFalse();
        assertThatThrownBy(() -> jdbc.update(assign, firstCinema, user))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(assign, UUID.randomUUID(), user))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM cinemas WHERE id = ?", firstCinema))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void publishedShowtimesCannotOverlapInOneAuditorium() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID cinema = UUID.randomUUID();
        UUID auditorium = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cinemas (id, name, address, city, timezone, created_at, updated_at)
                VALUES (?, 'Test', '1 Main St', 'Hanoi', 'Asia/Ho_Chi_Minh', now(), now())
                """, cinema);
        jdbc.update("""
                INSERT INTO auditoriums (id, cinema_id, name, created_at, updated_at)
                VALUES (?, ?, 'Screen 1', now(), now())
                """, auditorium, cinema);
        String insert = """
                INSERT INTO showtimes
                  (id, auditorium_id, movie_id, starts_at, ends_at, sales_close_at,
                   price_minor, currency, status, snapshot_version, created_at, updated_at)
                VALUES (?, ?, ?, now() + interval '1 day', now() + interval '1 day 2 hours',
                        now() + interval '20 hours', 100000, 'VND', 'PUBLISHED', 1, now(), now())
                """;
        jdbc.update(insert, UUID.randomUUID(), auditorium, UUID.randomUUID());
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), auditorium, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
