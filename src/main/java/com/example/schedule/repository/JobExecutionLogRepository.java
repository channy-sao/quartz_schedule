package com.example.schedule.repository;

import com.example.schedule.entity.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {
    List<JobExecutionLog> findTop50ByJobNameOrderByStartedAtDesc(String jobName);
    Optional<JobExecutionLog> findFirstByJobNameOrderByStartedAtDesc(String jobName);
    // Count total failures for a specific job
    long countByJobNameAndStatus(String jobName, String status);
    long countByStatus(String status);
}