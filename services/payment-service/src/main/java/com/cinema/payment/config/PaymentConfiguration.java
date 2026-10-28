package com.cinema.payment.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cinema.payment.provider.vnpay.VnpayGateway;
import com.cinema.payment.provider.vnpay.VnpayProperties;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class PaymentConfiguration {

    @Bean
    @ConfigurationProperties("payment.vnpay")
    VnpayProperties vnpayProperties() {
        return new VnpayProperties();
    }

    @Bean
    HttpClient paymentHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Bean
    VnpayGateway vnpayGateway(VnpayProperties properties, HttpClient paymentHttpClient) {
        properties.validate(); // fail-fast khi thiếu TMN code / hash secret
        return new VnpayGateway(properties, paymentHttpClient);
    }
}
