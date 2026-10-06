package com.cinema.cinema.dto; // 7.30

import jakarta.validation.constraints.NotBlank; // 7.31
import jakarta.validation.constraints.Size; // 7.32

public record CancelShowtimeRequest(@NotBlank @Size(max = 64) String reasonCode) { // 7.33 Lý do hủy, đưa vào event
}