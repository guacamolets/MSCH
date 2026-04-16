package com.example.msch.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.msch.services.SettingsManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {
    private val settingsManager = SettingsManager(context)

    fun scheduleNotification(nextDateMillis: Long, daysBefore: Int) {
        val workManager = WorkManager.getInstance(context)

        val workTag = if (daysBefore == 0) "today_work" else "before_work"

        val calendar = Calendar.getInstance().apply {
            timeInMillis = nextDateMillis
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            set(Calendar.HOUR_OF_DAY, settingsManager.reminderHour)
            set(Calendar.MINUTE, settingsManager.reminderMinute)
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        if (delay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(workTag)
                .build()

            workManager.enqueueUniqueWork(
                workTag,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun cancelNotification(tag: String) {
        WorkManager.getInstance(context).cancelUniqueWork(tag)
    }
}