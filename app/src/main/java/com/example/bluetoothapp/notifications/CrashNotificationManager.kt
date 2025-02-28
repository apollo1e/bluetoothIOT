package com.example.bluetoothapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bluetoothapp.MainActivity
import com.example.bluetoothapp.R
import com.example.bluetoothapp.data.CrashAlert
import com.example.bluetoothapp.data.CrashSeverity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CRASH_CHANNEL_ID = "crash_alerts_channel"
        private const val CRASH_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CRASH_CHANNEL_ID,
                "Crash Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for crashes detected by M5Stick devices"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCrashNotification(crashAlert: CrashAlert) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_CRASH_TAB", true)
            putExtra("CRASH_ALERT_ID", crashAlert.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val severityText = when (crashAlert.severity) {
            CrashSeverity.LOW -> "Minor"
            CrashSeverity.MEDIUM -> "Moderate"
            CrashSeverity.HIGH -> "Severe"
            CrashSeverity.CRITICAL -> "Critical"
        }
        
        // Use crash type from M5Stick if available
        val crashTypeText = if (crashAlert.crashType.isNotEmpty()) {
            crashAlert.crashType
        } else {
            "$severityText crash"
        }
        
        // Build notification content
        val contentTitle = "Crash Alert: ${crashAlert.deviceName}"
        
        // Build detailed content text
        val contentText = buildString {
            append("$crashTypeText detected")
            
            if (crashAlert.location.isNotEmpty() && crashAlert.location != "Unknown Location") {
                append(" at ${crashAlert.location}")
            }
            
            if (crashAlert.timestamp.isNotEmpty()) {
                append(" at ${crashAlert.timestamp}")
            }
            
            append(".")
        }
        
        // Build big text for expanded notification
        val bigText = buildString {
            append("Device: ${crashAlert.deviceName}\n")
            append("Type: $crashTypeText\n")
            
            if (crashAlert.accelerationValue > 0) {
                append("Impact: ${crashAlert.accelerationValue}g\n")
            }
            
            if (crashAlert.location.isNotEmpty() && crashAlert.location != "Unknown Location") {
                append("Location: ${crashAlert.location}\n")
            }
            
            if (crashAlert.timestamp.isNotEmpty()) {
                append("Time: ${crashAlert.timestamp}\n")
            }
            
            append("Tap for details.")
        }

        val builder = NotificationCompat.Builder(context, CRASH_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(CRASH_NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Handle missing notification permission
                e.printStackTrace()
            }
        }
    }
}