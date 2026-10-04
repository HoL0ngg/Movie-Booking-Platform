package com.cinema.auth.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinema.auth.dto.ApiError;
import com.cinema.auth.filter.TraceIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", safeMessage(error.getDefaultMessage())))
                .toList();
        return badRequest(request, Map.of("violations", violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        List<Map<String, String>> violations = exception.getConstraintViolations().stream()
                .map(error -> Map.of("field", error.getPropertyPath().toString(), "message", error.getMessage()))
                .toList();
        return badRequest(request, Map.of("violations", violations));
    }

    @ExceptionHandler(AuthException.class) // 13.12 Bắt AuthException từ mọi controller
    ResponseEntity<ApiError> handleAuth(AuthException exception, HttpServletRequest request) { // 13.13
        ApiError error = new ApiError(exception.getCode(), exception.getMessage(), // 13.14 Body lỗi chuẩn
                traceId(request), Instant.now(), Map.of()); // 13.15 traceId() là method private có sẵn
        return ResponseEntity.status(exception.getStatus()).body(error); // 13.16 Trả đúng HTTP status
    }

    private ResponseEntity<ApiError> badRequest(HttpServletRequest request, Map<String, Object> details) {
        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                "The request is invalid.",
                traceId(request),
                Instant.now(),
                details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        return traceId == null ? "unavailable" : traceId.toString();
    }

    private String safeMessage(String message) {
        return message == null ? "Invalid value" : message;
    }

}

