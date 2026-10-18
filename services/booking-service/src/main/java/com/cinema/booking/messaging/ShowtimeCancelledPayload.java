package com.cinema.booking.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShowtimeCancelledPayload(UUID showtimeId, long snapshotVersion, String reasonCode, String cancelledAt) {
}