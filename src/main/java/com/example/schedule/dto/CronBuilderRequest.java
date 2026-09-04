package com.example.schedule.dto;

import com.example.schedule.constant.CronScheduleType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CronBuilderRequest(

        @NotNull
        CronScheduleType type,

        /**
         * Timezone used when calculating next execution.
         *
         * Example:
         * Asia/Phnom_Penh
         */
        String timezone,

        /**
         * Time for DAILY / WEEKLY / MONTHLY / YEARLY.
         *
         * Example:
         * 09:30
         */
        String time,

        /**
         * Days for WEEKLY.
         *
         * Example:
         * ["MON", "WED", "FRI"]
         */
        List<String> daysOfWeek,

        /**
         * Day of month for MONTHLY / YEARLY.
         *
         * Supports Quartz values:
         *
         * 1
         * 15
         * L
         * LW
         * 15W
         * L-3
         */
        String dayOfMonth,

        /**
         * Specific date for SPECIFIC_DATE.
         *
         * Example:
         * 2026-09-15
         */
        LocalDate date,

        /**
         * Specific time for SPECIFIC_DATE.
         *
         * Example:
         * 09:30
         */
        String specificTime,

        /**
         * Custom Quartz Cron fields.
         */
        String seconds,

        String minutes,

        String hours,

        String dayOfMonthExpression,

        String month,

        String dayOfWeek,

        /**
         * Optional Quartz year.
         *
         * Example:
         * 2026
         * 2026-2030
         * *
         */
        String year

) {
}