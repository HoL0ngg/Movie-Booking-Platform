package com.cinema.gateway;

import com.cinema.gateway.config.SecurityConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

class CinemaRouteSecurityTests {
    private AnnotationConfigApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @AfterEach
    void close() {
        context.close();
    }

    @Test
    void forwardsCinemaAdminRequestsAndPreservesBearerForOwnerAuthorization() {
        for (String path : new String[] {"/api/v1/admin/cinemas", "/api/v1/admin/cinemas/room/auditoriums",
                "/api/v1/admin/auditoriums/room/seats", "/api/v1/admin/showtimes",
                "/api/v1/admin/showtimes/slot"}) {
            for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT}) {
                client.method(method).uri(path).header("Authorization", "Bearer test-admin-token")
                        .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("Bearer test-admin-token");
            }
        }
    }

    @Test
    void permitsPublicScheduleReadsAndKeepsUnknownAdminRoutesDenied() {
        client.get().uri("/api/v1/showtimes?date=2026-10-31").exchange().expectStatus().isOk();
        client.get().uri("/api/v1/cinemas/cinema/showtimes?date=2026-10-31")
                .exchange().expectStatus().isOk();
        client.get().uri("/api/v1/admin/users").exchange().expectStatus().isUnauthorized();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    @EnableWebFluxSecurity
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {
        @Bean
        RouterFunction<ServerResponse> downstream() {
            // Echo after the real gateway security chain; owner authorization is tested in cinema-service.
            return RouterFunctions.route(request -> true, request -> ServerResponse.ok()
                    .bodyValue(request.headers().firstHeader("Authorization") == null ? "public"
                            : request.headers().firstHeader("Authorization")));
        }
    }
}
