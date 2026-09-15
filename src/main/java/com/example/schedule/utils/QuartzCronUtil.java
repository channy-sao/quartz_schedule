package com.example.schedule.utils;

import com.example.schedule.dto.CronBuilderRequest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.quartz.CronExpression;

public final class QuartzCronUtil {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  /** Default processing time when the user doesn't pick one: 10:00 PM. */
  public static final LocalTime DEFAULT_PROCESS_TIME = LocalTime.of(22, 0);

  private QuartzCronUtil() {}

  /**
   * Build a Quartz Cron expression from a user-friendly request.
   *
   * <p>Note: the returned expression encodes the RECURRENCE PATTERN only. request.startDate() /
   * request.endDate() are validated here but must be applied separately on the Quartz Trigger
   * (TriggerBuilder.startAt(...)/.endAt(...)) by the caller that actually schedules the job.
   */
  public static String build(CronBuilderRequest request) {

    Objects.requireNonNull(request, "Cron request cannot be null");
    Objects.requireNonNull(request.type(), "Cron schedule type is required");

    validateDateRange(request.startDate(), request.endDate());

    String expression =
        switch (request.type()) {
          case EVERY_SECOND -> everySecond();

          case EVERY_MINUTE -> everyMinute();

          case EVERY_HOUR -> everyHour();

          case DAILY -> daily(request);

          case WEEKLY -> weekly(request);

          case MONTHLY -> monthly(request);

          case EVERY_3_MONTHS -> everyNMonths(request, 3);

          case EVERY_6_MONTHS -> everyNMonths(request, 6);

          case YEARLY -> yearly(request);

          case SPECIFIC_DATE -> specificDate(request);

          case SPECIFIC_TIME -> specificTime(request);

          case CUSTOM -> custom(request);
        };

    validate(expression);

    return expression;
  }

  /**
   * Every second.
   *
   * <p>0/1 * * * * ?
   */
  private static String everySecond() {
    return "0/1 * * * * ?";
  }

  /**
   * Every minute.
   *
   * <p>0 * * * * ?
   */
  private static String everyMinute() {
    return "0 * * * * ?";
  }

  /**
   * Every hour.
   *
   * <p>0 0 * * * ?
   */
  private static String everyHour() {
    return "0 0 * * * ?";
  }

  /**
   * Daily at HH:mm (defaults to 22:00 if no time given).
   *
   * <p>Start/end date come from request.startDate()/endDate() and belong on the Trigger, not here.
   *
   * <p>Example: (no time given)
   *
   * <p>Result: 0 0 22 * * ?
   */
  private static String daily(CronBuilderRequest request) {

    LocalTime time = resolveTime(request.time());

    return String.format("0 %d %d * * ?", time.getMinute(), time.getHour());
  }

  /**
   * Weekly at HH:mm (defaults to 22:00 if no time given).
   *
   * <p>Example:
   *
   * <p>daysOfWeek = [MON, WED, FRI]
   *
   * <p>Result:
   *
   * <p>0 0 22 ? * MON,WED,FRI
   */
  private static String weekly(CronBuilderRequest request) {

    LocalTime time = resolveTime(request.time());

    String days = buildDaysOfWeek(request.daysOfWeek());

    return String.format("0 %d %d ? * %s", time.getMinute(), time.getHour(), days);
  }

  /**
   * Monthly at HH:mm on a given day of month (defaults to 22:00 if no time given).
   *
   * <p>Example:
   *
   * <p>dayOfMonth = 15
   *
   * <p>Result:
   *
   * <p>0 0 22 15 * ?
   *
   * <p>Supports:
   *
   * <p>1 15 L LW 15W L-3
   */
  private static String monthly(CronBuilderRequest request) {

    LocalTime time = resolveTime(request.time());

    String dayOfMonth = requireValue(request.dayOfMonth(), "dayOfMonth");

    return String.format("0 %d %d %s * ?", time.getMinute(), time.getHour(), dayOfMonth);
  }

  /**
   * Every N months (used for EVERY_3_MONTHS and EVERY_6_MONTHS) on a given day of month, defaulting
   * to 22:00 if no time given. The recurring months are anchored off request.startDate()'s month,
   * e.g. a March start with interval 3 fires in March/June/September/December.
   *
   * <p>Example:
   *
   * <p>startDate = 2026-03-10, dayOfMonth = 10, interval = 3
   *
   * <p>Result:
   *
   * <p>0 0 22 10 3,6,9,12 ?
   */
  private static String everyNMonths(CronBuilderRequest request, int intervalMonths) {

    LocalTime time = resolveTime(request.time());

    String dayOfMonth = requireValue(request.dayOfMonth(), "dayOfMonth");

    String months = monthsForInterval(requireStartDate(request), intervalMonths);

    return String.format("0 %d %d %s %s ?", time.getMinute(), time.getHour(), dayOfMonth, months);
  }

  /**
   * Yearly ("Annually") at HH:mm on a given day of month (defaults to 22:00 if no time given). The
   * month comes from request.month() when supplied, otherwise it's derived from
   * request.startDate()'s month.
   *
   * <p>Example:
   *
   * <p>dayOfMonth = 1 startDate = 2026-01-15 (month not explicitly supplied)
   *
   * <p>Result:
   *
   * <p>0 0 22 1 1 ?
   */
  private static String yearly(CronBuilderRequest request) {

    LocalTime time = resolveTime(request.time());

    String dayOfMonth = requireValue(request.dayOfMonth(), "dayOfMonth");

    String month =
        (request.month() != null && !request.month().isBlank())
            ? request.month()
            : String.valueOf(requireStartDate(request).getMonthValue());

    return String.format("0 %d %d %s %s ?", time.getMinute(), time.getHour(), dayOfMonth, month);
  }

  /**
   * "One Time": specific date and time, defaulting to 22:00 if no time given.
   *
   * <p>Example:
   *
   * <p>date = 2026-09-15
   *
   * <p>Result:
   *
   * <p>0 0 22 15 9 ? 2026
   */
  private static String specificDate(CronBuilderRequest request) {

    if (request.date() == null) {
      throw new IllegalArgumentException("date is required for SPECIFIC_DATE");
    }

    String timeValue = request.specificTime() != null ? request.specificTime() : request.time();

    LocalTime time = resolveTime(timeValue);

    LocalDate date = request.date();

    return String.format(
        "0 %d %d %d %d ? %d",
        time.getMinute(),
        time.getHour(),
        date.getDayOfMonth(),
        date.getMonthValue(),
        date.getYear());
  }

  /**
   * SPECIFIC_TIME is interpreted as running every day at the specified time.
   *
   * <p>Example:
   *
   * <p>09:30
   *
   * <p>Result:
   *
   * <p>0 30 9 * * ?
   */
  private static String specificTime(CronBuilderRequest request) {

    String timeValue = request.specificTime() != null ? request.specificTime() : request.time();

    LocalTime time = resolveTime(timeValue);

    return String.format("0 %d %d * * ?", time.getMinute(), time.getHour());
  }

  /**
   * Build a completely custom Quartz Cron expression.
   *
   * <p>Supports all Quartz syntax such as:
   *
   * <p>* ? , - / L W LW #
   *
   * <p>Examples:
   *
   * <p>0 0/15 9-17 ? * MON-FRI
   *
   * <p>0 0 9 ? * MON#2
   *
   * <p>0 0 23 L * ?
   */
  private static String custom(CronBuilderRequest request) {

    String seconds = requireValue(request.seconds(), "seconds");

    String minutes = requireValue(request.minutes(), "minutes");

    String hours = requireValue(request.hours(), "hours");

    String dayOfMonth = requireValue(request.dayOfMonthExpression(), "dayOfMonthExpression");
    String dayOfWeek = requireValue(request.dayOfWeek(), "dayOfWeek");

    boolean domIsWildcard = dayOfMonth.equals("?");
    boolean dowIsWildcard = dayOfWeek.equals("?");

    if (!domIsWildcard && !dowIsWildcard) {
      throw new IllegalArgumentException(
          "Quartz cron requires either dayOfMonthExpression or dayOfWeek to be '?' — both cannot be specific values at once.");
    }

    String month = requireValue(request.month(), "month");

    String expression =
        String.join(
            " ",
            normalize(seconds),
            normalize(minutes),
            normalize(hours),
            normalize(dayOfMonth),
            normalize(month),
            normalize(dayOfWeek));

    if (request.year() != null && !request.year().isBlank()) {

      expression += " " + normalize(request.year());
    }

    return expression;
  }

  /** Validate Quartz Cron expression. */
  public static void validate(String expression) {

    if (expression == null || expression.isBlank()) {
      throw new IllegalArgumentException("Cron expression cannot be blank");
    }

    if (!CronExpression.isValidExpression(expression)) {

      throw new IllegalArgumentException("Invalid Quartz Cron expression: " + expression);
    }
  }

  /** Calculate next execution time. */
  public static ZonedDateTime getNextExecution(String expression, String timezone) {

    validate(expression);

    ZoneId zoneId = getZoneId(timezone);

    CronExpression cron = createCronExpression(expression, zoneId);

    Date next = cron.getNextValidTimeAfter(new Date());

    if (next == null) {
      return null;
    }

    return next.toInstant().atZone(zoneId);
  }

  /** Calculate multiple upcoming execution times. */
  public static List<ZonedDateTime> getNextExecutions(
      String expression, String timezone, int count) {

    if (count <= 0) {
      throw new IllegalArgumentException("count must be greater than zero");
    }

    if (count > 100) {
      throw new IllegalArgumentException("count cannot be greater than 100");
    }

    validate(expression);

    ZoneId zoneId = getZoneId(timezone);

    CronExpression cron = createCronExpression(expression, zoneId);

    final Date[] current = {new Date()};

    return java.util.stream.IntStream.range(0, count)
        .mapToObj(
            i -> {
              Date next = cron.getNextValidTimeAfter(current[0]);

              if (next == null) {
                return null;
              }

              current[0] = next;

              return next.toInstant().atZone(zoneId);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static CronExpression createCronExpression(String expression, ZoneId zoneId) {

    try {

      CronExpression cron = new CronExpression(expression);

      cron.setTimeZone(java.util.TimeZone.getTimeZone(zoneId));

      return cron;

    } catch (Exception e) {

      throw new IllegalArgumentException("Failed to create Quartz Cron expression", e);
    }
  }

  /** Parses HH:mm, or falls back to DEFAULT_PROCESS_TIME (22:00) when blank/null. */
  private static LocalTime resolveTime(String value) {

    if (value == null || value.isBlank()) {
      return DEFAULT_PROCESS_TIME;
    }

    try {

      return LocalTime.parse(value, TIME_FORMATTER);

    } catch (DateTimeParseException e) {

      throw new IllegalArgumentException("Invalid time: " + value + ". Expected HH:mm", e);
    }
  }

  private static String buildDaysOfWeek(List<String> days) {

    if (days == null || days.isEmpty()) {

      throw new IllegalArgumentException("daysOfWeek is required for WEEKLY");
    }

    return days.stream()
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(Collectors.joining(","));
  }

  /**
   * Builds the comma-separated Quartz month list for an N-month interval anchored to the given
   * start date, e.g. startDate month = 3 (March), interval = 3 -> "3,6,9,12".
   */
  private static String monthsForInterval(LocalDate startDate, int intervalMonths) {

    int anchor = startDate.getMonthValue();

    List<Integer> months = new ArrayList<>();

    for (int m = anchor; m <= 12; m += intervalMonths) {
      months.add(m);
    }

    return months.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  private static LocalDate requireStartDate(CronBuilderRequest request) {

    if (request.startDate() == null) {
      throw new IllegalArgumentException("startDate is required for this schedule type");
    }

    return request.startDate();
  }

  private static void validateDateRange(LocalDate startDate, LocalDate endDate) {

    if (startDate != null && endDate != null && endDate.isBefore(startDate)) {

      throw new IllegalArgumentException("endDate cannot be before startDate");
    }
  }

  private static String requireValue(String value, String field) {

    if (value == null || value.isBlank()) {

      throw new IllegalArgumentException(field + " is required");
    }

    return value;
  }

  private static String normalize(String value) {

    return value.trim().replaceAll("\\s+", "");
  }

  private static ZoneId getZoneId(String timezone) {

    if (timezone == null || timezone.isBlank()) {
      return ZoneId.of("Asia/Phnom_Penh");
    }

    try {

      return ZoneId.of(timezone);

    } catch (Exception e) {

      throw new IllegalArgumentException("Invalid timezone: " + timezone, e);
    }
  }
}
