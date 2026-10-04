package com.cinema.auth.dto; // 7.20

public record UserEmailResponse(String email) { // 7.21 Chỉ trả đúng một field cho service nội bộ
}