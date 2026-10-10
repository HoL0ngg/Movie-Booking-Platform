package com.cinema.notification.config; // 1.1

import org.springframework.context.annotation.Configuration; // 1.2
import org.springframework.scheduling.annotation.EnableScheduling; // 1.3

@Configuration(proxyBeanMethods = false) // 1.4
@EnableScheduling // 1.5 Bật @Scheduled cho NotificationDispatcher
public class SchedulingConfiguration { // 1.6
}