package com.example.schedule.dto;

import com.example.schedule.constant.CronScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Update-schedule payload. Mirrors CreateScheduleRequest's schedule-type-driven fields (see that
 * class's Javadoc for the field -> schedule type cheat sheet), plus a required changeReason for
 * audit purposes.
 *
 * NOTE: reconstructed to match ScheduleManagementService's existing usage
 * (request.cronExpression()/timezone()/changeReason()/message()/recipient()/webhookUrl()) —
 * reconcile field names against your actual file if it differs from this.
 */
public record UpdateScheduleRequest(

        @NotNull CronScheduleType type,

        String time,

        LocalDate date,

        LocalDate startDate,
        LocalDate endDate,

        List<String> daysOfWeek,

        String dayOfMonth,

        String month,

        String timezone,

        String message,
        String recipient,
        String webhookUrl,

        @NotBlank String changeReason
) {
    /** Maps this request onto the generic CronBuilderRequest consumed by QuartzCronUtil. */
    public CronBuilderRequest toCronBuilderRequest() {
        return new CronBuilderRequest(
                type, date, null, time,
                startDate, endDate, daysOfWeek, dayOfMonth, month,
                null, null, null, null, null, null, "Asia/Phnom_Penh"
        );
    }
}