package com.cinema.cinema.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveSeatLayoutRequest(@NotNull @Size(min = 1, max = 780) List<@NotNull @Valid PhysicalSeatDto> seats) {}
