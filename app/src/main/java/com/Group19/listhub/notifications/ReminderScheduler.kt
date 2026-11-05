package com.Group19.listhub.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    private const val CHANNEL_ID = "task_reminders"

    fun schedule(context: Context, taskId: String, title: String, dueAt: Long, recurrence: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("title", title)
            putExtra("dueAt", dueAt)
            putExtra("recurrence", recurrence)
        }
        val pending = PendingIntent.getBroadcast(context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending)
            }
        } else alarm.setExact(AlarmManager.RTC_WAKEUP, dueAt, pending)
    }

    fun cancel(context: Context, taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }

    fun nextOccurrence(dueAt: Long, recurrence: String): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = dueAt }
        when (recurrence) {
            "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> cal.add(Calendar.MONTH, 1)
            else -> return null
        }
        return cal.timeInMillis
    }
}
