package com.cinema.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAuditoriumRequest(@NotBlank @Size(max = 100) String name) {}
