package com.cinema.payment.dto;

public record CreateResult(PaymentResponse payment, boolean created) {
}
