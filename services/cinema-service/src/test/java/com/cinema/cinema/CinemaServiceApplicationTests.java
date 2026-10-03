package com.cinema.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;
import com.cinema.cinema.repository.CinemaManagerRepository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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
            .withDatabaseName("cinema_cinema_test");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CinemaManagerRepository cinemaManagers;

    @Test
    void contextLoadsAndAppliesOwnedDomainSchema() {
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
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
    void managerMigrationUpgradesV2WithoutChangingExistingCinemaData() {
        new JdbcTemplate(dataSource).execute("CREATE DATABASE cinema_manager_upgrade");
        DataSource upgradeDataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl().replace("/cinema_cinema_test", "/cinema_manager_upgrade"),
                POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(upgradeDataSource).target("2").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(upgradeDataSource);
        UUID cinema = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cinemas (id, name, address, city, timezone, created_at, updated_at)
                VALUES (?, 'Existing cinema', '1 Main St', 'Hanoi', 'Asia/Ho_Chi_Minh', now(), now())
                """, cinema);
        Flyway upgrade = Flyway.configure().dataSource(upgradeDataSource).load();
        upgrade.migrate();
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(jdbc.queryForObject("SELECT name FROM cinemas WHERE id = ?", String.class, cinema))
                .isEqualTo("Existing cinema");
        jdbc.update("INSERT INTO cinema_managers (cinema_id, user_id, assigned_at) VALUES (?, ?, now())",
                cinema, UUID.randomUUID());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cinema_managers", Long.class)).isEqualTo(1L);
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
