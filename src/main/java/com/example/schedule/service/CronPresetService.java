package com.example.schedule.service;

import com.example.schedule.dto.CronBuilderRequest;
import com.example.schedule.dto.CronPreset;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CronPresetService {

    public List<CronPreset> getAll() {
        return List.of(
                new CronPreset("every-15-min", "Every 15 minutes", "Good for polling/health checks",
                        CronBuilderRequest.custom("0", "0/15", "*", "?", "*", "*", null, null)),

                new CronPreset("business-hours-hourly", "Every hour, 9am-5pm weekdays",
                        "Good for reports during work hours",
                        CronBuilderRequest.custom("0", "0", "9-17", "?", "*", "MON-FRI", null, null)),

                new CronPreset("daily-morning", "Every day at 9:00 AM", "Classic daily digest time",
                        CronBuilderRequest.daily("09:00", null)),

                new CronPreset("weekday-morning", "Every weekday at 9:00 AM", "Standard Mon-Fri kickoff",
                        CronBuilderRequest.weekly(List.of("MON", "TUE", "WED", "THU", "FRI"), "09:00", null)),

                new CronPreset("end-of-month", "Last day of the month at 11:59 PM", "Month-end close jobs",
                        CronBuilderRequest.monthly("L", "23:59", null)),

                new CronPreset("midnight-daily", "Every day at midnight", "Daily cleanup/reconciliation",
                        CronBuilderRequest.daily("00:00", null))
        );
    }
}