package com.cinema.auth.config; // 3.1

import java.io.IOException; // 3.2 Lấy traceId đưa vào body lỗi
import java.time.Instant; // 3.3

import org.springframework.context.annotation.Bean; // 3.4
import org.springframework.context.annotation.Configuration; // 3.5
import org.springframework.http.HttpMethod; // 3.6
import org.springframework.http.HttpStatus; // 3.7
import org.springframework.security.access.AccessDeniedException; // 3.8
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 3.9
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // 3.10
import org.springframework.security.config.http.SessionCreationPolicy; // 3.11 Đã đăng nhập nhưng thiếu quyền
import org.springframework.security.core.AuthenticationException; // 3.12
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 3.13
import org.springframework.security.crypto.password.PasswordEncoder; // 3.14
import org.springframework.security.oauth2.core.OAuth2AuthenticationException; // 3.15 Lỗi xác thực
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter; // 3.16
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter; // 3.17
import org.springframework.security.web.SecurityFilterChain; // 3.18 Lỗi token sai/hết hạn

import com.cinema.auth.filter.TraceIdFilter; // 3.19 Jwt → Authentication

import jakarta.servlet.http.HttpServletRequest; // 3.20 Claim → quyền
import jakarta.servlet.http.HttpServletResponse; // 3.21

@Configuration(proxyBeanMethods = false) // 3.22
public class SecurityConfiguration { // 3.23

    @Bean // 3.24 Chuỗi filter bảo mật
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // 3.25
        return http // 3.26
                .csrf(AbstractHttpConfigurer::disable) // 3.27 API stateless dùng Bearer → tắt CSRF
                .httpBasic(AbstractHttpConfigurer::disable) // 3.28
                .formLogin(AbstractHttpConfigurer::disable) // 3.29
                .logout(AbstractHttpConfigurer::disable) // 3.30 Ta có API logout riêng
                .requestCache(AbstractHttpConfigurer::disable) // 3.31
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 3.32 Không tạo HttpSession
                .authorizeHttpRequests(requests -> requests // 3.33 Luật truy cập
                        .requestMatchers("/actuator/health/**", "/actuator/info",
                                "/openapi/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll() // 3.34 Endpoint kỹ thuật
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll() // 3.35 Chưa có token vẫn gọi được
                        .requestMatchers(HttpMethod.GET, "/internal/v1/users/*/email").permitAll() // 3.36 Mở ở Security; token nội bộ kiểm trong controller (gateway không route /internal)
                        .requestMatchers("/api/v1/auth/logout", "/api/v1/me").authenticated() // 3.37 Bắt buộc Bearer hợp lệ
                        .anyRequest().denyAll()) // 3.38 Còn lại từ chối
                .oauth2ResourceServer(oauth -> oauth // 3.39 Bật xác thực Bearer JWT
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())) // 3.40
                        .authenticationEntryPoint(this::unauthorized)) // 3.41 401 theo format ApiError
                .exceptionHandling(handling -> handling.accessDeniedHandler(this::forbidden)) // 3.42 403 theo format ApiError
                .build(); // 3.43
    }

    @Bean // 3.44
    PasswordEncoder passwordEncoder() { // 3.45
        return new BCryptPasswordEncoder(); // 3.46 BCrypt (tự salt)
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() { // 3.47
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter(); // 3.48
        authorities.setAuthoritiesClaimName("roles"); // 3.49 Đọc claim "roles"
        authorities.setAuthorityPrefix("ROLE_"); // 3.50 CUSTOMER → ROLE_CUSTOMER
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); // 3.51
        converter.setJwtGrantedAuthoritiesConverter(authorities); // 3.52
        return converter; // 3.53
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException { // 3.54 Gọi khi không/sai token
        boolean badToken = ex instanceof OAuth2AuthenticationException; // 3.55 Có token nhưng sai/hết hạn?
        writeError(request, response, HttpStatus.UNAUTHORIZED, // 3.56
                badToken ? "TOKEN_INVALID" : "AUTHENTICATION_REQUIRED", // 3.57
                badToken ? "Token is invalid or expired." : "Authentication is required."); // 3.58
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException { // 3.59
        writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission."); // 3.60
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message) throws IOException { // 3.61
        Object trace = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE); // 3.62 traceId do TraceIdFilter đặt
        response.setStatus(status.value()); // 3.63
        response.setContentType("application/json"); // 3.64
        response.setCharacterEncoding("UTF-8"); // 3.65
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message // 3.66 Tự dựng JSON (chuỗi cố định, an toàn)
                + "\",\"traceId\":\"" + (trace == null ? "unavailable" : trace) // 3.67
                + "\",\"timestamp\":\"" + Instant.now() + "\",\"details\":{}}"); // 3.68
    }
}