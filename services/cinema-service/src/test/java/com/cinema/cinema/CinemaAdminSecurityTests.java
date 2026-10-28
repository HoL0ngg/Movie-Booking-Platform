package com.cinema.cinema;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.cinema.cinema.config.SecurityConfiguration;
import com.cinema.cinema.controller.CinemaAdminController;
import com.cinema.cinema.exception.ApiExceptionHandler;
import com.cinema.cinema.service.CinemaManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.web.FilterChainProxy;

class CinemaAdminSecurityTests {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private CinemaManagementService service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();
        service = context.getBean(CinemaManagementService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @AfterEach
    void close() {
        context.close();
    }

    @Test
    void rejectsAnonymousAndNonAdminAtServiceBoundary() throws Exception {
        mvc.perform(get("/api/v1/admin/cinemas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/cinemas").header("Authorization", "Bearer user"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void adminCanReadAndInvalidLayoutsNeverReachService() throws Exception {
        when(service.listCinemas()).thenReturn(List.of());
        mvc.perform(get("/api/v1/admin/cinemas").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
        UUID room = UUID.randomUUID();
        for (String value : List.of("not-json", "{\"seats\":null}", "{\"seats\":[]}", "{\"seats\":[null]}",
                "{\"seats\":[{\"id\":\"invalid\"}]}",
                "{\"seats\":[{\"id\":\"" + UUID.randomUUID() + "\",\"auditoriumId\":\"" + room
                        + "\",\"rowLabel\":\"AA\",\"seatNumber\":31,\"seatType\":\"OTHER\",\"isActive\":true}]}")) {
            mvc.perform(put("/api/v1/admin/auditoriums/{id}/seats", room)
                    .header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(value))
                    .andExpect(status().isBadRequest());
        }
        verify(service).listCinemas();
        verifyNoMoreInteractions(service);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfiguration.class, CinemaAdminController.class, ApiExceptionHandler.class})
    static class TestConfiguration {
        @Bean
        CinemaManagementService service() { return mock(CinemaManagementService.class); }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token).header("alg", "HS256").subject(UUID.randomUUID().toString())
                    .claim("roles", List.of("admin".equals(token) ? "ADMIN" : "USER"))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        }
    }
}
