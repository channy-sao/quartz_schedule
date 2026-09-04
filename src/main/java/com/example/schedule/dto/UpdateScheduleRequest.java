package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateScheduleRequest(
        @NotBlank String cronExpression,
        String timezone,
        String message,
        String recipient,
        String webhookUrl,
        @NotBlank String changeReason
) {}