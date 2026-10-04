package com.cinema.auth.controller; // 6.1

import com.cinema.auth.dto.UserEmailResponse; // 6.2
import com.cinema.auth.exception.AuthException; // 6.3
import com.cinema.auth.service.AuthService; // 6.4
import java.nio.charset.StandardCharsets; // 6.5
import java.security.MessageDigest; // 6.6 So sánh hằng thời gian
import java.util.UUID; // 6.7
import org.springframework.beans.factory.annotation.Value; // 6.8
import org.springframework.http.HttpStatus; // 6.9
import org.springframework.web.bind.annotation.GetMapping; // 6.10
import org.springframework.web.bind.annotation.PathVariable; // 6.11
import org.springframework.web.bind.annotation.RequestHeader; // 6.12
import org.springframework.web.bind.annotation.RequestMapping; // 6.13
import org.springframework.web.bind.annotation.RestController; // 6.14

@RestController // 6.15
@RequestMapping("/internal/v1/users") // 6.16 Đường dẫn nội bộ, ngoài /api/v1
public class InternalUserController { // 6.17

    private final AuthService authService; // 6.18
    private final String internalToken; // 6.19

    public InternalUserController(AuthService authService, @Value("${auth.internal.token:}") String internalToken) { // 6.20
        this.authService = authService; // 6.21
        this.internalToken = internalToken; // 6.22
    }

    @GetMapping("/{userId}/email") // 6.23 GET /internal/v1/users/{userId}/email
    public UserEmailResponse email(@PathVariable UUID userId, // 6.24
                                   @RequestHeader(value = "X-Internal-Token", required = false) String token) { // 6.25 Header không bắt buộc ở tầng Spring, tự kiểm tra bên dưới
        boolean valid = !internalToken.isBlank() && token != null // 6.26 Chưa cấu hình token hoặc thiếu header → không hợp lệ
                && MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)); // 6.27 So sánh hằng thời gian
        if (!valid) { // 6.28
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required."); // 6.29
        }
        return new UserEmailResponse(authService.emailOf(userId)); // 6.30
    }
}