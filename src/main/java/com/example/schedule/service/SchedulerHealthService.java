package com.example.schedule.service;

import com.example.schedule.dto.SchedulerHealthResponse;
import com.example.schedule.repository.JobExecutionLogRepository;
import com.example.schedule.repository.SchedulerConfigRepository;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Service;

@Service("quartzHealthIndicator")
public class SchedulerHealthService implements HealthIndicator {

    private final Scheduler scheduler;
    private final SchedulerConfigRepository configRepository;
    private final JobExecutionLogRepository logRepository;

    public SchedulerHealthService(Scheduler scheduler,
                                 SchedulerConfigRepository configRepository,
                                 JobExecutionLogRepository logRepository) {
        this.scheduler = scheduler;
        this.configRepository = configRepository;
        this.logRepository = logRepository;
    }

    public SchedulerHealthResponse getHealthStatus() {
        try {
            String status = scheduler.isStarted() && !scheduler.isInStandbyMode() ? "RUNNING" : "STANDBY";
            if (scheduler.isShutdown()) {
                status = "SHUTDOWN";
            }

            int totalJobs = (int) configRepository.count();
            int currentlyExecuting = scheduler.getCurrentlyExecutingJobs().size();
            long totalFailed = logRepository.countByStatus("FAILED");
            long totalMisfired = logRepository.countByStatus("MISFIRED");

            return new SchedulerHealthResponse(status, totalJobs, currentlyExecuting, totalFailed, totalMisfired);
        } catch (SchedulerException e) {
            return new SchedulerHealthResponse("ERROR", 0, 0, 0, 0);
        }
    }

    // Spring Boot Actuator integration for Spring /actuator/health
    @Override
    public Health health() {
        try {
            if (scheduler.isStarted() && !scheduler.isInStandbyMode()) {
                return Health.up()
                        .withDetail("scheduler", "RUNNING")
                        .withDetail("executingJobs", scheduler.getCurrentlyExecutingJobs().size())
                        .build();
            }
            return Health.down().withDetail("scheduler", "STOPPED").build();
        } catch (SchedulerException e) {
            return Health.down(e).build();
        }
    }
}