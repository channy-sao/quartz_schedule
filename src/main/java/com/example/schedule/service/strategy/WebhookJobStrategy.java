package com.example.schedule.service.strategy;

import com.example.schedule.exception.JobPermanentFailureException;
import com.example.schedule.service.strategy.JobExecutionStrategy;
import org.quartz.JobDataMap;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Map;

@Component
public class WebhookJobStrategy implements JobExecutionStrategy {

    private final RestClient restClient = RestClient.create();

    @Override
    public String getType() { return "WEBHOOK"; }

    @Override
    @Retryable(
            retryFor = { RestClientException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void execute(JobDataMap dataMap, String idempotencyKey) {
        restClient.post()
                .uri(dataMap.getString("webhookUrl"))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", dataMap.getString("message")))
                .retrieve()
                .toBodilessEntity();
    }

    @Recover
    public void recover(RestClientException e, JobDataMap dataMap, String idempotencyKey) {
        throw new JobPermanentFailureException("Webhook failed after retries: " + idempotencyKey, e);
    }
}