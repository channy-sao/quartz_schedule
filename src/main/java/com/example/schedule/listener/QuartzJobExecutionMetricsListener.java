package com.example.schedule.listener;

import com.example.schedule.service.JobMetricsService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.stereotype.Component;

@Component
public class QuartzJobExecutionMetricsListener implements JobListener {

    private static final String START_TIME = "startTimeMs";
    private final JobMetricsService metricsService;

    public QuartzJobExecutionMetricsListener(JobMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public String getName() {
        return "GlobalJobExecutionMetricsListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        context.put(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {}

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobType = (String) context.getMergedJobDataMap().getOrDefault("jobType", "UNKNOWN");

        Long startTime = (Long) context.get(START_TIME);
        long durationMs = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0;

        if (jobException == null) {
            metricsService.recordSuccess(jobName, jobType, durationMs);
        } else {
            metricsService.recordFailure(jobName, jobType);
        }
    }
}