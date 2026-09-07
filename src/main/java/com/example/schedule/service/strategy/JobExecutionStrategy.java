package com.example.schedule.service.strategy;

import org.quartz.JobDataMap;

import java.time.Instant;

public interface JobExecutionStrategy {

    /**
     * The job type this strategy handles, e.g. "EMAIL", "WEBHOOK", "LOG".
     * Must match CreateScheduleRequest.jobType() values.
     */
    String getType();

    /**
     * Must be stable across retries of the *same* logical execution
     * (same job, same scheduled fire time) so downstream systems
     * can dedupe.
     */
    default String idempotencyKey(JobDataMap dataMap, Instant scheduledFireTime) {
        return dataMap.getString("jobName") + ":" + scheduledFireTime.toEpochMilli();
    }


    void execute(JobDataMap dataMap, String idempotencyKey);
}