package com.cinema.booking.messaging;

/**
 * Message không thể xử lý dù thử lại bao nhiêu lần (sai định dạng, thiếu field,
 * version lạ...).
 * Error handler sẽ đẩy thẳng sang quarantine topic, không retry.
 */
public class PoisonMessageException extends RuntimeException {
    public PoisonMessageException(String message) {
        super(message);
    }

    public PoisonMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
