package com.cinema.booking.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 2.16
import com.cinema.booking.dto.GetSeatsResponse;
import com.cinema.booking.repository.ShowtimeSeatRepository;

@Service
@Transactional(readOnly = true)
public class ShowtimeSeatService {
    private final ShowtimeSeatRepository showtimeSeatRepository;

    public ShowtimeSeatService(ShowtimeSeatRepository showtimeSeatRepository) {
        this.showtimeSeatRepository = showtimeSeatRepository;
    }

    public List<GetSeatsResponse> getSeats(UUID showtimeId) {
        return showtimeSeatRepository.findAllByShowtimeId(showtimeId)
                .stream()
                .map(seat -> new GetSeatsResponse(
                        seat.getShowtimeId(),
                        seat.getSeatId(),
                        seat.getSeatLabel(),
                        seat.getSeatType(),
                        seat.getPriceMinor(),
                        seat.getStatus()))
                .toList();
    }
}
