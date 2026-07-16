package com.fronterait.saludfamiliar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun scheduleDoseReminder(
        context: Context,
        doseId: Long,
        scheduledTime: Long,
        personId: Long,
        personName: String,
        medication: String,
        doseDesc: String
    ) {
        if (scheduledTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, doseId, personId, personName, medication, doseDesc)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
        }
    }

    fun cancelDoseReminder(context: Context, doseId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DoseReminderReceiver::class.java).apply {
            action = DoseReminderReceiver.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            doseId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        NotificationHelper.cancelNotification(context, doseId)
    }

    private fun buildPendingIntent(
        context: Context,
        doseId: Long,
        personId: Long,
        personName: String,
        medication: String,
        doseDesc: String
    ): PendingIntent {
        val intent = Intent(context, DoseReminderReceiver::class.java).apply {
            action = DoseReminderReceiver.ACTION_SHOW_REMINDER
            putExtra(DoseReminderReceiver.EXTRA_DOSE_ID, doseId)
            putExtra(DoseReminderReceiver.EXTRA_PERSON_ID, personId)
            putExtra(DoseReminderReceiver.EXTRA_PERSON_NAME, personName)
            putExtra(DoseReminderReceiver.EXTRA_MEDICATION, medication)
            putExtra(DoseReminderReceiver.EXTRA_DOSE_DESC, doseDesc)
        }
        return PendingIntent.getBroadcast(
            context,
            doseId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
