package com.cinema.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.AdditionalAnswers.delegatesTo;

import java.util.UUID;
import java.util.List;
import java.sql.Connection;
import com.cinema.cinema.dto.PhysicalSeatDto;
import com.cinema.cinema.dto.SaveCinemaRequest;
import com.cinema.cinema.dto.SaveAuditoriumRequest;
import com.cinema.cinema.dto.SaveSeatLayoutRequest;
import com.cinema.cinema.service.CinemaManagementService;
import com.cinema.cinema.repository.CinemaRepository;
import com.cinema.cinema.repository.AuditoriumRepository;
import com.cinema.cinema.repository.SeatRepository;
import com.cinema.cinema.repository.ShowtimeRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import com.cinema.cinema.repository.CinemaManagerRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "auth.jwt.secret=cinema-test-only-secret-at-least-32-bytes")
@ActiveProfiles("test")
class CinemaServiceApplicationTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.cinema.cinema.messaging.OutboxRelay outboxRelay;


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

    @Autowired
    private CinemaManagementService management;

    @Autowired private CinemaRepository cinemas;
    @Autowired private AuditoriumRepository auditoriums;
    @Autowired private SeatRepository seats;
    @Autowired private ShowtimeRepository showtimes;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactions;

    @Test
    void manualSeatUpgradePreservesLegacyRowsAndEnforcesPairConstraints() throws Exception {
        String schema = "seat_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection()) {
            String originalSchema = connection.getSchema();
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE SCHEMA " + schema);
            try {
                connection.setSchema(schema);
                jdbc.execute("""
                        CREATE TABLE seats (
                          id uuid NOT NULL PRIMARY KEY,
                          auditorium_id uuid NOT NULL,
                          row_label varchar(12) NOT NULL,
                          seat_number integer NOT NULL CHECK (seat_number > 0),
                          seat_type varchar(24) NOT NULL,
                          is_active boolean NOT NULL,
                          created_at timestamptz NOT NULL,
                          updated_at timestamptz NOT NULL,
                          UNIQUE (auditorium_id, row_label, seat_number)
                        )
                        """);
                UUID room = UUID.randomUUID();
                UUID standard = UUID.randomUUID();
                UUID couple = UUID.randomUUID();
                UUID otherRoomSeat = UUID.randomUUID();
                String insert = """
                        INSERT INTO seats (id, auditorium_id, row_label, seat_number, seat_type,
                                           is_active, created_at, updated_at)
                        VALUES (?, ?, 'A', ?, ?, true, now(), now())
                        """;
                jdbc.update(insert, standard, room, 1, "STANDARD");
                jdbc.update(insert, couple, room, 2, "COUPLE");
                jdbc.update(insert, otherRoomSeat, UUID.randomUUID(), 1, "VIP");
                String originalColumns = "id, auditorium_id, row_label, seat_number, seat_type, is_active, created_at, updated_at";
                var before = jdbc.queryForList("SELECT " + originalColumns + " FROM seats ORDER BY id");

                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/admin-seat-layout-upgrade.sql"));

                assertThat(jdbc.queryForList("SELECT " + originalColumns + " FROM seats ORDER BY id")).isEqualTo(before);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM seats WHERE paired_seat_id IS NULL", Long.class))
                        .isEqualTo(3);
                assertThatThrownBy(() -> jdbc.update("UPDATE seats SET seat_type = 'INVALID' WHERE id = ?", standard))
                        .isInstanceOf(DataIntegrityViolationException.class);
                assertThatThrownBy(() -> jdbc.update("UPDATE seats SET paired_seat_id = ? WHERE id = ?", otherRoomSeat, couple))
                        .isInstanceOf(DataIntegrityViolationException.class);
                assertThatThrownBy(() -> jdbc.update("UPDATE seats SET paired_seat_id = id WHERE id = ?", couple))
                        .isInstanceOf(DataIntegrityViolationException.class);
                assertThat(jdbc.queryForList("SELECT " + originalColumns + " FROM seats ORDER BY id")).isEqualTo(before);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM seats WHERE paired_seat_id IS NULL", Long.class))
                        .isEqualTo(3);
            } finally {
                // Roll back a failed upgrade before cleaning up only this disposable test schema.
                jdbc.execute("ROLLBACK");
                connection.setSchema(originalSchema);
                jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void staleIdReadCannotMoveASeatBetweenRooms() {
        UUID cinema = UUID.fromString(management.saveCinema(null,
                new SaveCinemaRequest("ID collision", "1 Main St", "Hanoi", null)).id());
        UUID source = management.createAuditorium(cinema, new SaveAuditoriumRequest("Source")).id();
        UUID target = management.createAuditorium(cinema, new SaveAuditoriumRequest("Target")).id();
        UUID id = UUID.randomUUID();
        PhysicalSeatDto original = new PhysicalSeatDto(id, source, "A", 1, "STANDARD", true, null);
        management.saveSeats(source, new SaveSeatLayoutRequest(List.of(original)));
        SeatRepository staleRead = mock(SeatRepository.class, delegatesTo(seats));
        // Model the read occurring before another room commits this UUID; writes use real PostgreSQL.
        doReturn(List.of()).when(staleRead).findAllById(List.of(id));
        CinemaManagementService racing = new CinemaManagementService(cinemas, auditoriums, staleRead, showtimes, entityManager);
        PhysicalSeatDto attempted = new PhysicalSeatDto(id, target, "B", 2, "VIP", true, null);
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status ->
                racing.saveSeats(target, new SaveSeatLayoutRequest(List.of(attempted)))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(management.getSeats(source)).containsExactly(original);
        assertThat(management.getSeats(target)).isEmpty();
    }

    @Test
    void physicalLayoutPersistsPairsAndReplacementAndLocksAfterAnyShowtime() {
        UUID cinema = UUID.fromString(management.saveCinema(null,
                new SaveCinemaRequest("Layout test", "1 Main St", "Hanoi", null)).id());
        UUID room = management.createAuditorium(cinema, new SaveAuditoriumRequest("Screen 1")).id();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<PhysicalSeatDto> pair = List.of(
                new PhysicalSeatDto(first, room, "A", 1, "COUPLE", true, second),
                new PhysicalSeatDto(second, room, "A", 2, "COUPLE", true, first));
        management.saveSeats(room, new SaveSeatLayoutRequest(pair));
        assertThat(management.getSeats(room)).containsExactlyElementsOf(pair);
        List<PhysicalSeatDto> replacement = List.of(new PhysicalSeatDto(first, room, "B", 3, "VIP", false, null));
        management.saveSeats(room, new SaveSeatLayoutRequest(replacement));
        assertThat(management.getSeats(room)).containsExactlyElementsOf(replacement);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO showtimes
                  (id, auditorium_id, movie_id, starts_at, ends_at, sales_close_at,
                   price_minor, currency, status, snapshot_version, created_at, updated_at)
                VALUES (?, ?, ?, now() + interval '1 day', now() + interval '1 day 2 hours',
                        now() + interval '20 hours', 100000, 'VND', 'DRAFT', 1, now(), now())
                """, UUID.randomUUID(), room, UUID.randomUUID());
        assertThatThrownBy(() -> management.saveSeats(room, new SaveSeatLayoutRequest(List.of())))
                .hasMessageContaining("cannot change");
        assertThat(management.getSeats(room)).containsExactlyElementsOf(replacement);
        assertThat(jdbc.queryForObject("select count(*) from seats where auditorium_id = ?", Long.class, room)).isEqualTo(1);
    }

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
