package com.cinema.cinema.service; // 10.1

import com.cinema.cinema.exception.CinemaException; // 10.2
import com.cinema.cinema.repository.CinemaManagerRepository; // 10.3
import java.util.List; // 10.4
import java.util.UUID; // 10.5
import org.springframework.http.HttpStatus; // 10.6
import org.springframework.security.oauth2.jwt.Jwt; // 10.7
import org.springframework.stereotype.Service; // 10.8

@Service // 10.9
public class CinemaAuthorizationService { // 10.10

    private final CinemaManagerRepository managers; // 10.11 Bảng cinema_managers

    public CinemaAuthorizationService(CinemaManagerRepository managers) { // 10.12
        this.managers = managers; // 10.13
    }

    public void assertCanManage(Jwt jwt, UUID cinemaId) { // 10.14 Ném lỗi nếu không đủ quyền
        List<String> roles = jwt.getClaimAsStringList("roles"); // 10.15 Claim "roles" do auth cấp
        if (roles == null) { // 10.16 Token không có claim
            roles = List.of(); // 10.17
        }
        if (roles.contains("ADMIN")) { // 10.18 ADMIN quản lý mọi rạp
            return; // 10.19
        }
        if (roles.contains("CINEMA_MANAGER") // 10.20 Quản lý rạp: phải có role
                && managers.existsByCinemaIdAndUserId(cinemaId, UUID.fromString(jwt.getSubject()))) { // 10.21 và được gán đúng rạp này
            return; // 10.22
        }
        throw new CinemaException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to manage this cinema."); // 10.23
    }
}