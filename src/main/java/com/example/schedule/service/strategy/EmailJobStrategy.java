package com.example.schedule.service.strategy;

import com.example.schedule.service.strategy.JobExecutionStrategy;
import org.quartz.JobDataMap;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class EmailJobStrategy implements JobExecutionStrategy {

    private final JavaMailSender mailSender;

    public EmailJobStrategy(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getType() { return "EMAIL"; }

    @Override
    @Retryable(
            retryFor = { org.springframework.mail.MailException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 8000)
    )
    public void execute(JobDataMap dataMap, String idempotencyKey) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(dataMap.getString("recipient"));
        mail.setSubject("Scheduled notification [" + idempotencyKey + "]");
        mail.setText(dataMap.getString("message"));
        mailSender.send(mail);
    }

    @Recover
    public void recover(org.springframework.mail.MailException e, JobDataMap dataMap, String idempotencyKey) {
        throw new com.example.schedule.exception.JobPermanentFailureException(
                "Email failed after retries: " + idempotencyKey, e);
    }
}