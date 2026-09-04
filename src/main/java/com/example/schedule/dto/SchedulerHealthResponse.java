package com.example.schedule.dto;

public record SchedulerHealthResponse(
        String schedulerStatus, // RUNNING, STANDBY, SHUTDOWN
        int totalConfiguredJobs,
        int currentlyExecutingJobs,
        long failedJobsTotal,
        long misfiredJobsTotal
) {}