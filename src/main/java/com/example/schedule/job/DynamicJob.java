package com.example.schedule.job;

import com.example.schedule.service.DeadLetterService;
import com.example.schedule.service.JobAlertService;
import com.example.schedule.service.JobExecutionAuditService;
import com.example.schedule.service.strategy.JobExecutionStrategy;
import com.example.schedule.service.strategy.JobExecutionStrategyRegistry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;

public class DynamicJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DynamicJob.class);

    @Autowired private JobExecutionStrategyRegistry strategyRegistry;
    @Autowired private JobExecutionAuditService auditService;
    @Autowired private DeadLetterService deadLetterService;
    @Autowired private JobAlertService alertService;
    @Autowired private RetryTemplate jobRetryTemplate;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String jobName = dataMap.getString("jobName");
        String jobType = dataMap.getString("jobType");
        Instant scheduledFireTime = context.getScheduledFireTime() != null
                ? context.getScheduledFireTime().toInstant()
                : Instant.now();

        JobExecutionStrategy strategy;
        try {
            strategy = strategyRegistry.resolve(jobType);
        } catch (IllegalArgumentException e) {
            log.error("Unknown jobType [{}] for job [{}]. Not retrying.", jobType, jobName);
            deadLetterService.record(jobName, jobType, scheduledFireTime, dataMap, e);
            alertService.notifyJobFailedPermanently(jobName, jobType, e);
            return; // don't throw — retrying won't fix an unknown type
        }

        String idempotencyKey = strategy.idempotencyKey(dataMap, scheduledFireTime);

        try {
            // 2. Execute via Spring Retry Template
            jobRetryTemplate.execute(
                    // Retry Callback
                    retryContext -> {
                        int attempt = retryContext.getRetryCount() + 1;
                        log.info("Executing job [{}] attempt [{}]", jobName, attempt);

                        Long logId = auditService.recordStart(jobName, jobType, scheduledFireTime, attempt, idempotencyKey);

                        try {
                            strategy.execute(dataMap, idempotencyKey);
                            auditService.recordSuccess(logId);
                        } catch (Exception e) {
                            auditService.recordFailure(logId, e);
                            throw e; // Rethrow to trigger Spring Retry backoff/retry mechanism
                        }
                        return null;
                    },
                    // Recovery Callback (Triggers when ALL retries fail)
                    recoveryContext -> {
                        Throwable lastError = recoveryContext.getLastThrowable();
                        log.error("Job [{}] failed permanently after retries. Moving to Dead Letter Queue.", jobName, lastError);

                        assert lastError != null;
                        deadLetterService.record(jobName, jobType, scheduledFireTime, dataMap, lastError);
                        alertService.notifyJobFailedPermanently(jobName, jobType, lastError);
                        return null;
                    }
            );

        } catch (Exception e) {

            JobExecutionException jex = new JobExecutionException(e);
            jex.setRefireImmediately(false); // don't spin-loop; next cron tick or manual replay
            throw jex;
        }
    }
}