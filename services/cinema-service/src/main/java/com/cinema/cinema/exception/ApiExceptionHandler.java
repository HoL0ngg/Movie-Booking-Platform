package com.cinema.cinema.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.cinema.cinema.dto.ApiError;
import com.cinema.cinema.filter.TraceIdFilter;

import jakarta.servlet.http.HttpServletRequest; // 16.12
import jakarta.validation.ConstraintViolationException; // 16.13

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

    @ExceptionHandler(CinemaException.class) // 16.14 Bắt CinemaException
    ResponseEntity<ApiError> handleCinema(CinemaException exception, HttpServletRequest request) { // 16.15
        ApiError error = new ApiError(exception.getCode(), exception.getMessage(), // 16.16
                traceId(request), Instant.now(), Map.of()); // 16.17
        return ResponseEntity.status(exception.getStatus()).body(error); // 16.18
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class) // 16.19 UUID/ngày sai định dạng
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) { // 16.20
        return badRequest(request, Map.of("field", exception.getName())); // 16.21 Dùng lại badRequest() có sẵn
    }

    @ExceptionHandler(MissingServletRequestParameterException.class) // 16.22 Thiếu tham số bắt buộc (vd date)
    ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException exception, HttpServletRequest request) { // 16.23
        return badRequest(request, Map.of("field", exception.getParameterName())); // 16.24
    }

    private ResponseEntity<ApiError> badRequest(HttpServletRequest request, Map<String, Object> details) {
        ApiError error = new ApiError(
                "VALIDATION_ERROR", "The request is invalid.", traceId(request), Instant.now(), details);
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

