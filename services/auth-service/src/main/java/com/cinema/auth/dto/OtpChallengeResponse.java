package com.cinema.auth.dto;

public record OtpChallengeResponse(long expiresIn, long resendAfter) { // giây
}