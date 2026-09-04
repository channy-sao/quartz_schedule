package com.example.schedule.service;

import com.example.schedule.entity.JobExecutionLog;
import com.example.schedule.repository.JobExecutionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class JobExecutionAuditService {

    private final JobExecutionLogRepository repository;

    public JobExecutionAuditService(JobExecutionLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long recordStart(String jobName, String jobType, Instant scheduledFireTime,
                              int attempt, String idempotencyKey) {
        JobExecutionLog log = new JobExecutionLog(
                jobName, jobType, scheduledFireTime, attempt, "STARTED", idempotencyKey);
        return repository.save(log).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long logId) {
        repository.findById(logId).ifPresent(l -> {
            l.setStatus("SUCCESS");
            l.setCompletedAt(Instant.now());
            repository.save(l);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMisfire(String jobName, String jobType, Instant scheduledFireTime, String reason) {
        JobExecutionLog log = new JobExecutionLog(
                jobName,
                jobType != null ? jobType : "UNKNOWN",
                scheduledFireTime != null ? scheduledFireTime : Instant.now(),
                1,
                "MISFIRED",
                "MISFIRE-" + System.currentTimeMillis()
        );
        log.setCompletedAt(Instant.now());
        log.setErrorMessage(reason);
        repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long logId, Throwable error) {
        repository.findById(logId).ifPresent(l -> {
            l.setStatus("FAILED");
            l.setCompletedAt(Instant.now());
            l.setErrorMessage(error.getMessage());
            l.setErrorStacktrace(stackTraceOf(error));
            repository.save(l);
        });
    }

    private String stackTraceOf(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        String trace = sw.toString();
        return trace.length() > 4000 ? trace.substring(0, 4000) : trace;
    }
}