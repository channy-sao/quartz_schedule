package com.example.schedule.constant;

public enum CronScheduleType {

    // Fine-grained / advanced types (unchanged)
    EVERY_SECOND,
    EVERY_MINUTE,
    EVERY_HOUR,
    SPECIFIC_TIME,
    CUSTOM,

    // Types matching the schedule-type picker in the UI
    SPECIFIC_DATE,   // "1, One Time"        -> Select Date only
    DAILY,           // "2, Daily"           -> Start date, End date
    WEEKLY,          // "3, Weekly"          -> Select (Weekdays), Start date, End date
    MONTHLY,         // "4, Monthly"         -> Select (Day), Start date, End date
    EVERY_3_MONTHS,  // "5, Every 3 Months"  -> Select (Day), Start date, End date
    EVERY_6_MONTHS,  // "6, Every 6 Months"  -> Select (Day), Start date, End date
    YEARLY           // "7, Annually"        -> Select (Day), Start date, End date
}