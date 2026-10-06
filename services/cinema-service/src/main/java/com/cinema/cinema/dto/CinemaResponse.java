package com.cinema.cinema.dto; // 7.1

public record CinemaResponse(String id, String name, String address, String city, String timezone) { // 7.2 Record = DTO bất biến
}