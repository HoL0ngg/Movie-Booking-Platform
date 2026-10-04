package com.cinema.auth.controller; // 5.1

import com.cinema.auth.dto.MeResponse; // 5.2
import com.cinema.auth.service.AuthService; // 5.3
import java.util.UUID; // 5.4
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 5.5
import org.springframework.security.oauth2.jwt.Jwt; // 5.6
import org.springframework.web.bind.annotation.GetMapping; // 5.7
import org.springframework.web.bind.annotation.RestController; // 5.8

@RestController // 5.9
public class MeController { // 5.10

    private final AuthService authService; // 5.11

    public MeController(AuthService authService) { // 5.12
        this.authService = authService; // 5.13
    }

    @GetMapping("/api/v1/me") // 5.14 GET /api/v1/me
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) { // 5.15
        return authService.me(UUID.fromString(jwt.getSubject())); // 5.16
    }
}