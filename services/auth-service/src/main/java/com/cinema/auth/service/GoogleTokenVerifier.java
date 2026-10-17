package com.cinema.auth.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.cinema.auth.exception.AuthException;

@Component
public class GoogleTokenVerifier {

    public record GoogleProfile(String sub, String email, boolean emailVerified) {} // dữ liệu ta cần từ token

    private static final String JWK_URI = "https://www.googleapis.com/oauth2/v3/certs"; // khóa công khai Google để kiểm chữ ký
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com"); // Google dùng cả 2 dạng

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${auth.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWK_URI).build(); // [G1] tự tải + cache khóa, kiểm chữ ký RS256
        OAuth2TokenValidator<Jwt> issuer = jwt -> ISSUERS.contains(jwt.getClaimAsString("iss")) // [G2] đúng nơi phát hành
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer"));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId) // [G3] token phải cấp cho app của ta
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_audience"));
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), issuer, audience)); // [G4] gộp: hạn dùng + issuer + audience
    }

    public GoogleProfile verify(String idToken) {
        if (clientId == null || clientId.isBlank()) { // thiếu cấu hình → lỗi server, không phải lỗi người dùng
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_NOT_CONFIGURED", "Google sign-in is not configured.");
        }
        try {
            Jwt jwt = decoder.decode(idToken); // [G5] ném JwtException nếu sai chữ ký/hết hạn/sai iss/aud
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Google token is invalid.");
            }
            return new GoogleProfile(jwt.getSubject(), email, Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))); // [G6]
        } catch (JwtException ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Google token is invalid.");
        }
    }
}