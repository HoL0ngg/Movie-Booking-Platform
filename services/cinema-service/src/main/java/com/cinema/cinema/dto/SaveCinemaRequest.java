package com.cinema.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveCinemaRequest(@NotBlank @Size(max = 200) String name,
                                @NotBlank @Size(max = 500) String address,
                                @NotBlank @Size(max = 100) String city,
                                @Size(max = 100) String timezone) {}
