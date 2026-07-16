package com.fronterait.saludfamiliar.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fronterait.saludfamiliar.MainActivity
import com.fronterait.saludfamiliar.R

object NotificationHelper {
    const val CHANNEL_ID = "dose_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de medicación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos para tomar la medicación a horario"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showDoseReminder(
        context: Context,
        doseId: Long,
        personId: Long,
        personName: String,
        medication: String,
        doseDesc: String
    ) {
        createChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            doseId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markTakenIntent = Intent(context, DoseReminderReceiver::class.java).apply {
            action = DoseReminderReceiver.ACTION_MARK_TAKEN
            putExtra(DoseReminderReceiver.EXTRA_DOSE_ID, doseId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            doseId.toInt(),
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💊 Hora de la medicación")
            .setContentText("$personName: $medication ($doseDesc)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Marcar como tomada", markTakenPendingIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(doseId.toInt(), notification)
    }

    fun cancelNotification(context: Context, doseId: Long) {
        NotificationManagerCompat.from(context).cancel(doseId.toInt())
    }
}
