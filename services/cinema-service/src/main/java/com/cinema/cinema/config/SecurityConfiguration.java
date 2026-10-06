package com.cinema.cinema.config; // 3.1

import java.io.IOException; // 3.2
import java.time.Instant; // 3.3

import org.springframework.context.annotation.Bean; // 3.4
import org.springframework.context.annotation.Configuration; // 3.5
import org.springframework.http.HttpMethod; // 3.6
import org.springframework.http.HttpStatus; // 3.7
import org.springframework.security.access.AccessDeniedException; // 3.8
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 3.9
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // 3.10
import org.springframework.security.config.http.SessionCreationPolicy; // 3.11
import org.springframework.security.core.AuthenticationException; // 3.12
import org.springframework.security.oauth2.core.OAuth2AuthenticationException; // 3.13
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter; // 3.14
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter; // 3.15
import org.springframework.security.web.SecurityFilterChain; // 3.16

import com.cinema.cinema.filter.TraceIdFilter; // 3.17

import jakarta.servlet.http.HttpServletRequest; // 3.18
import jakarta.servlet.http.HttpServletResponse; // 3.19

@Configuration(proxyBeanMethods = false) // 3.20
public class SecurityConfiguration { // 3.21

    @Bean // 3.22
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // 3.23
        return http // 3.24
                .csrf(AbstractHttpConfigurer::disable) // 3.25 API stateless → tắt CSRF
                .httpBasic(AbstractHttpConfigurer::disable) // 3.26
                .formLogin(AbstractHttpConfigurer::disable) // 3.27
                .logout(AbstractHttpConfigurer::disable) // 3.28
                .requestCache(AbstractHttpConfigurer::disable) // 3.29
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 3.30
                .authorizeHttpRequests(requests -> requests // 3.31
                        .requestMatchers("/actuator/health/**", "/actuator/info",
                                "/openapi/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll() // 3.32 Endpoint kỹ thuật
                        .requestMatchers(HttpMethod.GET, "/api/v1/cinemas", "/api/v1/cinemas/**", "/api/v1/showtimes/**").permitAll() // 3.33 Đọc công khai
                        .requestMatchers(HttpMethod.POST, "/api/v1/showtimes", "/api/v1/showtimes/**").authenticated() // 3.34 Lệnh quản trị cần token (role kiểm tra ở service)
                        .anyRequest().denyAll()) // 3.35 Còn lại chặn
                .oauth2ResourceServer(oauth -> oauth // 3.36 Bật xác thực Bearer JWT
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())) // 3.37
                        .authenticationEntryPoint(this::unauthorized)) // 3.38 401 theo format ApiError
                .exceptionHandling(handling -> handling.accessDeniedHandler(this::forbidden)) // 3.39 403 theo format ApiError
                .build(); // 3.40
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() { // 3.41
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter(); // 3.42
        authorities.setAuthoritiesClaimName("roles"); // 3.43 Claim "roles" do auth cấp
        authorities.setAuthorityPrefix("ROLE_"); // 3.44
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); // 3.45
        converter.setJwtGrantedAuthoritiesConverter(authorities); // 3.46
        return converter; // 3.47
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException { // 3.48
        boolean badToken = ex instanceof OAuth2AuthenticationException; // 3.49 Có token nhưng sai/hết hạn?
        writeError(request, response, HttpStatus.UNAUTHORIZED, // 3.50
                badToken ? "TOKEN_INVALID" : "AUTHENTICATION_REQUIRED", // 3.51
                badToken ? "Token is invalid or expired." : "Authentication is required."); // 3.52
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException { // 3.53
        writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission."); // 3.54
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message) throws IOException { // 3.55
        Object trace = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE); // 3.56
        response.setStatus(status.value()); // 3.57
        response.setContentType("application/json"); // 3.58
        response.setCharacterEncoding("UTF-8"); // 3.59
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message // 3.60 JSON tự dựng (chuỗi cố định)
                + "\",\"traceId\":\"" + (trace == null ? "unavailable" : trace) // 3.61
                + "\",\"timestamp\":\"" + Instant.now() + "\",\"details\":{}}"); // 3.62
    }
}