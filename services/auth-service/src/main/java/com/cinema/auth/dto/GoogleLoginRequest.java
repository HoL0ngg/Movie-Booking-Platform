package com.cinema.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank @Size(max = 4096) String idToken) { // ID token (JWT) do Google cấp cho frontend; giới hạn kích thước chống payload rác
}