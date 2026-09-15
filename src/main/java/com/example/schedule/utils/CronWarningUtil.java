package com.example.schedule.utils;

import com.example.schedule.constant.CronScheduleType;
import com.example.schedule.dto.CronBuilderRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CronWarningUtil {

    private CronWarningUtil() {}

    public static List<String> check(CronBuilderRequest request, String cronExpression) {
        List<String> warnings = new ArrayList<>();

        switch (request.type()) {
            case EVERY_SECOND -> warnings.add(
                    "This runs 86,400 times per day. Make sure your job is lightweight and idempotent.");
            case EVERY_MINUTE -> warnings.add(
                    "This runs 1,440 times per day. Confirm this frequency is intentional.");
            default -> { /* no warning needed */ }
        }

        if (request.type() == CronScheduleType.SPECIFIC_DATE
                && request.date() != null
                && request.date().isBefore(LocalDate.now())) {
            warnings.add("The specified date is in the past — this schedule will never fire.");
        }

        if (request.endDate() != null
                && request.endDate().isBefore(LocalDate.now())) {
            warnings.add("The end date is in the past — this schedule will never fire.");
        }

        if (request.startDate() != null
                && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            warnings.add("The end date is before the start date.");
        }

        if (isDefaultTimeEligible(request.type())
                && (request.time() == null || request.time().isBlank())) {
            warnings.add("No time was specified — defaulting to 10:00 PM.");
        }

        return warnings;
    }

    /**
     * Types where an omitted `time` silently falls back to QuartzCronUtil.DEFAULT_PROCESS_TIME
     * (10:00 PM), and therefore deserve a heads-up to the caller.
     */
    private static boolean isDefaultTimeEligible(CronScheduleType type) {
        return switch (type) {
            case DAILY, WEEKLY, MONTHLY, EVERY_3_MONTHS, EVERY_6_MONTHS, YEARLY, SPECIFIC_DATE -> true;
            default -> false;
        };
    }
}