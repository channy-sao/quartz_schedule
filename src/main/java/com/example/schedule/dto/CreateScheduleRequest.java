package com.example.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateScheduleRequest(
        @NotBlank String cronExpression,
        String timezone,
        @NotBlank String jobType,
        String message,
        String recipient,
        String webhookUrl,
        String businessName,
        String description,
        LocalDate endDate
) {}