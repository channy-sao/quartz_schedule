package com.example.schedule.dto;

import java.time.ZonedDateTime;

public record NextExecutionInfo(
        ZonedDateTime time,
        String relative,   // "in 2h 15m"
        String dayLabel    // "Monday, Sep 14"
) {}