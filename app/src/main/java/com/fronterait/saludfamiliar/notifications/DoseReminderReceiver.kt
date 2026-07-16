package com.fronterait.saludfamiliar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fronterait.saludfamiliar.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_REMINDER = "com.fronterait.saludfamiliar.ACTION_SHOW_REMINDER"
        const val ACTION_MARK_TAKEN = "com.fronterait.saludfamiliar.ACTION_MARK_TAKEN"
        const val EXTRA_DOSE_ID = "extra_dose_id"
        const val EXTRA_PERSON_ID = "extra_person_id"
        const val EXTRA_PERSON_NAME = "extra_person_name"
        const val EXTRA_MEDICATION = "extra_medication"
        const val EXTRA_DOSE_DESC = "extra_dose_desc"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1L)
        if (doseId < 0) return

        when (intent.action) {
            ACTION_MARK_TAKEN -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppDatabase.getDatabase(context).appDao().markDoseTakenById(doseId)
                    } finally {
                        NotificationHelper.cancelNotification(context, doseId)
                        pendingResult.finish()
                    }
                }
            }
            else -> {
                val personId = intent.getLongExtra(EXTRA_PERSON_ID, -1L)
                val personName = intent.getStringExtra(EXTRA_PERSON_NAME) ?: ""
                val medication = intent.getStringExtra(EXTRA_MEDICATION) ?: ""
                val doseDesc = intent.getStringExtra(EXTRA_DOSE_DESC) ?: ""
                NotificationHelper.showDoseReminder(context, doseId, personId, personName, medication, doseDesc)
            }
        }
    }
}
