package com.example.azarbijan_tech

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.widget.Button
import kotlinx.coroutines.delay
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.Duration


class UsageStatsActivity : AppCompatActivity() {
    private  val PREFS_NAME = "screen_time_prefs"
    private  val KEY_USER_ID = "user_id"
    private lateinit var pieChart: PieChart
    private lateinit var prefs : SharedPreferences
    private lateinit var barChart: HorizontalBarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

//        pieChart = findViewById(R.id.pieChart)
//        barChart = findViewById(R.id.barChart) // if you have a chart for timeline
//
        val sessions = getLatestSessions()
        val appUsage = getUsageSummary(sessions)
//
//        setupPieChart(appUsage)
//        setupBarChart(sessions)

        // ✅ Upload sessions
        val api = MainActivity.RetrofitClient.instance
        prefs = getSharedPreferences("screen_time_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putString(KEY_USER_ID, "").apply()
        var userId = prefs.getString(KEY_USER_ID, "") ?: ""
//        var userId = ""
        println(userId)
        checkregister(userId,api)
//        uploadSessions(sessions, userId, api)
        fetchHealthData()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, StatsFragment())
            .addToBackStack(null)
            .commit()

    }


    private fun checkregister(userId : String,api : ApiService){
        if (userId.isEmpty()) {
            val deviceInfo = mapOf(
                "device_type" to "android",
                "name" to "android"
            )
            api.registerDevice(deviceInfo).enqueue(object : Callback<DeviceResponse> {
                override fun onResponse(
                    call: Call<DeviceResponse>,
                    response: Response<DeviceResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val userId1 = response.body()!!.user_id
                        prefs.edit().putString(KEY_USER_ID, userId1).apply()
                        Log.d("ScreenTimeService", "✅ Device registered, user_id=$userId")

                    } else {
                        Log.e("ScreenTimeService", "❌ Device registration failed: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<DeviceResponse>, t: Throwable) {
                    Log.e("ScreenTimeService", "❌ Device registration error: ${t.message}")
                }
            })
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
    private fun uploadSessions(sessions: List<UsageSession>, userId: String, api: ApiService) {
        for (session in sessions) {
            // Skip "Idle" sessions if you don't want to track them
            if (session.appName.lowercase() == "idle") continue
            val simpleAppName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(session.packageName, 0)
                ).toString()
            } catch (e: Exception) {
                session.packageName.substringAfterLast('.') // fallback: take last part
            }
            val data = ScreenTimeData(
                user_id = userId,
                app_name = simpleAppName,
                start_time = session.startTime,
                end_time = session.endTime,
                device_type = "android"

            )
            val body = mapOf(
                "user_id" to data.user_id,
                "app_name" to data.app_name,
                "start_time" to data.start_time,
                "end_time" to data.end_time,
                "device_type" to data.device_type
            )

            api.sendScreenTime(data).enqueue(object : retrofit2.Callback<LogResponse> {
                override fun onResponse(
                    call: retrofit2.Call<LogResponse>,
                    response: retrofit2.Response<LogResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.d("Upload", "✅ Uploaded ${data.app_name}: ${response.body()}")
                    } else {
                        Log.e("Upload", "❌ Server error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: retrofit2.Call<LogResponse>, t: Throwable) {
                    Log.e("Upload", "❌ Failed to upload ${data.app_name}: ${t.message}")
                }
            })




        }
    }
    private fun getLatestSessions(): List<UsageSession> {
        val prefs = getSharedPreferences("screen_time_prefs", Context.MODE_PRIVATE)
        val defaultStartTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val lastUploadTime = prefs.getLong("last_upload_time", defaultStartTime) // default 0 = start of epoch

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = lastUploadTime.coerceAtLeast(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        )

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val sessions = mutableListOf<UsageSession>()
        val event = UsageEvents.Event()
        val openEvents = mutableMapOf<String, Long>() // packageName -> startTime

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> openEvents[event.packageName] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = openEvents[event.packageName]
                    if (start != null) {
                        val appName = try {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(event.packageName, 0)
                            ).toString()
                        } catch (e: Exception) {
                            event.packageName.substringAfterLast('.')
                                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
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

        // Sort by start time
        return sessions.sortedBy { it.startTime }
    }
    private fun fetchHealthData() {
        println("1 fetch")
        try {
            if (!HealthConnectClient.isProviderAvailable(this)) {
                Log.d("HealthConnect", "Health Connect is not available on this device")
                println("ℹ️ Health Connect is not available on this device")
                return
            }

            val healthConnectClient = HealthConnectClient.getOrCreate(this)

            lifecycleScope.launch {
                try {
                    val now = Instant.now()
                    val startTime = now.minus(7, ChronoUnit.DAYS)

                    // Steps
                    val steps = healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType = StepsRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, now)
                        )
                    ).records

                    steps.forEach {
                        Log.d("Steps", "Count: ${it.count}, Time: ${it.startTime} - ${it.endTime}")
                        println("Steps ➝ Count: ${it.count}, Time: ${it.startTime} - ${it.endTime}")
                    }

                    // Sleep
                    val sleep = healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, now)
                        )
                    ).records

                    sleep.forEach {
                        Log.d("Sleep", "Duration: ${Duration.between(it.startTime, it.endTime)}")
                        println("Sleep ➝ Duration: ${Duration.between(it.startTime, it.endTime)}")
                    }

                    // Workouts
                    val workouts = healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType = ExerciseSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, now)
                        )
                    ).records

                    workouts.forEach {
                        Log.d("Workout", "Type: ${it.exerciseType}, Duration: ${Duration.between(it.startTime, it.endTime)}")
                        println("Workout ➝ Type: ${it.exerciseType}, Duration: ${Duration.between(it.startTime, it.endTime)}")
                    }

                } catch (e: Exception) {
                    Log.e("HealthConnect", "Error fetching data: ${e.message}")
                    println("❌ Error fetching Health Data: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error initializing Health Connect: ${e.message}")
            println("❌ Error initializing Health Connect: ${e.message}")
        }
    }



}
