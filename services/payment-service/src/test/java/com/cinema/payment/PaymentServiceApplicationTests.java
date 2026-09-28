package com.cinema.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
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
class PaymentServiceApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-alpine3.24")
            .withDatabaseName("cinema_payment_test");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsAndAppliesOwnedDomainSchema() {
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @Test
    void duplicateProviderEventCannotBeRecordedTwice() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID payment = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payments
                  (id, booking_id, reservation_id, user_id, amount_minor, currency,
                   request_key, request_hash, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 100000, 'VND', ?, decode('a1', 'hex'), 'REQUESTED', now(), now())
                """, payment, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID().toString());
        jdbc.update("""
                INSERT INTO provider_events
                  (id, payment_id, provider, event_identity, result_code, received_at)
                VALUES (?, ?, 'test-provider', 'event-1', 'SUCCESS', now())
                """, UUID.randomUUID(), payment);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO provider_events
                  (id, payment_id, provider, event_identity, result_code, received_at)
                VALUES (?, ?, 'test-provider', 'event-1', 'SUCCESS', now())
                """, UUID.randomUUID(), payment)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM provider_events WHERE payment_id = ?",
                Integer.class, payment)).isEqualTo(1);
    }
}
