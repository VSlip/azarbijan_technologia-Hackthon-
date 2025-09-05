package com.example.azarbijan_tech

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.*

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart

    private lateinit var barChart: HorizontalBarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        pieChart = findViewById(R.id.pieChart)

        val sessions = getTodayUsageSessions()
        val appUsage = getUsageSummary(sessions)

        setupPieChart(appUsage)

        // 🔹 Example: print all sessions with gaps
        for (session in sessions) {
            val start = Date(session.startTime)
            val end = Date(session.endTime)
            println("${session.appName} → $start - $end")
        }
    }


    private fun getTodayUsageStats(): Map<String, Long> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val stats: List<UsageStats> =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val usageMap = mutableMapOf<String, Long>()
        for (usage in stats) {
            if (usage.totalTimeInForeground > 0) {
                val appName = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(usage.packageName, 0)
                    ).toString()
                } catch (e: Exception) {
                    usage.packageName
                }
                usageMap[appName] = (usageMap[appName] ?: 0L) + usage.totalTimeInForeground
            }
        }
        return usageMap.toList()
            .sortedByDescending { it.second }
            .take(7) // show top 7 apps
            .toMap()
    }

    private fun setupPieChart(usageMap: Map<String, Long>) {
        val entries = usageMap.map { (app, time) ->
            PieEntry(time.toFloat(), app)
        }

        val dataSet = PieDataSet(entries, "App Usage Today")
        dataSet.colors = listOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
            Color.CYAN, Color.YELLOW, Color.LTGRAY
        )
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "App Usage"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    private fun getTodayUsageSessions(): List<UsageSession> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)

        val sessions = mutableListOf<UsageSession>()
        val event = UsageEvents.Event()
        val openEvents = mutableMapOf<String, Long>() // packageName -> startTime

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    openEvents[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = openEvents[event.packageName]
                    if (start != null) {
                        val appName = try {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(event.packageName, 0)
                            ).toString()
                        } catch (e: Exception) {
                            event.packageName.substringAfterLast('.')
                                .replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                }
                        }

                        sessions.add(
                            UsageSession(
                                appName = appName,
                                packageName = event.packageName,
                                startTime = start,
                                endTime = event.timeStamp
                            )
                        )

                        openEvents.remove(event.packageName)
                    }
                }
            }
        }

        // 🔹 Sort sessions by time
        val sortedSessions = sessions.sortedBy { it.startTime }.toMutableList()

        // 🔹 Insert "Idle" gaps
        val withGaps = mutableListOf<UsageSession>()
        for (i in 0 until sortedSessions.size) {
            val current = sortedSessions[i]
            withGaps.add(current)

            if (i < sortedSessions.size - 1) {
                val next = sortedSessions[i + 1]
                if (current.endTime < next.startTime) {
                    withGaps.add(
                        UsageSession(
                            appName = "Idle",
                            packageName = "idle",
                            startTime = current.endTime,
                            endTime = next.startTime
                        )
                    )
                }
            }
        }

        return withGaps
    }

    private fun getUsageSummary(sessions: List<UsageSession>): Map<String, Long> {
        val usageMap = mutableMapOf<String, Long>()
        for (session in sessions) {
            val duration = session.endTime - session.startTime
            usageMap[session.appName] = (usageMap[session.appName] ?: 0L) + duration
        }
        return usageMap.toList()
            .sortedByDescending { it.second }
            .take(7) // show top 7
            .toMap()
    }
    private fun formatToLocalTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault() // device timezone
        return sdf.format(Date(timestamp))
    }

    private fun setupBarChart(sessions: List<UsageSession>) {
        val entries = mutableListOf<BarEntry>()

        // We treat each session as a stacked part of a bar
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        for ((index, session) in sessions.withIndex()) {
            val start = (session.startTime - startOfDay).toFloat() / 1000 / 60 // minutes since start of day
            val end = (session.endTime - startOfDay).toFloat() / 1000 / 60
            val duration = end - start

            entries.add(
                BarEntry(
                    0f, // single row timeline
                    floatArrayOf(duration), // stacked part
                    session.appName // label
                )
            )
        }

        val dataSet = BarDataSet(entries, "Usage Timeline")

        // Colors: different for each app
        val colors = sessions.map {
            when (it.appName.lowercase(Locale.getDefault())) {
                "idle" -> Color.LTGRAY
                "youtube" -> Color.RED
                "instagram" -> Color.MAGENTA
                "chrome" -> Color.BLUE
                else -> Color.GREEN
            }
        }
        dataSet.colors = colors
        dataSet.setDrawValues(false)

        val data = BarData(dataSet)

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setFitBars(true)
        barChart.axisLeft.isEnabled = false
        barChart.axisRight.isEnabled = false
        barChart.xAxis.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.invalidate()
    }
}

