package com.example.schedule.listener;

import com.example.schedule.service.JobAlertService;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuartzMisfireListener implements TriggerListener {

    private static final Logger log =
            LoggerFactory.getLogger(QuartzMisfireListener.class);

    private final JobAlertService alertService;

    public QuartzMisfireListener(JobAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Returns the unique name of this trigger listener.
     *
     * @return listener name
     */
    @Override
    public String getName() {
        return "GlobalMisfireListener";
    }

    /**
     * Called by Quartz when a trigger has missed its scheduled
     * execution time according to the configured misfire threshold
     * and misfire policy.
     *
     * @param trigger the trigger that was misfired
     */
    @Override
    public void triggerMisfired(Trigger trigger) {

        String jobName = trigger.getJobKey().getName();
        String triggerName = trigger.getKey().getName();

        log.error(
                "QUARTZ MISFIRE DETECTED - Job [{}], Trigger [{}], " +
                        "Scheduled Fire Time [{}], Next Fire Time [{}]",
                jobName,
                triggerName,
                trigger.getPreviousFireTime(),
                trigger.getNextFireTime()
        );

        try {
            alertService.notifyJobFailedPermanently(
                    jobName,
                    "MISFIRE",
                    new RuntimeException(
                            "Job missed its scheduled execution window"
                    )
            );

            log.info(
                    "Misfire alert sent successfully for Job [{}]",
                    jobName
            );

        } catch (Exception e) {
            /*
             * Do not allow an alert/notification failure to interfere
             * with Quartz scheduler processing.
             */
            log.error(
                    "Failed to send misfire notification for Job [{}]",
                    jobName,
                    e
            );
        }
    }

    /**
     * Called by Quartz when a trigger is about to fire.
     *
     * @param trigger the trigger that is firing
     * @param context the job execution context
     */
    @Override
    public void triggerFired(
            Trigger trigger,
            JobExecutionContext context) {

        String jobName = trigger.getJobKey().getName();
        String triggerName = trigger.getKey().getName();

        log.info(
                "QUARTZ TRIGGER FIRED - Trigger [{}], Job [{}]",
                triggerName,
                jobName
        );
    }

    /**
     * Determines whether Quartz should prevent the job from executing.
     *
     * Returning false means the job is allowed to execute normally.
     *
     * Returning true would veto/prevent the job execution.
     *
     * @param trigger the trigger that is about to fire
     * @param context the job execution context
     * @return false to allow job execution
     */
    @Override
    public boolean vetoJobExecution(
            Trigger trigger,
            JobExecutionContext context) {

        return false;
    }

    /**
     * Called by Quartz after the trigger's job execution has completed.
     *
     * @param trigger the completed trigger
     * @param context the job execution context
     * @param triggerInstructionCode Quartz's instruction for what
     *                              should happen to the trigger
     */
    @Override
    public void triggerComplete(
            Trigger trigger,
            JobExecutionContext context,
            Trigger.CompletedExecutionInstruction triggerInstructionCode) {

        String jobName = trigger.getJobKey().getName();
        String triggerName = trigger.getKey().getName();

        log.info(
                "QUARTZ TRIGGER COMPLETED - Trigger [{}], Job [{}], " +
                        "Instruction [{}]",
                triggerName,
                jobName,
                triggerInstructionCode
        );
    }
}