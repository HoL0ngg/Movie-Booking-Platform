package com.cinema.cinema.config; // 2.1

import org.springframework.context.annotation.Configuration; // 2.2
import org.springframework.scheduling.annotation.EnableScheduling; // 2.3

@Configuration(proxyBeanMethods = false) // 2.4
@EnableScheduling // 2.5 Bật xử lý @Scheduled (cho OutboxRelay)
public class SchedulingConfiguration { // 2.6
}