package com.cinema.payment.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cinema.payment.common.JsonUtils;
import com.cinema.payment.dto.ReservationDto;
import com.cinema.payment.exception.ApiException;

/**
 * Lấy reservation từ booking-service bằng chính JWT của người dùng:
 * booking-service chỉ trả reservation
 * của chủ sở hữu, nên đồng thời kiểm tra quyền sở hữu và cho ra số tiền đáng
 * tin (không tin số tiền từ client).
 */

@Component
public class BookingClient {
    private static final Logger LOG = Logger.getLogger(BookingClient.class.getName());
    private final HttpClient http;
    private final String baseUrl;
    private final Duration timeout;

    public BookingClient(HttpClient paymentHttpClient,
            @Value("${payment.booking.base-url}") String baseUrl,
            @Value("${payment.booking.timeout:PT5S}") Duration timeout) {
        this.http = paymentHttpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
    }

    public ReservationSnapshot getReservation(UUID reservationId, String bearerToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/reservations/" + reservationId))
                .timeout(timeout)
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        }

        int status = response.statusCode();
        if (status == 404) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Không tìm thấy reservation");
        }
        if (status == 401 || status == 403) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Token không được booking-service chấp nhận");
        }
        if (status != 200) {
            LOG.warning("booking-service trả HTTP " + status + " khi lấy reservation " + reservationId);
            throw unavailable(null);
        }
        try {
            ReservationDto dto = JsonUtils.fromJson(response.body(), ReservationDto.class);
            return new ReservationSnapshot(dto.reservationId(), dto.status(), Instant.parse(dto.holdExpiresAt()),
                    dto.totalMinor());
        } catch (IllegalStateException | DateTimeParseException | NullPointerException e) {
            LOG.log(Level.WARNING, "Phản hồi reservation không hợp lệ", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "BOOKING_BAD_RESPONSE",
                    "Phản hồi từ booking-service không hợp lệ");
        }
    }

    private ApiException unavailable(Throwable cause) {
        if (cause != null) {
            LOG.log(Level.WARNING, "Không gọi được booking-service", cause);
        }
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "BOOKING_UNAVAILABLE",
                "booking-service tạm thời không khả dụng");
    }

}
