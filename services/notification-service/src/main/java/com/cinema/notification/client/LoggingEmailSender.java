package com.cinema.notification.client; // 11.4

import org.slf4j.Logger; // 11.5
import org.slf4j.LoggerFactory; // 11.6
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; // 11.7
import org.springframework.stereotype.Component; // 11.8

@Component // 11.9
@ConditionalOnProperty(name = "notification.mail.mode", havingValue = "log", matchIfMissing = true) // 11.10 Mặc định (và khi mode=log)
public class LoggingEmailSender implements EmailSender { // 11.11

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class); // 11.12

    @Override // 11.13
    public void send(String to, String subject, String body) { // 11.14
        log.info("[EMAIL-LOG] subject={} body={}", subject, body); // 11.15 Không log địa chỉ nhận
    }
}