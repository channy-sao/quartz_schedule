package com.example.schedule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableRetry
@EnableScheduling // needed for SchedulerReconciliationService's @Scheduled method
public class RetryConfig {
}