package com.example.schedule.dto;

public record CronPreset(
        String id,
        String label,
        String description,
        CronBuilderRequest request
) {}