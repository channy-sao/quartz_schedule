package com.example.schedule.utils;

import com.example.schedule.dto.CronBuilderRequest;
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

        if (request.type() == com.example.schedule.constant.CronScheduleType.SPECIFIC_DATE
                && request.date() != null
                && request.date().isBefore(java.time.LocalDate.now())) {
            warnings.add("The specified date is in the past — this schedule will never fire.");
        }

        return warnings;
    }
}