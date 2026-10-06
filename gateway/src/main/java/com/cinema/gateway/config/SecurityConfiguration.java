package com.cinema.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(e -> e.authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);   // 401 sạch, không có WWW-Authenticate
                    return exchange.getResponse().setComplete();
                }))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/api/v1/movies", "/api/v1/movies/*").permitAll()
                        .pathMatchers(HttpMethod.POST,
                            "/api/v1/auth/register/otp", "/api/v1/auth/register/otp/verify",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/me").permitAll()
                        .pathMatchers("/api/v1/cinemas", "/api/v1/cinemas/**", "/api/v1/showtimes", "/api/v1/showtimes/**").permitAll()
                        .pathMatchers(
                                "/actuator/health/**", "/actuator/info",
                                "/openapi/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .anyExchange().denyAll())
                .build();
    }

    @Bean
    ReactiveUserDetailsService rejectingReactiveUserDetailsService() {
        return username -> Mono.error(new UsernameNotFoundException("Authentication is not implemented in Phase 1"));
    }
}
