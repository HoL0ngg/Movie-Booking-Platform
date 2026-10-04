package com.cinema.auth.dto; // 7.8

import jakarta.validation.constraints.Email; // 7.9
import jakarta.validation.constraints.NotBlank; // 7.10

public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { // 7.11 Login không kiểm độ dài mật khẩu
}