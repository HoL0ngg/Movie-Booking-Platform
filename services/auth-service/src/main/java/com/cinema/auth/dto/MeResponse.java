package com.cinema.auth.dto; // 7.17

import java.util.List; // 7.18

public record MeResponse(String id, String email, List<String> roles) { // 7.19 Chỉ field an toàn, không có password_hash
}