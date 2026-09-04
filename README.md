6. Test: Daily
   Request
   POST /api/schedules/cron
   Content-Type: application/json
   {
   "type": "DAILY",
   "timezone": "Asia/Phnom_Penh",
   "time": "09:00"
   }
   Result
   {
   "valid": true,
   "type": "DAILY",
   "cronExpression": "0 0 9 * * ?",
   "timezone": "Asia/Phnom_Penh",
   "nextExecution": "2026-09-04T09:00:00+07:00",
   "nextExecutions": [
   "2026-09-04T09:00:00+07:00",
   "2026-09-05T09:00:00+07:00",
   "2026-09-06T09:00:00+07:00",
   "2026-09-07T09:00:00+07:00",
   "2026-09-08T09:00:00+07:00"
   ]
   }
7. Test: Weekly

Monday, Wednesday, Friday at 09:30:

{
"type": "WEEKLY",
"timezone": "Asia/Phnom_Penh",
"time": "09:30",
"daysOfWeek": [
"MON",
"WED",
"FRI"
]
}

Generates:

0 30 9 ? * MON,WED,FRI
8. Test: Monthly

15th of every month at 09:00:

{
"type": "MONTHLY",
"timezone": "Asia/Phnom_Penh",
"time": "09:00",
"dayOfMonth": "15"
}

Generates:

0 0 9 15 * ?
Last day
{
"type": "MONTHLY",
"timezone": "Asia/Phnom_Penh",
"time": "23:00",
"dayOfMonth": "L"
}

Generates:

0 0 23 L * ?
Last weekday
{
"type": "MONTHLY",
"timezone": "Asia/Phnom_Penh",
"time": "09:00",
"dayOfMonth": "LW"
}

Generates:

0 0 9 LW * ?
9. Test: Yearly

January 1 at 09:00:

{
"type": "YEARLY",
"timezone": "Asia/Phnom_Penh",
"time": "09:00",
"dayOfMonth": "1",
"month": "JAN"
}

Generates:

0 0 9 1 JAN ?
10. Test: Specific date

September 15, 2026 at 09:30:

{
"type": "SPECIFIC_DATE",
"timezone": "Asia/Phnom_Penh",
"date": "2026-09-15",
"specificTime": "09:30"
}

Generates:

0 30 9 15 9 ? 2026

This is a one-time Cron expression because the year is fixed.

11. Test: Every 15 minutes during business hours

This is where CUSTOM becomes useful:

{
"type": "CUSTOM",
"timezone": "Asia/Phnom_Penh",
"seconds": "0",
"minutes": "0/15",
"hours": "9-17",
"dayOfMonthExpression": "?",
"month": "*",
"dayOfWeek": "MON-FRI",
"year": "*"
}

Result:

0 0/15 9-17 ? * MON-FRI *
12. Test: Second Monday
    {
    "type": "CUSTOM",
    "timezone": "Asia/Phnom_Penh",
    "seconds": "0",
    "minutes": "0",
    "hours": "9",
    "dayOfMonthExpression": "?",
    "month": "*",
    "dayOfWeek": "MON#2",
    "year": "*"
    }

Result:

0 0 9 ? * MON#2 *
13. Test: Last Friday
    {
    "type": "CUSTOM",
    "timezone": "Asia/Phnom_Penh",
    "seconds": "0",
    "minutes": "0",
    "hours": "18",
    "dayOfMonthExpression": "?",
    "month": "*",
    "dayOfWeek": "FRIL",
    "year": "*"
    }

Result:

0 0 18 ? * FRIL *
One correction I'd make to the design

I would not call this CronBuilderRequest in your final business API.

Keep it as a technical/testing endpoint:

POST /api/schedules/cron

Later your actual endpoint can be:

POST /api/schedules

with:

{
"scheduleType": "MONTHLY",
"category": "BILL_PAYMENT",
"billerCode": "EWP",
"schedule": {
"dayOfMonth": "15",
"time": "09:00",
"timezone": "Asia/Phnom_Penh"
}
}

Then:

Business Request
↓
Strategy
↓
Schedule Configuration
↓
QuartzCronUtil
↓
0 0 9 15 * ?
↓
Quartz