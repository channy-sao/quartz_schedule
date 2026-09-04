package com.example.schedule.service;

import com.example.schedule.dto.CreateScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.dto.UpdateScheduleRequest;
import com.example.schedule.entity.SchedulerConfig;
import com.example.schedule.exception.ScheduleCreationException;
import com.example.schedule.exception.ScheduleNotFoundException;
import com.example.schedule.job.DynamicJob;
import com.example.schedule.repository.SchedulerConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Service
public class ScheduleManagementService {

    private static final String TRIGGER_SUFFIX = "-trigger";

    private final SchedulerConfigRepository configRepository;
    private final Scheduler scheduler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScheduleManagementService(SchedulerConfigRepository configRepository, Scheduler scheduler) {
        this.configRepository = configRepository;
        this.scheduler = scheduler;
    }

    @Transactional
    public SchedulerConfig createSchedule(CreateScheduleRequest request, String createdBy) {

        if (configRepository.existsByJobName(request.jobName())) {
            throw new IllegalArgumentException("Job already exists: " + request.jobName());
        }

        SchedulerConfig config = new SchedulerConfig();
        config.setJobName(request.jobName());
        config.setJobType(request.jobType());
        config.setBusinessName(request.businessName());
        config.setDescription(request.description());
        config.setCronExpression(request.cronExpression());
        config.setTimezone(request.timezone());
        config.setJobData(toJobDataJson(request));
        config.setEnabled(true);
        config.setCreatedBy(createdBy);

        SchedulerConfig saved = configRepository.saveAndFlush(config);

        try {
            registerWithQuartz(saved);
        } catch (Exception e) {
            // Throwing here rolls back the saveAndFlush above (same @Transactional method)
            throw new ScheduleCreationException("Failed to register job with Quartz: " + request.jobName(), e);
        }

        return saved;
    }

    @Transactional
    public SchedulerConfig updateSchedule(String jobName, UpdateScheduleRequest request, String updatedBy) {

        SchedulerConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + jobName));

        config.setCronExpression(request.cronExpression());
        config.setTimezone(request.timezone());
        config.setJobData(toJobDataJson(request));
        config.setUpdatedBy(updatedBy);
        config.setChangeReason(request.changeReason());

        SchedulerConfig saved = configRepository.save(config); // throws OptimisticLockException on conflict

        try {
            rescheduleInQuartz(saved);
        } catch (Exception e) {
            throw new ScheduleCreationException("Failed to reschedule job in Quartz: " + jobName, e);
        }

        return saved;
    }

    @Transactional
    public void deleteSchedule(String jobName) {
        SchedulerConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + jobName));

        try {
            scheduler.deleteJob(JobKey.jobKey(jobName));
        } catch (SchedulerException e) {
            throw new ScheduleCreationException("Failed to delete Quartz job: " + jobName, e);
        }

        configRepository.delete(config);
    }

    @Transactional
    public void setEnabled(String jobName, boolean enabled, String updatedBy) {
        SchedulerConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + jobName));

        try {
            if (enabled) {
                scheduler.resumeJob(JobKey.jobKey(jobName));
            } else {
                scheduler.pauseJob(JobKey.jobKey(jobName));
            }
        } catch (SchedulerException e) {
            throw new ScheduleCreationException("Failed to toggle job state: " + jobName, e);
        }

        config.setEnabled(enabled);
        config.setUpdatedBy(updatedBy);
        configRepository.save(config);
    }

    public void triggerNow(String jobName) {
        try {
            scheduler.triggerJob(JobKey.jobKey(jobName));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to trigger job: " + jobName, e);
        }
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(String jobName) {
        SchedulerConfig config = configRepository.findByJobName(jobName)
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule not found: " + jobName));

        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName + TRIGGER_SUFFIX);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            Trigger.TriggerState state = trigger != null
                    ? scheduler.getTriggerState(triggerKey)
                    : Trigger.TriggerState.NONE;

            return new ScheduleResponse(
                    config.getJobName(),
                    config.getBusinessName(),
                    config.getDescription(),
                    config.getJobType(),
                    config.getCronExpression(),
                    state.name(),
                    trigger != null ? toInstant(trigger.getNextFireTime()) : null,
                    trigger != null ? toInstant(trigger.getPreviousFireTime()) : null,
                    config.getCreatedBy(),
                    config.getUpdatedBy(),
                    config.getUpdatedAt()
            );
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to read Quartz trigger state: " + jobName, e);
        }
    }

    // --- internal helpers ---

    private void registerWithQuartz(SchedulerConfig config) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(config.getJobName());
        TriggerKey triggerKey = TriggerKey.triggerKey(config.getJobName() + TRIGGER_SUFFIX);

        JobDetail jobDetail = JobBuilder.newJob(DynamicJob.class)
                .withIdentity(jobKey)
                .storeDurably()
                .usingJobData(buildJobDataMap(config))
                .build();

        CronScheduleBuilder scheduleBuilder = buildCronSchedule(config);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withSchedule(scheduleBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void rescheduleInQuartz(SchedulerConfig config) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(config.getJobName());
        TriggerKey triggerKey = TriggerKey.triggerKey(config.getJobName() + TRIGGER_SUFFIX);

        JobDetail existing = scheduler.getJobDetail(jobKey);
        if (existing == null) {
            throw new ScheduleNotFoundException("Quartz job missing for: " + config.getJobName());
        }

        JobDetail updated = existing.getJobBuilder()
                .storeDurably()
                .usingJobData(buildJobDataMap(config))
                .build();

        scheduler.addJob(updated, true);

        CronTrigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .withSchedule(buildCronSchedule(config))
                .build();

        scheduler.rescheduleJob(triggerKey, newTrigger);
    }

    private CronScheduleBuilder buildCronSchedule(SchedulerConfig config) {
        CronScheduleBuilder builder = CronScheduleBuilder
                .cronSchedule(config.getCronExpression())
                .withMisfireHandlingInstructionDoNothing();

        if (config.getTimezone() != null && !config.getTimezone().isBlank()) {
            builder = builder.inTimeZone(TimeZone.getTimeZone(config.getTimezone()));
        }
        return builder;
    }

    private JobDataMap buildJobDataMap(SchedulerConfig config) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobName", config.getJobName());
        dataMap.put("jobType", config.getJobType());

        Map<String, Object> extra = fromJson(config.getJobData());
        extra.forEach((k, v) -> dataMap.put(k, v == null ? "" : v.toString()));

        return dataMap;
    }

    private String toJobDataJson(CreateScheduleRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", request.message());
        map.put("recipient", request.recipient());
        map.put("webhookUrl", request.webhookUrl());
        return writeJson(map);
    }

    private String toJobDataJson(UpdateScheduleRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", request.message());
        map.put("recipient", request.recipient());
        map.put("webhookUrl", request.webhookUrl());
        return writeJson(map);
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return json == null ? new HashMap<>() : objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Instant toInstant(java.util.Date date) {
        return date == null ? null : date.toInstant();
    }
}