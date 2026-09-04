package com.example.schedule.repository;

import com.example.schedule.entity.SchedulerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchedulerConfigRepository extends JpaRepository<SchedulerConfig, Long> {
    Optional<SchedulerConfig> findByJobName(String jobName);
    boolean existsByJobName(String jobName);
    List<SchedulerConfig> findByEnabledTrue();
}