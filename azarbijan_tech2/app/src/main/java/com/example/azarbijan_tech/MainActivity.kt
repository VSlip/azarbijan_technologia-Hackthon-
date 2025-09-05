package com.example.azarbijan_tech

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var screenTimeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        // Use XML layout instead of Compose
        setContentView(R.layout.activity_main)

        screenTimeText = findViewById(R.id.screenTimeText)

        // Start foreground service (optional if you want background tracking)
        val serviceIntent = Intent(this, ScreenTimeService::class.java)
        startForegroundService(serviceIntent)

        // Collect screen time from service (your timer)
        lifecycleScope.launch {
            ScreenTimeService.screenTimeFlow.collect { time ->
                runOnUiThread {
                    screenTimeText.text = "Timer: ${formatTime(time)}"
                }
            }
        }

        // Also query real app screen time
        lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                val totalTime = getTodayScreenTime()
                runOnUiThread {
                    screenTimeText.append("\nUsage Stats: ${formatTime(totalTime / 1000)}")
                }
                kotlinx.coroutines.delay(10_000L) // refresh every 10s
            }
        }
        findViewById<Button>(R.id.openPieChartBtn).setOnClickListener {
            startActivity(Intent(this, UsageStatsActivity::class.java))
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun getTodayScreenTime(): Long {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val stats: List<UsageStats> =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        var totalTime = 0L
        for (usage in stats) {
            totalTime += usage.totalTimeInForeground
        }
        return totalTime
    }
}
