package com.waytolearn.alertadolar

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailyNotificationScheduler {

    const val KEY_SCHEDULED_HOUR = "scheduled_hour"

    private const val WORK_NAME = "DailyCurrencyStatusWorker"
    private val targetHours = intArrayOf(10, 20)

    fun scheduleNext(context: Context) {
        val now = Calendar.getInstance()
        val next = nextRunCalendar(now)
        val delayMs = (next.timeInMillis - now.timeInMillis).coerceAtLeast(1_000L)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val scheduledHour = next.get(Calendar.HOUR_OF_DAY)

        val request = OneTimeWorkRequestBuilder<DolarWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_SCHEDULED_HOUR to scheduledHour))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun nextRunCalendar(now: Calendar): Calendar {
        var best: Calendar? = null
        for (hour in targetHours) {
            val candidate = now.clone() as Calendar
            candidate.set(Calendar.HOUR_OF_DAY, hour)
            candidate.set(Calendar.MINUTE, 0)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (best == null || candidate.timeInMillis < best.timeInMillis) {
                best = candidate
            }
        }
        return best ?: now
    }
}
