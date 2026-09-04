package com.example.schedule.dto;

import java.time.Instant;

public record TriggerJobResponse(
        String jobName,
        String executionId,
        String status,
        Instant triggeredAt
) {}