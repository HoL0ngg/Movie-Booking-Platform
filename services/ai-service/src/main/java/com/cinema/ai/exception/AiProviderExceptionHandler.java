package com.cinema.ai.exception;

import java.time.Instant;
import java.util.Map;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.cinema.ai.controller.ChatController;
import com.cinema.ai.filter.TraceIdFilter;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatController.class)
public class AiProviderExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiProviderExceptionHandler.class);

    @ExceptionHandler({ApiException.class, GenAiIOException.class})
    public ResponseEntity<Map<String, Object>> providerUnavailable(HttpServletRequest request, Exception exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        log.warn("AI provider unavailable: traceId={} exceptionType={} providerStatus={}",
                request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE), cause.getClass().getSimpleName(),
                cause instanceof ApiException apiException ? apiException.code() : "unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "AI_PROVIDER_UNAVAILABLE",
                "message", "Cinemate đang tạm bận hoặc không kết nối được với Gemini. Vui lòng thử lại sau.",
                "traceId", String.valueOf(request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)),
                "timestamp", Instant.now().toString()));
    }
}
