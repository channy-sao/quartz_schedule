package com.example.schedule.service;

import com.example.schedule.dto.CronTypeMetadata;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CronTypeMetadataService {

    public List<CronTypeMetadata> getAll() {
        return List.of(
            new CronTypeMetadata("EVERY_SECOND", "Every second", "Fires once per second.",
                    List.of(), List.of(), "No fields needed"),
            new CronTypeMetadata("EVERY_MINUTE", "Every minute", "Fires once per minute.",
                    List.of(), List.of(), "No fields needed"),
            new CronTypeMetadata("EVERY_HOUR", "Every hour", "Fires once per hour.",
                    List.of(), List.of(), "No fields needed"),
            new CronTypeMetadata("DAILY", "Daily", "Fires every day at a fixed time.",
                    List.of("time"), List.of(), "time: \"09:30\""),
            new CronTypeMetadata("WEEKLY", "Weekly", "Fires on chosen weekdays at a fixed time.",
                    List.of("time", "daysOfWeek"), List.of(),
                    "time: \"09:30\", daysOfWeek: [\"MON\",\"WED\",\"FRI\"]"),
            new CronTypeMetadata("MONTHLY", "Monthly", "Fires on a chosen day of month at a fixed time.",
                    List.of("time", "dayOfMonth"), List.of(),
                    "time: \"09:30\", dayOfMonth: \"15\" (also supports L, LW, 15W, L-3)"),
            new CronTypeMetadata("YEARLY", "Yearly", "Fires once a year on a chosen date/time.",
                    List.of("time", "dayOfMonth", "month"), List.of(),
                    "time: \"09:00\", dayOfMonth: \"1\", month: \"JAN\""),
            new CronTypeMetadata("SPECIFIC_DATE", "One-time (specific date)", "Fires exactly once on a given date/time.",
                    List.of("date"), List.of("specificTime", "time"),
                    "date: \"2026-09-15\", specificTime: \"09:30\""),
            new CronTypeMetadata("SPECIFIC_TIME", "Every day at a specific time", "Alias of DAILY.",
                    List.of(), List.of("specificTime", "time"), "specificTime: \"09:30\""),
            new CronTypeMetadata("CUSTOM", "Custom (advanced)", "Full raw Quartz cron field control.",
                    List.of("seconds", "minutes", "hours", "dayOfMonthExpression", "month", "dayOfWeek"),
                    List.of("year"), "e.g. every 15 min, 9-5, weekdays: seconds=0 minutes=0/15 hours=9-17 dayOfMonthExpression=? month=* dayOfWeek=MON-FRI")
        );
    }
}