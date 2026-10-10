package com.cinema.notification.client; // 11.1

public interface EmailSender { // 11.2 Cổng gửi email, tách khỏi cách gửi cụ thể
    void send(String to, String subject, String body); // 11.3 Ném RuntimeException nếu gửi thất bại
}