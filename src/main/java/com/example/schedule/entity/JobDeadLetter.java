package com.example.schedule.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_dead_letter")
public class JobDeadLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "scheduled_fire_time", nullable = false)
    private Instant scheduledFireTime;

    @Column(name = "job_data", columnDefinition = "jsonb", nullable = false)
    private String jobData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    public Long getId() { return id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public Instant getScheduledFireTime() { return scheduledFireTime; }
    public void setScheduledFireTime(Instant scheduledFireTime) { this.scheduledFireTime = scheduledFireTime; }
    public String getJobData() { return jobData; }
    public void setJobData(String jobData) { this.jobData = jobData; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}