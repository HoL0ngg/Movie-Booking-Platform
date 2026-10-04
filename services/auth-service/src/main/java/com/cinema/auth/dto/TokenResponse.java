package com.cinema.auth.dto; // 7.15

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) { // 7.16 expiresIn tính bằng giây
}