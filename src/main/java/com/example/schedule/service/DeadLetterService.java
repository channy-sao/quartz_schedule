package com.example.schedule.service;

import com.example.schedule.entity.JobDeadLetter;
import com.example.schedule.repository.JobDeadLetterRepository;
import com.example.schedule.service.strategy.JobExecutionStrategy;
import com.example.schedule.service.strategy.JobExecutionStrategyRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeadLetterService {

    private final JobDeadLetterRepository repository;
    private final JobExecutionStrategyRegistry strategyRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeadLetterService(JobDeadLetterRepository repository,
                               JobExecutionStrategyRegistry strategyRegistry) {
        this.repository = repository;
        this.strategyRegistry = strategyRegistry;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String jobName, String jobType, Instant scheduledFireTime,
                         JobDataMap dataMap, Throwable error) {
        JobDeadLetter dl = new JobDeadLetter();
        dl.setJobName(jobName);
        dl.setJobType(jobType);
        dl.setScheduledFireTime(scheduledFireTime);
        dl.setJobData(toJson(dataMap));
        dl.setErrorMessage(error.getMessage());
        repository.save(dl);
    }

    @Transactional
    public void replay(Long id, String operator) {
        JobDeadLetter dl = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dead letter not found: " + id));

        if (dl.isResolved()) {
            throw new IllegalStateException("Dead letter already resolved: " + id);
        }

        JobExecutionStrategy strategy = strategyRegistry.resolve(dl.getJobType());
        JobDataMap dataMap = fromJson(dl.getJobData());

        try {
            String idempotencyKey = strategy.idempotencyKey(dataMap, dl.getScheduledFireTime());
            strategy.execute(dataMap, idempotencyKey);

            dl.setResolved(true);
            dl.setResolvedAt(Instant.now());
            dl.setResolvedBy(operator);
            repository.save(dl);

        } catch (Exception e) {
            throw new RuntimeException("Replay failed for dead letter " + id + ": " + e.getMessage(), e);
        }
    }

    private String toJson(JobDataMap dataMap) {
        try {
            return objectMapper.writeValueAsString(dataMap.getWrappedMap());
        } catch (Exception e) {
            return "{}";
        }
    }

    private JobDataMap fromJson(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, HashMap.class);
            return new JobDataMap(map);
        } catch (Exception e) {
            return new JobDataMap();
        }
    }
}