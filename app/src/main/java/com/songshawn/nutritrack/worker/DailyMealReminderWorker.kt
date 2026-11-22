package com.songshawn.nutritrack.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.songshawn.nutritrack.MainActivity
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.util.Constants
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that posts a daily notification reminding the user to log
 * their meals and review their live HEIFA nutrition score.
 */
class DailyMealReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Log today's meals in NutriTrack")
            .setContentText("Keep your 7-day HEIFA score streak alive — log breakfast, lunch, or dinner.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "nutritrack_daily_meal_reminder"
        private const val UNIQUE_WORK_NAME = "nutritrack_daily_meal_reminder_work"
        private const val PREF_KEY_REMINDER_ENABLED = "daily_meal_reminder_enabled"
        private const val NOTIFICATION_ID = 1001

        fun isReminderEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_KEY_REMINDER_ENABLED, false)
        }

        fun setReminderEnabled(context: Context, enabled: Boolean) {
            val appCtx = context.applicationContext
            appCtx.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_REMINDER_ENABLED, enabled)
                .apply()

            val workManager = WorkManager.getInstance(appCtx)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<DailyMealReminderWorker>(
                    24,
                    TimeUnit.HOURS,
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } else {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            }
        }

        private fun ensureNotificationChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Meal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily reminders to log meals and track your HEIFA score"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
