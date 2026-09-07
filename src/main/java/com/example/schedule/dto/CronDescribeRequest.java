package com.example.schedule.dto;

public record CronDescribeRequest(
        String cronExpression,
        String timezone
) {}