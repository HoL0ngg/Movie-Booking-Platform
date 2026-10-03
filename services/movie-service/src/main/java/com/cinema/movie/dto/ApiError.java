package com.cinema.movie.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(String code, String message, String traceId, Instant timestamp, Map<String, Object> details) {
}
