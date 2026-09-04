package com.example.schedule.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JobMetricsService {

    private final MeterRegistry registry;

    public JobMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordSuccess(String jobName, String jobType, long durationMs) {
        Counter.builder("jobs.execution.total")
                .tag("jobName", jobName)
                .tag("jobType", jobType)
                .tag("status", "SUCCESS")
                .register(registry)
                .increment();

        Timer.builder("jobs.execution.duration")
                .tag("jobName", jobName)
                .tag("jobType", jobType)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordFailure(String jobName, String jobType) {
        Counter.builder("jobs.execution.total")
                .tag("jobName", jobName)
                .tag("jobType", jobType)
                .tag("status", "FAILED")
                .register(registry)
                .increment();
    }

    public void recordRetry(String jobName, String jobType, int attempt) {
        Counter.builder("jobs.retry.total")
                .tag("jobName", jobName)
                .tag("jobType", jobType)
                .tag("attempt", String.valueOf(attempt))
                .register(registry)
                .increment();
    }

    public void recordMisfire(String jobName) {
        Counter.builder("jobs.misfire.total")
                .tag("jobName", jobName)
                .register(registry)
                .increment();
    }
}