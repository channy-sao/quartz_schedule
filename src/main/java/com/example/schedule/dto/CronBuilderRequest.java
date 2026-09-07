package com.example.schedule.dto;

import com.example.schedule.constant.CronScheduleType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CronBuilderRequest(

        @NotNull CronScheduleType type,
        String timezone,
        String time,
        List<String> daysOfWeek,
        String dayOfMonth,
        LocalDate date,
        String specificTime,
        String seconds,
        String minutes,
        String hours,
        String dayOfMonthExpression,
        String month,
        String dayOfWeek,
        String year

) {

        // ---- Named factory methods: no more positional-null guessing ----

        public static CronBuilderRequest everySecond() {
                return new CronBuilderRequest(CronScheduleType.EVERY_SECOND,
                        null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest everyMinute() {
                return new CronBuilderRequest(CronScheduleType.EVERY_MINUTE,
                        null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest everyHour() {
                return new CronBuilderRequest(CronScheduleType.EVERY_HOUR,
                        null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest daily(String time, String timezone) {
                return new CronBuilderRequest(CronScheduleType.DAILY,
                        timezone, time, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest weekly(List<String> daysOfWeek, String time, String timezone) {
                return new CronBuilderRequest(CronScheduleType.WEEKLY,
                        timezone, time, daysOfWeek, null, null, null, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest monthly(String dayOfMonth, String time, String timezone) {
                return new CronBuilderRequest(CronScheduleType.MONTHLY,
                        timezone, time, null, dayOfMonth, null, null, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest yearly(String dayOfMonth, String month, String time, String timezone) {
                return new CronBuilderRequest(CronScheduleType.YEARLY,
                        timezone, time, null, dayOfMonth, null, null, null, null, null, null, month, null, null);
        }

        public static CronBuilderRequest specificDate(LocalDate date, String specificTime, String timezone) {
                return new CronBuilderRequest(CronScheduleType.SPECIFIC_DATE,
                        timezone, null, null, null, date, specificTime, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest specificTime(String time, String timezone) {
                return new CronBuilderRequest(CronScheduleType.SPECIFIC_TIME,
                        timezone, null, null, null, null, time, null, null, null, null, null, null, null);
        }

        public static CronBuilderRequest custom(String seconds, String minutes, String hours,
                                                String dayOfMonthExpression, String month, String dayOfWeek,
                                                String year, String timezone) {
                return new CronBuilderRequest(CronScheduleType.CUSTOM,
                        timezone, null, null, null, null, null,
                        seconds, minutes, hours, dayOfMonthExpression, month, dayOfWeek, year);
        }
}