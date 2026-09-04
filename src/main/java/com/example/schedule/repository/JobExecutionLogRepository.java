package com.example.schedule.repository;

import com.example.schedule.entity.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {
    List<JobExecutionLog> findTop50ByJobNameOrderByStartedAtDesc(String jobName);
}