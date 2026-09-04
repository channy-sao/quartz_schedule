package com.example.schedule.utils;

import com.example.schedule.dto.CronBuilderRequest;
import org.quartz.CronExpression;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class QuartzCronUtil {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private QuartzCronUtil() {
    }

    /**
     * Build a Quartz Cron expression from a user-friendly request.
     */
    public static String build(CronBuilderRequest request) {

        Objects.requireNonNull(request, "Cron request cannot be null");
        Objects.requireNonNull(request.type(), "Cron schedule type is required");

        String expression = switch (request.type()) {

            case EVERY_SECOND ->
                    everySecond();

            case EVERY_MINUTE ->
                    everyMinute();

            case EVERY_HOUR ->
                    everyHour();

            case DAILY ->
                    daily(request);

            case WEEKLY ->
                    weekly(request);

            case MONTHLY ->
                    monthly(request);

            case YEARLY ->
                    yearly(request);

            case SPECIFIC_DATE ->
                    specificDate(request);

            case SPECIFIC_TIME ->
                    specificTime(request);

            case CUSTOM ->
                    custom(request);
        };

        validate(expression);

        return expression;
    }

    /**
     * Every second.
     *
     * 0/1 * * * * ?
     */
    private static String everySecond() {
        return "0/1 * * * * ?";
    }

    /**
     * Every minute.
     *
     * 0 * * * * ?
     */
    private static String everyMinute() {
        return "0 * * * * ?";
    }

    /**
     * Every hour.
     *
     * 0 0 * * * ?
     */
    private static String everyHour() {
        return "0 0 * * * ?";
    }

    /**
     * Daily at HH:mm.
     *
     * Example:
     * 09:30
     *
     * Result:
     * 0 30 9 * * ?
     */
    private static String daily(CronBuilderRequest request) {

        LocalTime time = parseTime(request.time());

        return String.format(
                "0 %d %d * * ?",
                time.getMinute(),
                time.getHour()
        );
    }

    /**
     * Weekly at HH:mm.
     *
     * Example:
     *
     * daysOfWeek = [MON, WED, FRI]
     * time = 09:30
     *
     * Result:
     *
     * 0 30 9 ? * MON,WED,FRI
     */
    private static String weekly(CronBuilderRequest request) {

        LocalTime time = parseTime(request.time());

        String days = buildDaysOfWeek(request.daysOfWeek());

        return String.format(
                "0 %d %d ? * %s",
                time.getMinute(),
                time.getHour(),
                days
        );
    }

    /**
     * Monthly at HH:mm.
     *
     * Example:
     *
     * dayOfMonth = 15
     * time = 09:30
     *
     * Result:
     *
     * 0 30 9 15 * ?
     *
     * Supports:
     *
     * 1
     * 15
     * L
     * LW
     * 15W
     * L-3
     */
    private static String monthly(CronBuilderRequest request) {

        LocalTime time = parseTime(request.time());

        String dayOfMonth =
                requireValue(
                        request.dayOfMonth(),
                        "dayOfMonth"
                );

        return String.format(
                "0 %d %d %s * ?",
                time.getMinute(),
                time.getHour(),
                dayOfMonth
        );
    }

    /**
     * Yearly at HH:mm.
     *
     * Example:
     *
     * dayOfMonth = 1
     * month = JAN
     * time = 09:00
     *
     * Result:
     *
     * 0 0 9 1 JAN ?
     */
    private static String yearly(CronBuilderRequest request) {

        LocalTime time = parseTime(request.time());

        String dayOfMonth =
                requireValue(
                        request.dayOfMonth(),
                        "dayOfMonth"
                );

        String month =
                requireValue(
                        request.month(),
                        "month"
                );

        return String.format(
                "0 %d %d %s %s ?",
                time.getMinute(),
                time.getHour(),
                dayOfMonth,
                month
        );
    }

    /**
     * Specific date and time.
     *
     * Example:
     *
     * date = 2026-09-15
     * specificTime = 09:30
     *
     * Result:
     *
     * 0 30 9 15 9 ? 2026
     */
    private static String specificDate(CronBuilderRequest request) {

        if (request.date() == null) {
            throw new IllegalArgumentException(
                    "date is required for SPECIFIC_DATE"
            );
        }

        String timeValue =
                request.specificTime() != null
                        ? request.specificTime()
                        : request.time();

        LocalTime time = parseTime(timeValue);

        LocalDate date = request.date();

        return String.format(
                "0 %d %d %d %d ? %d",
                time.getMinute(),
                time.getHour(),
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear()
        );
    }

    /**
     * SPECIFIC_TIME is interpreted as running every day
     * at the specified time.
     *
     * Example:
     *
     * 09:30
     *
     * Result:
     *
     * 0 30 9 * * ?
     */
    private static String specificTime(CronBuilderRequest request) {

        String timeValue =
                request.specificTime() != null
                        ? request.specificTime()
                        : request.time();

        LocalTime time = parseTime(timeValue);

        return String.format(
                "0 %d %d * * ?",
                time.getMinute(),
                time.getHour()
        );
    }

    /**
     * Build a completely custom Quartz Cron expression.
     *
     * Supports all Quartz syntax such as:
     *
     * *
     * ?
     * ,
     * -
     * /
     * L
     * W
     * LW
     * #
     *
     * Examples:
     *
     * 0 0/15 9-17 ? * MON-FRI
     *
     * 0 0 9 ? * MON#2
     *
     * 0 0 23 L * ?
     */
    private static String custom(CronBuilderRequest request) {

        String seconds =
                requireValue(request.seconds(), "seconds");

        String minutes =
                requireValue(request.minutes(), "minutes");

        String hours =
                requireValue(request.hours(), "hours");

        String dayOfMonth =
                requireValue(
                        request.dayOfMonthExpression(),
                        "dayOfMonthExpression"
                );

        String month =
                requireValue(request.month(), "month");

        String dayOfWeek =
                requireValue(request.dayOfWeek(), "dayOfWeek");

        String expression = String.join(
                " ",
                normalize(seconds),
                normalize(minutes),
                normalize(hours),
                normalize(dayOfMonth),
                normalize(month),
                normalize(dayOfWeek)
        );

        if (request.year() != null
                && !request.year().isBlank()) {

            expression += " " + normalize(request.year());
        }

        return expression;
    }

    /**
     * Validate Quartz Cron expression.
     */
    public static void validate(String expression) {

        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException(
                    "Cron expression cannot be blank"
            );
        }

        if (!CronExpression.isValidExpression(expression)) {

            throw new IllegalArgumentException(
                    "Invalid Quartz Cron expression: " + expression
            );
        }
    }

    /**
     * Calculate next execution time.
     */
    public static ZonedDateTime getNextExecution(
            String expression,
            String timezone
    ) {

        validate(expression);

        ZoneId zoneId = getZoneId(timezone);

        CronExpression cron =
                createCronExpression(expression, zoneId);

        Date next =
                cron.getNextValidTimeAfter(new Date());

        if (next == null) {
            return null;
        }

        return next.toInstant().atZone(zoneId);
    }

    /**
     * Calculate multiple upcoming execution times.
     */
    public static List<ZonedDateTime> getNextExecutions(
            String expression,
            String timezone,
            int count
    ) {

        if (count <= 0) {
            throw new IllegalArgumentException(
                    "count must be greater than zero"
            );
        }

        if (count > 100) {
            throw new IllegalArgumentException(
                    "count cannot be greater than 100"
            );
        }

        validate(expression);

        ZoneId zoneId = getZoneId(timezone);

        CronExpression cron =
                createCronExpression(expression, zoneId);

        final Date[] current = {new Date()};

        return java.util.stream.IntStream
                .range(0, count)
                .mapToObj(i -> {

                    Date next =
                            cron.getNextValidTimeAfter(current[0]);

                    if (next == null) {
                        return null;
                    }

                    current[0] = next;

                    return next.toInstant().atZone(zoneId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static CronExpression createCronExpression(
            String expression,
            ZoneId zoneId
    ) {

        try {

            CronExpression cron =
                    new CronExpression(expression);

            cron.setTimeZone(
                    java.util.TimeZone.getTimeZone(zoneId)
            );

            return cron;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Failed to create Quartz Cron expression",
                    e
            );
        }
    }

    private static LocalTime parseTime(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "time is required"
            );
        }

        try {

            return LocalTime.parse(
                    value,
                    TIME_FORMATTER
            );

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "Invalid time: "
                            + value
                            + ". Expected HH:mm",
                    e
            );
        }
    }

    private static String buildDaysOfWeek(
            List<String> days
    ) {

        if (days == null || days.isEmpty()) {

            throw new IllegalArgumentException(
                    "daysOfWeek is required for WEEKLY"
            );
        }

        return days.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(","));
    }

    private static String requireValue(
            String value,
            String field
    ) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        return value;
    }

    private static String normalize(String value) {

        return value
                .trim()
                .replaceAll("\\s+", "");
    }

    private static ZoneId getZoneId(String timezone) {

        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("Asia/Phnom_Penh");
        }

        try {

            return ZoneId.of(timezone);

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Invalid timezone: " + timezone,
                    e
            );
        }
    }
}