package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateScheduleRequest(
        @NotBlank String jobName,
        @NotBlank String cronExpression,
        String timezone,
        @NotBlank String jobType,
        String message,
        String recipient,
        String webhookUrl,
        String businessName,
        String description
) {}