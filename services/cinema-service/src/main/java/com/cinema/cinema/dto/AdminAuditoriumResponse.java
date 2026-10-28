package com.cinema.cinema.dto;

import java.util.UUID;

public record AdminAuditoriumResponse(UUID id, UUID cinemaId, String name) {}
