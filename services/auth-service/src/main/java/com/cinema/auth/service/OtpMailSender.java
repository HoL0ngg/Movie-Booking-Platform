package com.cinema.auth.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class OtpMailSender {

    private static final Logger log = LoggerFactory.getLogger(OtpMailSender.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public OtpMailSender(JavaMailSender mailSender,
                         @Value("${auth.otp.mail-enabled:false}") boolean enabled,
                         @Value("${auth.otp.mail-from}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void send(String to, String otp, Duration ttl) {
        if (!enabled) { // dev: không gửi mail thật, xem mã trong log
            log.warn("[DEV] OTP for {} = {}", to, otp);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Mã xác thực cinémat");
        message.setText("Mã OTP của bạn là: " + otp + "\nMã có hiệu lực " + ttl.toMinutes()
                + " phút. Không chia sẻ mã này cho bất kỳ ai.");
        mailSender.send(message);
    }
}