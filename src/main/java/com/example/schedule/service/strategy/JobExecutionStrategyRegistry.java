package com.example.schedule.service.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobExecutionStrategyRegistry {

    private final Map<String, JobExecutionStrategy> strategies;

    public JobExecutionStrategyRegistry(List<JobExecutionStrategy> strategyBeans) {
        this.strategies = strategyBeans.stream()
                .collect(Collectors.toMap(
                        JobExecutionStrategy::getType,
                        Function.identity()
                ));
    }

    public JobExecutionStrategy resolve(String jobType) {
        JobExecutionStrategy strategy = strategies.get(jobType);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy registered for jobType: " + jobType);
        }
        return strategy;
    }
}