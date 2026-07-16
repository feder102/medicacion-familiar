package com.fronterait.saludfamiliar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fronterait.saludfamiliar.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).appDao()
                val reminders = dao.getPendingDoseReminders(System.currentTimeMillis())
                reminders.forEach { info ->
                    AlarmScheduler.scheduleDoseReminder(
                        context = context,
                        doseId = info.id,
                        scheduledTime = info.scheduledTime,
                        personId = info.personId,
                        personName = info.personName,
                        medication = info.medication,
                        doseDesc = info.doseDesc
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
