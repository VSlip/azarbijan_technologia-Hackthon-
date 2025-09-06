package com.example.azarbijan_tech

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenTimeService : Service() {

    private lateinit var usageStatsManager: UsageStatsManager
    private val CHANNEL_ID = "ScreenTimeServiceChannel"
    private val NOTIFICATION_ID = 1

    // ✅ Create a coroutine scope for this service
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    companion object {
        private val _screenTimeFlow = MutableStateFlow(0L)
        val screenTimeFlow: StateFlow<Long> = _screenTimeFlow.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start tracking screen time
        startScreenTimeTracking()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // ��� cancel coroutines when service is destroyed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your screen time usage"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Tracking")
            .setContentText("Monitoring your device usage")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startScreenTimeTracking() {
        serviceScope.launch {
            var screenTime = 0L
            while (isActive) { // ✅ stop automatically if service is destroyed
                screenTime += 1
                _screenTimeFlow.value = screenTime
                delay(1000) // Update every second
            }
        }
    }
}
