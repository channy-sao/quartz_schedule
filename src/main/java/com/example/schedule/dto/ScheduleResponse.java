package com.example.schedule.dto;

import java.time.Instant;

public record ScheduleResponse(
        String jobName,
        String businessName,
        String description,
        String jobType,
        String cronExpression,
        String triggerState,
        Instant nextFireTime,
        Instant previousFireTime,
        String lastExecutionStatus,
        Long lastExecutionDurationMs,
        long failureCount,
        String createdBy,
        String updatedBy,
        Instant updatedAt
) {}