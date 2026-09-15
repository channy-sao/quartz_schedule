package com.example.schedule.dto;

import com.example.schedule.constant.CronScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;


/**
 * Create-schedule payload. Instead of a raw Quartz cron string, the caller picks a schedule
 * type — matching the "Schedule Type" picker (One Time / Daily / Weekly / Monthly /
 * Every 3 Months / Every 6 Months / Annually) — plus only the fields relevant to that type.
 * The cron expression itself is built server-side via QuartzCronUtil.
 *
 * Field -> schedule type cheat sheet:
 *   ONE_TIME (SPECIFIC_DATE)  -> date [+ time]
 *   DAILY                     -> [time], startDate, endDate
 *   WEEKLY                    -> daysOfWeek, [time], startDate, endDate
 *   MONTHLY                   -> dayOfMonth, [time], startDate, endDate
 *   EVERY_3_MONTHS            -> dayOfMonth, startDate (anchors the quarter), [time], endDate
 *   EVERY_6_MONTHS            -> dayOfMonth, startDate (anchors the half-year), [time], endDate
 *   YEARLY (Annually)         -> dayOfMonth, startDate (or month), [time], endDate
 *
 * `time` is optional everywhere and defaults to 10:00 PM (QuartzCronUtil.DEFAULT_PROCESS_TIME).
 */
public record CreateScheduleRequest(

        @NotNull CronScheduleType type,

        // "HH:mm" — optional, defaults to 22:00 if omitted
        String time,

        // ONE_TIME only
        LocalDate date,

        // Validity window for recurring types. startDate also anchors the recurring month for
        // EVERY_3_MONTHS / EVERY_6_MONTHS / YEARLY when `month` isn't explicitly supplied.
        // Applied on the Quartz Trigger itself (startAt/endAt), not baked into the cron string.
        LocalDate startDate,
        LocalDate endDate,

        // WEEKLY only, e.g. ["MON", "WED", "FRI"]
        List<String> daysOfWeek,

        // MONTHLY / EVERY_3_MONTHS / EVERY_6_MONTHS / YEARLY
        String dayOfMonth,

        // YEARLY only — optional, derived from startDate's month when omitted
        String month,

        String timezone,

        @NotBlank String jobType,
        String message,
        String recipient,
        String webhookUrl,
        String businessName,
        String description
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