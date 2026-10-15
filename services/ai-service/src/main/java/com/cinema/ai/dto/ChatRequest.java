package com.cinema.ai.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 30) List<@Valid ChatMessage> history) {

    public ChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
