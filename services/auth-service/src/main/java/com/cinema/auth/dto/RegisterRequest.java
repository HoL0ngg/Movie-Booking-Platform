package com.cinema.auth.dto; // 7.1

import jakarta.validation.constraints.Email; // 7.2
import jakarta.validation.constraints.NotBlank; // 7.3
import jakarta.validation.constraints.Size; // 7.4

public record RegisterRequest( // 7.5 Record = DTO bất biến
        @NotBlank @Email @Size(max = 254) String email, // 7.6 Bắt buộc, đúng định dạng email, tối đa 254 ký tự
        @NotBlank @Size(min = 8, max = 72) String password) { // 7.7 8–72 ký tự (BCrypt giới hạn 72 byte)
}