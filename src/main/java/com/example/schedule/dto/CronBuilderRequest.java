package com.example.schedule.dto;

import com.example.schedule.constant.CronScheduleType;
import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for building a Quartz Cron expression.
 *
 * Only the fields relevant to {@code type} need to be populated; everything else can be left null.
 *
 * If {@code time} is omitted, schedules default to 10:00 PM (22:00) — see
 * {@code QuartzCronUtil.DEFAULT_PROCESS_TIME}.
 *
 * {@code startDate} / {@code endDate} are NOT encoded into the cron expression itself — Quartz cron
 * has no native concept of a validity window. Instead:
 * <ul>
 *   <li>{@code startDate}'s month anchors the recurrence for EVERY_3_MONTHS / EVERY_6_MONTHS /
 *       YEARLY when {@code month} isn't explicitly supplied.</li>
 *   <li>Both {@code startDate} and {@code endDate} should be applied on the Quartz Trigger itself
 *       ({@code TriggerBuilder.startAt(startDate)} / {@code .endAt(endDate)}) by whatever code
 *       actually schedules the job — that's the correct place for a validity window in Quartz.</li>
 * </ul>
 */
public record CronBuilderRequest(

        CronScheduleType type,

        // ONE_TIME (SPECIFIC_DATE)
        LocalDate date,
        String specificTime,

        // Shared time-of-day, "HH:mm". Optional — defaults to 22:00 if blank.
        String time,

        // DAILY / WEEKLY / MONTHLY / EVERY_3_MONTHS / EVERY_6_MONTHS / YEARLY validity window.
        // Not part of the cron expression — pass through to the Quartz Trigger's startAt/endAt.
        LocalDate startDate,
        LocalDate endDate,

        // WEEKLY
        List<String> daysOfWeek,

        // MONTHLY / EVERY_3_MONTHS / EVERY_6_MONTHS / YEARLY — day of month. Supports Quartz
        // syntax such as "15", "L", "LW", "15W", "L-3"
        String dayOfMonth,

        // YEARLY — optional; derived from startDate's month when omitted
        String month,

        // CUSTOM
        String seconds,
        String minutes,
        String hours,
        String dayOfMonthExpression,
        String dayOfWeek,
        String year,
        String timezone
) {
}