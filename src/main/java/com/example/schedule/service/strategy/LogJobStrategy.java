package com.example.schedule.service.strategy;

import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogJobStrategy implements JobExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(LogJobStrategy.class);

    @Override
    public String getType() { return "LOG"; }

    @Override
    public void execute(JobDataMap dataMap, String idempotencyKey) {
        log.info("[LOG job] {} fired (key={}) with message: {}",
                dataMap.getString("jobName"), idempotencyKey, dataMap.getString("message"));
    }
}