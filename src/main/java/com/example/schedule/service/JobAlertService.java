package com.example.schedule.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobAlertService {

    private static final Logger log = LoggerFactory.getLogger(JobAlertService.class);

    public void notifyJobFailedPermanently(String jobName, String jobType, Throwable error) {
        // Replace with real integration: PagerDuty, Slack webhook, SIEM, email distro.
        // Must never throw — a broken alert channel must not mask the original failure.
        try {
            log.error("ALERT: Job [{}] type [{}] failed permanently: {}", jobName, jobType, error.getMessage());
        } catch (Exception ignored) {
            // swallow — alerting must be best-effort
        }
    }

    public void notifyDrift(String message) {
        try {
            log.warn("ALERT (drift): {}", message);
        } catch (Exception ignored) {}
    }
}