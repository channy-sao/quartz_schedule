package com.example.schedule.utils;

import java.util.UUID;
import java.util.regex.Pattern;

public final class JobNameGenerator {
    private static final String JOB_NAME_PREFIX = "AMK-BANK-SCHEDULER-JOB";

    private JobNameGenerator() {}

    /**
     * Generates a unique, storage-safe job name derived from the
     */
    public static String generate() {

        String uniqueId = UUID.randomUUID().toString();

        return JOB_NAME_PREFIX + "-" + uniqueId.toLowerCase().replaceAll("-", "");
    }
}