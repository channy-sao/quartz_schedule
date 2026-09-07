package com.example.schedule.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record CronBuilderResponse(
        boolean valid,
        String type,
        String cronExpression,
        String timezone,
        String humanReadableDescription,
        NextExecutionInfo nextExecution,
        List<NextExecutionInfo> nextExecutions,
        List<String> warnings
) {}