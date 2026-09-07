package com.example.schedule.utils;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.example.schedule.dto.CronBuilderRequest;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Locale;

public final class CronDescriptionUtil {

  private static final CronParser QUARTZ_PARSER =
      new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

  private CronDescriptionUtil() {}

  /**
   * Describe any raw Quartz cron string in plain English. Used for the reverse "describe this cron"
   * endpoint and as a fallback / sanity-check for our own generated expressions.
   */
  public static String describe(String quartzCronExpression) {
    try {
      Cron cron = QUARTZ_PARSER.parse(quartzCronExpression);
      return CronDescriptor.instance(Locale.ENGLISH).describe(cron);
    } catch (Exception e) {
      return "Unable to generate a description for this expression.";
    }
  }

  /**
   * A friendlier, template-aware description built from what the user actually selected — more
   * natural than the generic cron-utils output for our own builder types (e.g. "Every Monday,
   * Wednesday and Friday at 9:30 AM" instead of "at 30 minutes past hour 9 on Monday...").
   */
  public static String describeFriendly(CronBuilderRequest request) {
    final String timeHhmm =
        request.specificTime() != null ? request.specificTime() : request.time();
    return switch (request.type()) {
      case EVERY_SECOND -> "Runs every second.";
      case EVERY_MINUTE -> "Runs every minute, on the minute.";
      case EVERY_HOUR -> "Runs every hour, on the hour.";
      case DAILY -> "Runs every day at " + friendlyTime(request.time()) + ".";
      case WEEKLY ->
          "Runs every "
              + joinDays(request.daysOfWeek())
              + " at "
              + friendlyTime(request.time())
              + ".";
      case MONTHLY ->
          "Runs on day "
              + request.dayOfMonth()
              + " of every month at "
              + friendlyTime(request.time())
              + ".";
      case YEARLY ->
          "Runs every year on "
              + friendlyMonth(request.month())
              + " "
              + request.dayOfMonth()
              + " at "
              + friendlyTime(request.time())
              + ".";
      case SPECIFIC_DATE ->
          "Runs once on " + request.date() + " at " + friendlyTime(timeHhmm) + ".";
      case SPECIFIC_TIME -> "Runs every day at " + friendlyTime(timeHhmm) + ".";
      case CUSTOM ->
          null; // fall back to cron-utils describe() for CUSTOM — too many combinations to
                // hand-write
    };
  }

  /**
   * "in 2 hours 15 minutes" style relative label. Falls back gracefully for far-future or past
   * times.
   */
  public static String relativeLabel(ZonedDateTime target) {
    if (target == null) return null;

    Duration diff = Duration.between(ZonedDateTime.now(target.getZone()), target);
    if (diff.isNegative()) return "just passed";

    long days = diff.toDays();
    long hours = diff.toHoursPart();
    long minutes = diff.toMinutesPart();
    long seconds = diff.toSecondsPart();

    if (days > 0) return "in " + days + "d " + hours + "h";
    if (hours > 0) return "in " + hours + "h " + minutes + "m";
    if (minutes > 0) return "in " + minutes + "m " + seconds + "s";
    return "in " + seconds + "s";
  }

  private static String friendlyTime(String hhmm) {
    if (hhmm == null) return "";
    try {
      java.time.LocalTime t = java.time.LocalTime.parse(hhmm);
      return t.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
    } catch (Exception e) {
      return hhmm;
    }
  }

  private static String joinDays(java.util.List<String> days) {
    if (days == null || days.isEmpty()) return "";
    if (days.size() == 1) return capitalize(days.get(0));
    String allButLast =
        days.subList(0, days.size() - 1).stream()
            .map(CronDescriptionUtil::capitalize)
            .collect(java.util.stream.Collectors.joining(", "));
    return allButLast + " and " + capitalize(days.get(days.size() - 1));
  }

  private static String capitalize(String day) {
    String lower = day.trim().toLowerCase(Locale.ROOT);
    return switch (lower) {
      case "mon" -> "Monday";
      case "tue" -> "Tuesday";
      case "wed" -> "Wednesday";
      case "thu" -> "Thursday";
      case "fri" -> "Friday";
      case "sat" -> "Saturday";
      case "sun" -> "Sunday";
      default -> day;
    };
  }

  private static String friendlyMonth(String month) {
    if (month == null) return "";
    return switch (month.trim().toUpperCase(Locale.ROOT)) {
      case "1", "JAN" -> "January";
      case "2", "FEB" -> "February";
      case "3", "MAR" -> "March";
      case "4", "APR" -> "April";
      case "5", "MAY" -> "May";
      case "6", "JUN" -> "June";
      case "7", "JUL" -> "July";
      case "8", "AUG" -> "August";
      case "9", "SEP" -> "September";
      case "10", "OCT" -> "October";
      case "11", "NOV" -> "November";
      case "12", "DEC" -> "December";
      default -> month;
    };
  }
}
