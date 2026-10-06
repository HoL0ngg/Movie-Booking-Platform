package com.cinema.cinema.exception; // 16.1

import org.springframework.http.HttpStatus; // 16.2

public class CinemaException extends RuntimeException { // 16.3 Unchecked: Spring tự rollback transaction

    private final HttpStatus status; // 16.4
    private final String code; // 16.5

    public CinemaException(HttpStatus status, String code, String message) { // 16.6
        super(message); // 16.7
        this.status = status; // 16.8
        this.code = code; // 16.9
    }

    public HttpStatus getStatus() { return status; } // 16.10
    public String getCode() { return code; } // 16.11
}