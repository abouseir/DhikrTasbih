package com.example.dhikrtasbih

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * WorkManager worker that fires the correct Adhkar reminder notification.
 * Called by WorkManager wired from SettingsScreen toggle.
 *
 * INPUT DATA KEY: "type" → "morning" | "evening"
 */
class AdhkarReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        when (inputData.getString("type")) {
            "morning" -> if (prefs.getMorningNotification()) NotificationHelper.sendMorningNotification(applicationContext)
            "evening" -> if (prefs.getEveningNotification()) NotificationHelper.sendEveningNotification(applicationContext)
        }
        return Result.success()
    }
}
