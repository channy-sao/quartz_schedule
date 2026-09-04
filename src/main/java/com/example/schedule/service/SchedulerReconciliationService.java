package com.example.schedule.service;

import com.example.schedule.entity.SchedulerConfig;
import com.example.schedule.repository.SchedulerConfigRepository;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SchedulerReconciliationService {

    private final SchedulerConfigRepository configRepository;
    private final Scheduler scheduler;
    private final JobAlertService alertService;

    public SchedulerReconciliationService(SchedulerConfigRepository configRepository,
                                            Scheduler scheduler,
                                            JobAlertService alertService) {
        this.configRepository = configRepository;
        this.scheduler = scheduler;
        this.alertService = alertService;
    }

    @Scheduled(cron = "0 */15 * * * *") // plain Spring @Scheduled, not Quartz — deliberately separate
    public void reconcile() {
        List<SchedulerConfig> enabledConfigs = configRepository.findByEnabledTrue();

        for (SchedulerConfig config : enabledConfigs) {
            try {
                if (!scheduler.checkExists(JobKey.jobKey(config.getJobName()))) {
                    alertService.notifyDrift(
                            "Config enabled but Quartz job missing: " + config.getJobName());
                }
            } catch (SchedulerException e) {
                alertService.notifyDrift("Reconciliation check failed for " + config.getJobName());
            }
        }
    }
}