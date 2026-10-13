package com.cinema.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotNull Role role,
        @NotBlank @Size(max = 4000) String content) {

    public enum Role {
        USER,
        ASSISTANT
    }
}
