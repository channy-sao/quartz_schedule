package com.example.schedule.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_execution_log")
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "scheduled_fire_time", nullable = false)
    private Instant scheduledFireTime;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "status", nullable = false)
    private String status; // STARTED, SUCCESS, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stacktrace", columnDefinition = "TEXT")
    private String errorStacktrace;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    public JobExecutionLog() {}

    public JobExecutionLog(String jobName, String jobType, Instant scheduledFireTime,
                             int attemptNumber, String status, String idempotencyKey) {
        this.jobName = jobName;
        this.jobType = jobType;
        this.scheduledFireTime = scheduledFireTime;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getJobName() { return jobName; }
    public String getJobType() { return jobType; }
    public Instant getScheduledFireTime() { return scheduledFireTime; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorStacktrace() { return errorStacktrace; }
    public void setErrorStacktrace(String errorStacktrace) { this.errorStacktrace = errorStacktrace; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
}