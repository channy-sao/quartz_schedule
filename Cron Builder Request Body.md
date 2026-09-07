
Ready-to-use JSON request bodies for every `CronBuilderRequest` type. Each example matches the request fields exactly and is copy-ready.

1. Every second

Description: Runs every second.

```json
{
  "type": "EVERY_SECOND"
}
```

2. Every minute

Description: Runs once each minute.

```json
{
  "type": "EVERY_MINUTE"
}
```

3. Every hour

Description: Runs at the top of every hour.

```json
{
  "type": "EVERY_HOUR"
}
```

4. Daily — every day at 9:30 AM

Description: Runs daily at the specified local time.

```json
{
  "type": "DAILY",
  "time": "09:30",
  "timezone": "Asia/Phnom_Penh"
}
```

5. Weekly — Mon/Wed/Fri at 9:30 AM

Description: Runs weekly on specific weekdays.

```json
{
  "type": "WEEKLY",
  "time": "09:30",
  "daysOfWeek": ["MON", "WED", "FRI"],
  "timezone": "Asia/Phnom_Penh"
}
```

6. Monthly — 15th of every month at 9:30 AM

Description: Runs monthly on a fixed day.

```json
{
  "type": "MONTHLY",
  "time": "09:30",
  "dayOfMonth": "15",
  "timezone": "Asia/Phnom_Penh"
}
```

Monthly variant — last day of month

Description: Use `"L"` to indicate the last day of the month.

```json
{
  "type": "MONTHLY",
  "time": "23:59",
  "dayOfMonth": "L",
  "timezone": "Asia/Phnom_Penh"
}
```

7. Yearly — Jan 1st at 9:00 AM

Description: Runs once per year on the given month/day.

```json
{
  "type": "YEARLY",
  "time": "09:00",
  "dayOfMonth": "1",
  "month": "JAN",
  "timezone": "Asia/Phnom_Penh"
}
```

8. Specific date — one-time run

Description: Runs once on a specific date and time.

```json
{
  "type": "SPECIFIC_DATE",
  "date": "2026-09-15",
  "specificTime": "09:30",
  "timezone": "Asia/Phnom_Penh"
}
```

9. Specific time — every day at a fixed time (alias of daily)

Description: Alias of `DAILY` using the `specificTime` field.

```json
{
  "type": "SPECIFIC_TIME",
  "specificTime": "09:30",
  "timezone": "Asia/Phnom_Penh"
}
```

10. Custom — every 15 minutes, 9am-5pm, weekdays

Description: Full cron-style fields for advanced schedules.

```json
{
  "type": "CUSTOM",
  "seconds": "0",
  "minutes": "0/15",
  "hours": "9-17",
  "dayOfMonthExpression": "?",
  "month": "*",
  "dayOfWeek": "MON-FRI",
  "timezone": "Asia/Phnom_Penh"
}
```

11. Custom — last day of month at 11 PM

```json
{
  "type": "CUSTOM",
  "seconds": "0",
  "minutes": "0",
  "hours": "23",
  "dayOfMonthExpression": "L",
  "month": "*",
  "dayOfWeek": "?",
  "timezone": "Asia/Phnom_Penh"
}
```

12. Custom — 2nd Monday of every month

```json
{
  "type": "CUSTOM",
  "seconds": "0",
  "minutes": "0",
  "hours": "9",
  "dayOfMonthExpression": "?",
  "month": "*",
  "dayOfWeek": "MON#2",
  "timezone": "Asia/Phnom_Penh"
}
```

Notes:

- Use an IANA timezone string for `timezone` (e.g., `Asia/Phnom_Penh`).
- For `CUSTOM` schedules, the fields follow Quartz-style cron semantics (including seconds).
- Replace example times and dates with values appropriate for your environment before sending.

Want me to also add the equivalent Quartz cron expressions or a JSON Schema for validation?