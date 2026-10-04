package com.cinema.auth.exception; // 13.1

import org.springframework.http.HttpStatus; // 13.2

public class AuthException extends RuntimeException { // 13.3 Unchecked → Spring tự rollback transaction

    private final HttpStatus status; // 13.4 Mã HTTP trả về
    private final String code; // 13.5 Mã lỗi ổn định cho client

    public AuthException(HttpStatus status, String code, String message) { // 13.6
        super(message); // 13.7 Message an toàn cho người dùng
        this.status = status; // 13.8
        this.code = code; // 13.9
    }

    public HttpStatus getStatus() { return status; } // 13.10
    public String getCode() { return code; } // 13.11
}