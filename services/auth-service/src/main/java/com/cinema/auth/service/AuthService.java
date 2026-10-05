package com.cinema.auth.service; // 8.1

import java.time.Instant; // 8.2
import java.util.List; // 8.3
import java.util.Locale; // 8.4
import java.util.UUID; // 8.5

import org.springframework.dao.DataIntegrityViolationException; // 8.6
import org.springframework.http.HttpStatus; // 8.7
import org.springframework.security.crypto.password.PasswordEncoder; // 8.8
import org.springframework.stereotype.Service; // 8.9
import org.springframework.transaction.annotation.Transactional; // 8.10

import com.cinema.auth.config.JwtProperties; // 8.11
import com.cinema.auth.dto.LoginRequest; // 8.12
import com.cinema.auth.dto.MeResponse; // 8.13
import com.cinema.auth.dto.RefreshRequest; // 8.14
import com.cinema.auth.dto.RegisterRequest; // 8.15
import com.cinema.auth.dto.TokenResponse; // 8.16
import com.cinema.auth.entity.RefreshSessionEntity; // 8.17
import com.cinema.auth.entity.RoleEntity; // 8.18
import com.cinema.auth.entity.UserEntity; // 8.19
import com.cinema.auth.entity.UserRoleEntity; // 8.20
import com.cinema.auth.exception.AuthException; // 8.21 Vi phạm unique/FK ở DB
import com.cinema.auth.repository.RefreshSessionRepository; // 8.22
import com.cinema.auth.repository.RoleRepository; // 8.23
import com.cinema.auth.repository.UserRepository; // 8.24
import com.cinema.auth.repository.UserRoleRepository; // 8.25

@Service // 8.26 Bean chứa logic nghiệp vụ
public class AuthService { // 8.27

    private static final String DEFAULT_ROLE = "CUSTOMER"; // 8.28 Role gán khi đăng ký
    private static final String ACTIVE = "ACTIVE"; // 8.29 Khớp CHECK status ở DB

    private final UserRepository userRepository; // 8.30
    private final RoleRepository roleRepository; // 8.31
    private final UserRoleRepository userRoleRepository; // 8.32
    private final RefreshSessionRepository refreshSessionRepository; // 8.33
    private final PasswordEncoder passwordEncoder; // 8.34
    private final JwtService jwtService; // 8.35
    private final TokenHasher tokenHasher; // 8.36
    private final JwtProperties jwtProperties; // 8.37

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, // 8.38 Constructor injection
                       UserRoleRepository userRoleRepository, RefreshSessionRepository refreshSessionRepository, // 8.39
                       PasswordEncoder passwordEncoder, JwtService jwtService, // 8.40
                       TokenHasher tokenHasher, JwtProperties jwtProperties) { // 8.41
        this.userRepository = userRepository; // 8.42
        this.roleRepository = roleRepository; // 8.43
        this.userRoleRepository = userRoleRepository; // 8.44
        this.refreshSessionRepository = refreshSessionRepository; // 8.45
        this.passwordEncoder = passwordEncoder; // 8.46
        this.jwtService = jwtService; // 8.47
        this.tokenHasher = tokenHasher; // 8.48
        this.jwtProperties = jwtProperties; // 8.49
    }

    @Transactional // 8.50 Tạo user + gán role + tạo session trong 1 transaction
    public TokenResponse register(RegisterRequest request) { // 8.51
        String email = request.email().trim(); // 8.52 Khớp CHECK email_normalized = lower(btrim(email))
        String normalized = email.toLowerCase(Locale.ROOT); // 8.53
        if (userRepository.existsByEmailNormalized(normalized)) { // 8.54 Kiểm tra trùng email
            throw emailTaken(); // 8.55
        }
        RoleEntity role = roleRepository.findByCode(DEFAULT_ROLE) // 8.56
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER chưa được seed")); // 8.57 Chưa chạy seed-roles.sql → 500
        Instant now = Instant.now(); // 8.58
        UserEntity user = new UserEntity(UUID.randomUUID(), email, normalized, // 8.59 id tự sinh ở app
                passwordEncoder.encode(request.password()), ACTIVE, now); // 8.60 Lưu hash, không lưu mật khẩu thô
        try { // 8.61
            userRepository.saveAndFlush(user); // 8.62 Flush ngay để lỗi unique nổ tại đây
        } catch (DataIntegrityViolationException ex) { // 8.63 Hai request đăng ký cùng email song song
            throw emailTaken(); // 8.64
        }
        userRoleRepository.save(new UserRoleEntity(user.getId(), role.getId(), now)); // 8.65 Gán role CUSTOMER
        return issueTokens(user, List.of(DEFAULT_ROLE), now); // 8.66
    }

    @Transactional // 8.67 Có ghi refresh_sessions
    public TokenResponse login(LoginRequest request) { // 8.68
        UserEntity user = userRepository.findByEmailNormalized(request.email().trim().toLowerCase(Locale.ROOT)) // 8.69
                .filter(found -> ACTIVE.equals(found.getStatus())) // 8.70 Tài khoản DISABLED không đăng nhập được
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash())) // 8.71 So mật khẩu với hash
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.")); // 8.72 Một lỗi chung, không lộ email có tồn tại hay không
        return issueTokens(user, roleRepository.findCodesByUserId(user.getId()), Instant.now()); // 8.73
    }

    @Transactional(noRollbackFor = AuthException.class) // 8.74 Giữ việc thu hồi session dù ném AuthException
    public TokenResponse refresh(RefreshRequest request) { // 8.75
        Instant now = Instant.now(); // 8.76
        RefreshSessionEntity session = refreshSessionRepository.findByTokenHash(tokenHasher.hash(request.refreshToken())) // 8.77 Tìm theo hash, có khóa dòng
                .orElseThrow(this::tokenInvalid); // 8.78 Không tìm thấy → 401
        if (session.getRevokedAt() != null) { // 8.79 Token đã thu hồi mà bị dùng lại = replay
            refreshSessionRepository.revokeAllActiveByUserId(session.getUserId(), now); // 8.80 Thu hồi mọi session còn hiệu lực của user
            throw tokenInvalid(); // 8.81
        }
        if (!session.getExpiresAt().isAfter(now)) { // 8.82 Hết hạn
            throw tokenInvalid(); // 8.83
        }
        UserEntity user = userRepository.findById(session.getUserId()) // 8.84
                .filter(found -> ACTIVE.equals(found.getStatus())) // 8.85
                .orElseThrow(this::tokenInvalid); // 8.86
        session.revoke(now); // 8.87 Rotation: thu hồi token cũ (JPA tự UPDATE khi commit)
        return issueTokens(user, roleRepository.findCodesByUserId(user.getId()), now); // 8.88 Cấp cặp token mới
    }

    @Transactional // 8.89
    public void logout(UUID userId, String refreshToken) { // 8.90
        refreshSessionRepository.findByTokenHash(tokenHasher.hash(refreshToken)) // 8.91
                .filter(session -> session.getUserId().equals(userId)) // 8.92 Chỉ thu hồi session của chính mình
                .filter(session -> session.getRevokedAt() == null) // 8.93 Đã thu hồi rồi thì bỏ qua
                .ifPresent(session -> session.revoke(Instant.now())); // 8.94 Idempotent: không có cũng không lỗi
    }

    @Transactional(readOnly = true) // 8.95 Chỉ đọc
    public MeResponse me(UUID userId) { // 8.96
        UserEntity user = userRepository.findById(userId) // 8.97
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.")); // 8.98 Token hợp lệ nhưng user đã bị xóa
        return new MeResponse(user.getId().toString(), user.getEmail(), roleRepository.findCodesByUserId(userId)); // 8.99
    }

    @Transactional(readOnly = true) // 8.100
    public String emailOf(UUID userId) { // 8.101 Dành cho service nội bộ (notification)
        return userRepository.findById(userId) // 8.102
                .map(UserEntity::getEmail) // 8.103 Chỉ lấy email, không lộ field khác
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found.")); // 8.104
    }

    private TokenResponse issueTokens(UserEntity user, List<String> roles, Instant now) { // 8.105 Dùng chung cho register/login/refresh
        String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail(), roles, now); // 8.106
        String refreshToken = tokenHasher.newToken(); // 8.107 Chuỗi ngẫu nhiên trả cho client
        refreshSessionRepository.save(new RefreshSessionEntity(UUID.randomUUID(), user.getId(), // 8.108 DB chỉ lưu hash
                tokenHasher.hash(refreshToken), now.plus(jwtProperties.refreshTtl()), now)); // 8.109
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtProperties.accessTtl().toSeconds()); // 8.110
    }

    private AuthException emailTaken() { // 8.111
        return new AuthException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered."); // 8.112
    }

    private AuthException tokenInvalid() { // 8.113
        return new AuthException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Token is invalid or expired."); // 8.114
    }
}