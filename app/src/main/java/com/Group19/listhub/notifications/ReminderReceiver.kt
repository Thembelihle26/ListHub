package com.Group19.listhub.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val title = intent.getStringExtra("title") ?: "Task reminder"
        val dueAt = intent.getLongExtra("dueAt", 0L)
        val recurrence = intent.getStringExtra("recurrence") ?: "Never"
        createChannel(context)
        val notification = NotificationCompat.Builder(context, "task_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("List Hub reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification) } catch (_: SecurityException) { }
        ReminderScheduler.nextOccurrence(dueAt, recurrence)?.let { next ->
            ReminderScheduler.schedule(context, taskId, title, next, recurrence)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel("task_reminders", "Task reminders", NotificationManager.IMPORTANCE_HIGH))
        }
    }
}
