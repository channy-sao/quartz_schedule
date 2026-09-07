package com.example.schedule.job;

import com.example.schedule.entity.SchedulerConfig;
import com.example.schedule.repository.SchedulerConfigRepository;
import com.example.schedule.service.DeadLetterService;
import com.example.schedule.service.JobAlertService;
import com.example.schedule.service.JobExecutionAuditService;
import com.example.schedule.service.strategy.JobExecutionStrategy;
import com.example.schedule.service.strategy.JobExecutionStrategyRegistry;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dynamic Quartz Job implementation.
 *
 * Includes:
 * - @DisallowConcurrentExecution: Prevents duplicate concurrent runs across cluster nodes.
 * - Dynamic Strategy Resolution: Maps jobType to execution strategy.
 * - Configurable Timeouts: Timeboxes long-running tasks using CompletableFuture.
 * - Spring Retry: Performs automatic retries with backoff.
 * - Audit & DLQ: Records execution metrics, dead-letter records, and alerts on failure.
 */
@Component
@DisallowConcurrentExecution
public class DynamicJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(DynamicJob.class);

    // Unbounded cached executor dedicated to wrapping strategy executions with timeouts
    private static final ExecutorService TASK_EXECUTOR = Executors.newCachedThreadPool();

    @Autowired private JobExecutionStrategyRegistry strategyRegistry;
    @Autowired private JobExecutionAuditService auditService;
    @Autowired private DeadLetterService deadLetterService;
    @Autowired private JobAlertService alertService;
    @Autowired private RetryTemplate jobRetryTemplate;
    @Autowired private SchedulerConfigRepository configRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String jobName = dataMap.getString("jobName");
        String jobType = dataMap.getString("jobType");

        // 1. Determine fire time
        Instant scheduledFireTime = context.getScheduledFireTime() != null
                ? context.getScheduledFireTime().toInstant()
                : Instant.now();

        // 2. Fetch configured timeout (defaults to 300 seconds / 5 minutes)
        int timeoutSeconds = configRepository.findByJobName(jobName)
                .map(SchedulerConfig::getTimeoutSeconds)
                .orElse(300);

        // 3. Resolve execution strategy based on jobType
        final JobExecutionStrategy strategy;
        try {
            strategy = strategyRegistry.resolve(jobType);
        } catch (IllegalArgumentException e) {
            log.error("Unknown jobType [{}] for job [{}]. Skipping execution.", jobType, jobName);
            deadLetterService.record(jobName, jobType, scheduledFireTime, dataMap, e);
            alertService.notifyJobFailedPermanently(jobName, jobType, e);
            return; // Exit without throwing; retrying won't resolve an unmapped job strategy
        }

        // 4. Resolve idempotency key (manual executionId or strategy-generated)
        String executionId = dataMap.getString("executionId");
        String idempotencyKey = (executionId != null && !executionId.isBlank())
                ? executionId
                : strategy.idempotencyKey(dataMap, scheduledFireTime);

        // 5. Execute via Spring Retry Template with Timeout Guard
        try {
            jobRetryTemplate.execute(
                    // Retry Callback
                    retryContext -> {
                        int attempt = retryContext.getRetryCount() + 1;
                        log.info("Executing job [{}] (type: [{}], attempt: [{}])", jobName, jobType, attempt);

                        Long logId = auditService.recordStart(jobName, jobType, scheduledFireTime, attempt, idempotencyKey);

                        CompletableFuture<Void> taskFuture = null;
                        try {
                            // Run the strategy task asynchronously to enforce timeout
                            taskFuture = CompletableFuture.runAsync(
                                    () ->
                                            strategy.execute(dataMap, idempotencyKey),TASK_EXECUTOR
                            );

                            // Block and wait until completion or timeout
                            taskFuture.get(timeoutSeconds, TimeUnit.SECONDS);

                            auditService.recordSuccess(logId);
                        } catch (TimeoutException e) {
                            taskFuture.cancel(true); // Signal thread cancellation/interrupt
                            TimeoutException timeoutError = new TimeoutException(
                                    String.format("Job [%s] timed out after %d seconds on attempt %d", jobName, timeoutSeconds, attempt)
                            );
                            auditService.recordFailure(logId, timeoutError);
                            throw timeoutError; // Rethrow to trigger Spring Retry
                        } catch (Exception e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            auditService.recordFailure(logId, cause);
                            throw new RuntimeException(cause); // Rethrow to trigger Spring Retry
                        }
                        return null;
                    },
                    // Recovery Callback (Fires when ALL retry attempts fail)
                    recoveryContext -> {
                        Throwable lastError = recoveryContext.getLastThrowable();
                        log.error("Job [{}] failed permanently after all retry attempts.", jobName, lastError);

                        Throwable errorToRecord = lastError != null ? lastError : new RuntimeException("Unknown execution failure");
                        deadLetterService.record(jobName, jobType, scheduledFireTime, dataMap, errorToRecord);
                        alertService.notifyJobFailedPermanently(jobName, jobType, errorToRecord);
                        return null;
                    }
            );

        } catch (Exception e) {
            log.error("Unhandled exception processing execution for job [{}]", jobName, e);
            JobExecutionException jex = new JobExecutionException(e);
            jex.setRefireImmediately(false); // បើកំណត់ true៖ ប្រាប់ Quartz ថា "Job នេះទើបតែ Crash/Error! សូមរត់វាឡើងវិញភ្លាមៗនៅនឹងកន្លែង។"
                                            //បើកំណត់ false (ដូចក្នុង Code របស់អ្នក)៖ ប្រាប់ Quartz ថា "កុំរត់វាឡើងវិញភ្លាមៗ! យើងបានចាត់ចែងការ Retry រួចហើយតាមរយៈ Spring Retry
            throw jex;
        }
    }

  /***
   * setRefireImmediately(false) មិនបានរារាំង Quartz ពីការដំណើរការ Misfire នោះទេ។ Misfire និង setRefireImmediately គឺជាមុខងារពីរខុសគ្នាទាំងស្រុងនៅក្នុង Quartz៖
   *
   * តើ setRefireImmediately(false) ធ្វើការអ្វីពិតប្រាកដ?
   * setRefireImmediately គ្រប់គ្រង Exception (កំហុស) ដែលកើតឡើងពេលកំពុង រត់ (Runtime Exception) មិនមែនគ្រប់គ្រង Misfire ទេ៖
   *
   * វាប្រាប់ Quartz ពីអ្វីត្រូវធ្វើ នៅពេលដែល Job ទម្លាក់ JobExecutionException ពេលកំពុង run។
   *
   * បើកំណត់ true៖ ប្រាប់ Quartz ថា "Job នេះទើបតែ Crash/Error! សូមរត់វាឡើងវិញភ្លាមៗនៅនឹងកន្លែង។"
   *
   * បើកំណត់ false (ដូចក្នុង Code របស់អ្នក)៖ ប្រាប់ Quartz ថា "កុំរត់វាឡើងវិញភ្លាមៗ! យើងបានចាត់ចែងការ Retry រួចហើយតាមរយៈ Spring Retry។"
   *
   * តើ Quartz គ្រប់គ្រង Misfire យ៉ាងដូចម្តេច?
   * Misfire កើតឡើង មុនពេល Job ចាប់ផ្តើមរត់ (ឧទាហរណ៍៖ Server ត្រូវបានបិទ ឬ Thread ពេញ ពេលដល់ម៉ោងត្រូវរត់)។
   *
   * ការងារ Misfire ត្រូវបានគ្រប់គ្រងដោយ Misfire Instruction របស់ Trigger (មិនមែន setRefireImmediately ទេ) ដូចជា៖
   *
   * MISFIRE_INSTRUCTION_FIRE_ONCE_NOW៖ រត់ Job ដែលខកខាននោះភ្លាមៗ នៅពេល Server ដំណើរការឡើងវិញ។
   *
   * MISFIRE_INSTRUCTION_DO_NOTHING៖ រំលងការរត់ដែលខកខាន ហើយរង់ចាំម៉ោង Cron បន្ទាប់ទៀត។
   *
   * ហេតុអ្វីបានជា setRefireImmediately(false) ជាជម្រើសត្រឹមត្រូវក្នុង Code របស់អ្នក?
   * Spring Retry បានចាត់ចែងការ Retry រួចហើយ៖ នៅក្នុង DynamicJob របស់លោកអ្នក មាន jobRetryTemplate ដែលព្យាយាមរត់ឡើងវិញនៅពេលមានបញ្ហា។
   *
   * ការពារការ Retry ជាន់គ្នា (Duplicate Retry Loop)៖ ប្រសិនបើអ្នកដាក់ true នោះ Quartz នឹងបង្ខំឱ្យរត់ឡើងវិញភ្លាមៗ បន្ថែមលើ Spring Retry ដែលនាំឱ្យមានការរត់ជាន់គ្នាច្រើនដង។
   *
   * Misfire នៅតែធ្វើការធម្មតា៖ ប្រសិនបើ Quartz ខកខានម៉ោងរត់ QuartzMisfireListener របស់អ្នកនៅតែចាប់បាន ហើយ Misfire Policy របស់ Trigger នឹងនៅតែដំណើរការជាធម្មតា។
   */
}
