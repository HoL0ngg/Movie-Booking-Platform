package com.cinema.notification.exception; // 4.1

public class InvalidEventException extends RuntimeException { // 4.2 Event sai định dạng/thiếu field (lỗi vĩnh viễn)

    public InvalidEventException(String message) { // 4.3
        super(message); // 4.4 Chỉ nêu tên field, không kèm giá trị (tránh lộ dữ liệu)
    }
}