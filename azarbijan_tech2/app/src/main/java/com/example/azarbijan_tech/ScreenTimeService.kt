package com.example.azarbijan_tech

import android.app.*
import android.content.Intent
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenTimeService : Service() {

    private val binder = LocalBinder()
    private var job: Job? = null
    private var seconds = 0L

    companion object {
        private val _screenTimeFlow = MutableStateFlow(0L)
        val screenTimeFlow = _screenTimeFlow.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000L)
                seconds++
                _screenTimeFlow.value = seconds
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "screen_time_channel"
        val channel = NotificationChannel(channelId, "Screen Time Tracker", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Screen Time Tracker")
            .setContentText("Tracking screen usage…")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenTimeService = this@ScreenTimeService
    }
}
