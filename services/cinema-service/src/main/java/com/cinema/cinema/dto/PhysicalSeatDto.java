package com.cinema.cinema.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhysicalSeatDto(@NotNull UUID id, @NotNull UUID auditoriumId,
                              @NotNull @Pattern(regexp = "[A-Z]") String rowLabel,
                              @NotNull @Min(1) @Max(30) Integer seatNumber,
                              @NotNull @Pattern(regexp = "STANDARD|VIP|COUPLE") String seatType,
                              @NotNull Boolean isActive, UUID pairedSeatId) {}
