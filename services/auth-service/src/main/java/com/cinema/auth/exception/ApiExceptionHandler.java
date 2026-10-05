package com.cinema.auth.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinema.auth.dto.ApiError;
import com.cinema.auth.filter.TraceIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    @ExceptionHandler(Exception.class) // lỗi không lường trước → 500 JSON thật, không bị forward sang /error rồi hóa 401
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        if (exception instanceof ErrorResponse spring) { // 400/404/405/415... của chính Spring MVC: giữ nguyên status
            HttpStatus status = HttpStatus.resolve(spring.getStatusCode().value());
            ApiError error = new ApiError("REQUEST_ERROR", "The request could not be processed.",
                    traceId(request), Instant.now(), Map.of());
            return ResponseEntity.status(status != null ? status : HttpStatus.BAD_REQUEST).body(error);
        }
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        ApiError error = new ApiError("INTERNAL_ERROR", "Something went wrong. Please try again.",
                traceId(request), Instant.now(), Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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