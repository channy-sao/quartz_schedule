package com.example.schedule.controller;

import com.example.schedule.dto.CreateScheduleRequest;
import com.example.schedule.dto.CronBuilderRequest;
import com.example.schedule.dto.CronBuilderResponse;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.dto.UpdateScheduleRequest;
import com.example.schedule.entity.SchedulerConfig;
import com.example.schedule.service.ScheduleManagementService;
import com.example.schedule.utils.QuartzCronUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class SchedulerController {

    private final ScheduleManagementService scheduleService;

    public SchedulerController(ScheduleManagementService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateScheduleRequest request,
                                      @RequestHeader("X-User-Id") String userId) {
        SchedulerConfig config = scheduleService.createSchedule(request, userId);
        return ResponseEntity.ok(config);
    }

    @PutMapping("/{jobName}")
    public ResponseEntity<?> update(@PathVariable String jobName,
                                      @Valid @RequestBody UpdateScheduleRequest request,
                                      @RequestHeader("X-User-Id") String userId) {
        SchedulerConfig config = scheduleService.updateSchedule(jobName, request, userId);
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/{jobName}")
    public ResponseEntity<?> delete(@PathVariable String jobName) {
        scheduleService.deleteSchedule(jobName);
        return ResponseEntity.ok("Schedule deleted successfully");
    }

    @PostMapping("/{jobName}/pause")
    public ResponseEntity<?> pause(@PathVariable String jobName,
                                     @RequestHeader("X-User-Id") String userId) {
        scheduleService.setEnabled(jobName, false, userId);
        return ResponseEntity.ok("Schedule paused successfully");
    }

    @PostMapping("/{jobName}/resume")
    public ResponseEntity<?> resume(@PathVariable String jobName,
                                      @RequestHeader("X-User-Id") String userId) {
        scheduleService.setEnabled(jobName, true, userId);
        return ResponseEntity.ok("Schedule resumed successfully");
    }

    @PostMapping("/{jobName}/trigger")
    public ResponseEntity<?> trigger(@PathVariable String jobName) {
        scheduleService.triggerNow(jobName);
        return ResponseEntity.ok("Job triggered successfully");
    }

    @GetMapping("/{jobName}")
    public ResponseEntity<ScheduleResponse> get(@PathVariable String jobName) {
        return ResponseEntity.ok(scheduleService.getSchedule(jobName));
    }

    /**
     * Generate and validate Quartz Cron expression.
     *
     * POST /api/schedules/cron
     */
    @PostMapping("/cron")
    public ResponseEntity<CronBuilderResponse> generateCron(
            @Valid @RequestBody CronBuilderRequest request) {

        String cronExpression =
                QuartzCronUtil.build(request);

        String timezone =
                request.timezone() == null
                        || request.timezone().isBlank()
                        ? "Asia/Phnom_Penh"
                        : request.timezone();

        ZonedDateTime nextExecution =
                QuartzCronUtil.getNextExecution(
                        cronExpression,
                        timezone
                );

        List<ZonedDateTime> nextExecutions =
                QuartzCronUtil.getNextExecutions(
                        cronExpression,
                        timezone,
                        5
                );

        return ResponseEntity.ok(
                new CronBuilderResponse(
                        true,
                        request.type().name(),
                        cronExpression,
                        timezone,
                        nextExecution,
                        nextExecutions
                )
        );
    }
}