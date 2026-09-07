
# Cron Builder Request Body — Examples and Notes

Ready-to-use JSON request bodies for every `CronBuilderRequest` type. Each example matches the request fields exactly and is copy-ready.

## Table of contents

- Examples
- Notes & cron parsing

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

## Notes — /describe, CUSTOM and cron-utils

Fair question — let's be concrete about the actual problem it solves, not just "it's convenient." The core problem: describing an arbitrary cron string in English is genuinely hard.

Your `/describe` endpoint accepts any valid Quartz cron string a user pastes in — you have no control over its shape. And your `CUSTOM` builder type lets users freely combine all 6–7 raw cron fields themselves. In both cases, you can't know ahead of time which combination you're dealing with.

Try hand-writing a describer for these real Quartz expressions and see how fast it gets ugly:

### Examples

| Cron | What it means |
|---|---|
| `0 15 10 ? * MON-FRI` | Every weekday at 10:15 AM |
| `0 0/5 14,18 * * ?` | Every 5 minutes during hour 14 and hour 18, every day |
| `0 15 10 L-2 * ?` | 10:15 AM, 2 days before the last day of the month |
| `0 15 10 ? * 6L` | 10:15 AM, on the last Friday of the month |
| `0 15 10 ? * 6#3` | 10:15 AM, on the third Friday of the month |
| `0 0 12 1/5 * ?` | Noon, every 5 days starting on the 1st |

Each of these needs different English phrasing logic: ranges (`MON-FRI`), lists (`14,18`), the `L` "last" modifier combined with an offset (`L-2`), day-of-week + `L` combined (`6L`), the `#` nth-weekday-of-month operator (`6#3`), and step values (`1/5`). And they can all combine with each other. Writing a correct, general-purpose parser for the full Quartz cron grammar — by hand — is a multi-week project with a long tail of edge cases, and it's very easy to get subtly wrong (e.g., off-by-one on which day `6` means, or mishandling `L` when combined with a step).

### Why not write it ourselves?

Because this is a solved, narrow, well-tested problem that doesn't touch your actual business logic (banking rules, retries, dead-letters — the stuff that's novel and worth your team's engineering time). `cron-utils` provides:

- A grammar-based parser for the Quartz cron dialect (not just standard 5-field Unix cron — Quartz's 6/7-field format with `?`, `L`, `W`, `#` has real differences)
- Years of community bug fixes addressing edge cases
- A `CronDescriptor` to produce grammatically correct English (and other locales if you need i18n)

Writing your own would mean re-solving a problem someone else already solved correctly and maintaining it forever as new edge cases are reported.

### Where hand-written descriptions are better

For structured builder types (`DAILY`, `WEEKLY`, `MONTHLY`, etc.) you already know user intent from the form fields. `describeFriendly()` can hand-write concise, user-friendly sentences (e.g., "Runs every Monday, Wednesday and Friday at 9:30 AM") that are better UX than a generic parser.

The library is therefore scoped to the two places where you lack context: raw pasted cron strings (`/describe`) and fully custom field combinations (`CUSTOM`).

### Bottom line

- Keep `cron-utils` for `/describe` and `CUSTOM` parsing because it solves a correctness-critical, combinatorial problem.
- If you never plan to support `/describe` or `CUSTOM`, you can drop the dependency and rely on hand-written descriptions for structured builders. But since both are part of your API surface, keeping `cron-utils` is the pragmatic choice.