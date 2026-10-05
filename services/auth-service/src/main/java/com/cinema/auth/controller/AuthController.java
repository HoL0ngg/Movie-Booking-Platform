package com.cinema.auth.controller; // 4.1

import java.util.UUID; // 4.2

import org.springframework.http.HttpStatus; // 4.3
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 4.4
import org.springframework.security.oauth2.jwt.Jwt; // 4.5
import org.springframework.web.bind.annotation.PostMapping; // 4.6
import org.springframework.web.bind.annotation.RequestBody; // 4.7 Kích hoạt validation DTO
import org.springframework.web.bind.annotation.RequestMapping; // 4.8
import org.springframework.web.bind.annotation.ResponseStatus; // 4.9
import org.springframework.web.bind.annotation.RestController; // 4.10 Lấy principal hiện tại

import com.cinema.auth.dto.LoginRequest; // 4.11 Token đã verify
import com.cinema.auth.dto.OtpChallengeResponse;
import com.cinema.auth.dto.OtpVerifyRequest;
import com.cinema.auth.dto.RefreshRequest; // 4.12
import com.cinema.auth.dto.RegisterRequest; // 4.13
import com.cinema.auth.dto.TokenResponse; // 4.14
import com.cinema.auth.service.AuthService; // 4.15

import jakarta.validation.Valid; // 4.16

@RestController // 4.17 Controller trả JSON
@RequestMapping("/api/v1/auth") // 4.18 Prefix chung
public class AuthController { // 4.19

    private final AuthService authService; // 4.20 Logic ở service, controller mỏng

    public AuthController(AuthService authService) { // 4.21 Constructor injection
        this.authService = authService; // 4.22
    }

    @PostMapping("/register/otp")
    @ResponseStatus(HttpStatus.ACCEPTED) // 202: đã nhận, chờ xác minh
    public OtpChallengeResponse registerOtp(@Valid @RequestBody RegisterRequest request) {
        return authService.registerRequestOtp(request);
    }

    @PostMapping("/register/otp/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registerVerify(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.registerVerifyOtp(request);
    }

    @PostMapping("/login") // 4.27
    public TokenResponse login(@Valid @RequestBody LoginRequest request) { // 4.28
        return authService.login(request); // 4.29
    }

    @PostMapping("/refresh") // 4.30
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) { // 4.31
        return authService.refresh(request); // 4.32
    }

    @PostMapping("/logout") // 4.33
    @ResponseStatus(HttpStatus.NO_CONTENT) // 4.34 Trả 204, không body
    public void logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RefreshRequest request) { // 4.35 jwt = access token đã verify
        authService.logout(UUID.fromString(jwt.getSubject()), request.refreshToken()); // 4.36 subject = userId
    }
}