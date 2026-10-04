package com.cinema.auth.dto; // 7.12

import jakarta.validation.constraints.NotBlank; // 7.13

public record RefreshRequest(@NotBlank String refreshToken) { // 7.14 Dùng chung cho refresh và logout
}