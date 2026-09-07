package com.example.schedule.dto;

import java.util.List;

public record CronTypeMetadata(
        String type,
        String label,
        String description,
        List<String> requiredFields,
        List<String> optionalFields,
        String example
) {}