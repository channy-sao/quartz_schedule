package com.example.schedule.service;

import com.example.schedule.dto.CronBuilderRequest;
import com.example.schedule.dto.CronBuilderResponse;
import com.example.schedule.dto.CronDescribeRequest;
import com.example.schedule.dto.NextExecutionInfo;
import com.example.schedule.utils.CronDescriptionUtil;
import com.example.schedule.utils.CronWarningUtil;
import com.example.schedule.utils.QuartzCronUtil;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CronBuilderService {

    private static final DateTimeFormatter DAY_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d 'at' h:mm a");

    /**
     * Build a full CronBuilderResponse (expression, human description,
     * next runs, warnings) from a structured request.
     *
     * Static so it can be called from anywhere (controller, presets,
     * favorites) without needing a Spring-managed instance.
     */
    public static CronBuilderResponse buildResponse(CronBuilderRequest request) {

        String cronExpression = QuartzCronUtil.build(request);

        String timezone = request.timezone() == null || request.timezone().isBlank()
                ? "Asia/Phnom_Penh"
                : request.timezone();

        ZonedDateTime next = QuartzCronUtil.getNextExecution(cronExpression, timezone);
        List<ZonedDateTime> upcoming = QuartzCronUtil.getNextExecutions(cronExpression, timezone, 5);

        String humanDescription = CronDescriptionUtil.describeFriendly(request);
        if (humanDescription == null) {
            // CUSTOM type has no hand-written template — fall back to cron-utils
            humanDescription = CronDescriptionUtil.describe(cronExpression);
        }

        List<String> warnings = CronWarningUtil.check(request, cronExpression);

        return new CronBuilderResponse(
                true,
                request.type().name(),
                cronExpression,
                timezone,
                humanDescription,
                toInfo(next),
                upcoming.stream().map(CronBuilderService::toInfo).toList(),
                warnings
        );
    }

    /**
     * Reverse lookup: given a raw Quartz cron string, describe it in
     * plain English and compute upcoming fire times.
     */
    public static CronBuilderResponse describe(CronDescribeRequest request) {

        QuartzCronUtil.validate(request.cronExpression());

        String timezone = request.timezone() == null || request.timezone().isBlank()
                ? "Asia/Phnom_Penh"
                : request.timezone();

        ZonedDateTime next = QuartzCronUtil.getNextExecution(request.cronExpression(), timezone);
        List<ZonedDateTime> upcoming = QuartzCronUtil.getNextExecutions(request.cronExpression(), timezone, 5);

        return new CronBuilderResponse(
                true,
                "CUSTOM",
                request.cronExpression(),
                timezone,
                CronDescriptionUtil.describe(request.cronExpression()),
                toInfo(next),
                upcoming.stream().map(CronBuilderService::toInfo).toList(),
                List.of()
        );
    }

    private static NextExecutionInfo toInfo(ZonedDateTime time) {
        if (time == null) {
            return null;
        }
        return new NextExecutionInfo(
                time,
                CronDescriptionUtil.relativeLabel(time),
                time.format(DAY_LABEL_FORMAT)
        );
    }
}