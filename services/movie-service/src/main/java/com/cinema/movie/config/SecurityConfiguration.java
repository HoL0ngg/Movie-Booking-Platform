package com.cinema.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import java.io.IOException;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.cinema.movie.filter.TraceIdFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/v1/admin/movies", "/api/v1/admin/movies/*", "/api/v1/admin/genres").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/movies", "/api/v1/movies/*")
                        .permitAll()
                        .requestMatchers(
                                "/actuator/health/**", "/actuator/info",
                                "/openapi/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, error) -> writeError(request, response, 401,
                                "AUTHENTICATION_REQUIRED", "A valid admin token is required.")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> writeError(request, response, 401,
                                "AUTHENTICATION_REQUIRED", "A valid admin token is required."))
                        .accessDeniedHandler((request, response, error) -> writeError(request, response, 403,
                                "FORBIDDEN", "Administrator permission is required.")))
                .build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status,
                            String code, String message) throws IOException {
        Object trace = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"traceId\":\"" + (trace == null ? "unavailable" : trace)
                + "\",\"timestamp\":\"" + Instant.now() + "\",\"details\":{}}");
    }
}
