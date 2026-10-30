package com.cinema.ai.config;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.cinema.ai.client.CinemaScheduleClient;
import com.cinema.ai.client.MovieCatalogClient;
import com.cinema.ai.filter.TraceIdFilter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogProperties.class)
public class CatalogClientConfiguration {

    @Bean
    MovieCatalogClient movieCatalogClient(CatalogProperties properties) {
        return new MovieCatalogClient(restClient(properties.movieServiceUrl(), properties));
    }

    @Bean
    CinemaScheduleClient cinemaScheduleClient(CatalogProperties properties) {
        return new CinemaScheduleClient(restClient(properties.cinemaServiceUrl(), properties));
    }

    private static RestClient restClient(String baseUrl, CatalogProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(propagateTraceId())
                .build();
    }

    private static ClientHttpRequestInterceptor propagateTraceId() {
        return (request, body, execution) -> {
            String traceId = MDC.get(TraceIdFilter.TRACE_ID_ATTRIBUTE);
            if (traceId != null) {
                request.getHeaders().set(TraceIdFilter.TRACE_ID_HEADER, traceId);
            }
            return execution.execute(request, body);
        };
    }
}
