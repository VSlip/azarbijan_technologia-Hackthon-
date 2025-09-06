package com.example.azarbijan_tech
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.azarbijan_tech.DeviceUsageAdapter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StatsFragment : Fragment() {

    private lateinit var barChart: HorizontalBarChart
    private lateinit var pieChart: PieChart
    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var activityListRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        barChart = view.findViewById(R.id.barChart)
        pieChart = view.findViewById(R.id.pieChart)
        deviceRecyclerView = view.findViewById(R.id.rvDeviceUsage)
        deviceRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize activity list
        activityListRecyclerView = view.findViewById(R.id.rvActivityList)
        activityListRecyclerView.layoutManager = LinearLayoutManager(requireContext())


        fetchLogs()
        return view
    }

    private fun setupActivityList(logs: List<ScreenTimeData>) {
        // Aggregate total time per app
        val appDurations = logs.groupBy { it.app_name }
            .map { (appName, appLogs) ->
                val totalMillis = appLogs.sumOf { it.end_time - it.start_time }
                ActivityItem(
                    name = appName,
                    duration = formatDuration(totalMillis)
                )
            }
            .sortedByDescending { it.duration } // Optional: sort by duration

        // Set adapter
        activityListRecyclerView.adapter = ActivityAdapter(appDurations)
    }
    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun fetchLogs() {
        RetrofitClient.instance.getScreenTimeLogs()
            .enqueue(object : Callback<List<ScreenTimeData>> {
                override fun onResponse(
                    call: Call<List<ScreenTimeData>>,
                    response: Response<List<ScreenTimeData>>
                ) {
                    if (response.isSuccessful) {
                        val logs = response.body()
                        if (!logs.isNullOrEmpty()) {
                            // Print logs for debugging
                            logs.forEach { log ->
                                println("📊 App: ${log.app_name}, Device: ${log.device_type}, Start: ${log.start_time}, End: ${log.end_time}")
                            }

                            // Setup charts
                            setupStackedBarChart(logs)
                            setupPieChart(logs)
                            setupDeviceBreakdown(logs)
                            setupActivityList(logs)
                        }
                    }
                }

                override fun onFailure(call: Call<List<ScreenTimeData>>, t: Throwable) {
                    println("❌ Failed to fetch logs: ${t.message}")
                }
            })
    }

    private fun setupBarChart(logs: List<ScreenTimeData>) {
        // Group logs by app and sum durations
        val appTimeMap = logs.groupBy { it.app_name }
            .mapValues { entry ->
                entry.value.sumOf { it.end_time - it.start_time }.toFloat() / 1000f // duration in seconds
            }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        var index = 0f

        appTimeMap.forEach { (appName, duration) ->
            entries.add(BarEntry(index, floatArrayOf(duration)))
            labels.add(appName)
            index += 1f
        }

        val dataSet = BarDataSet(entries, "App Screen Time (s)").apply {
            colors = listOf(Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.RED)
            setDrawValues(true)
        }

        val data = BarData(dataSet).apply { barWidth = 0.5f }

        barChart.apply {
            this.data = data
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }
    private fun setupStackedBarChart(logs: List<ScreenTimeData>) {
        // Group logs by app and sum durations
        val appTimeMap = logs.groupBy { it.app_name }
            .mapValues { entry ->
                entry.value.sumOf { it.end_time - it.start_time }.toFloat() / 1000f // seconds
            }

        val labels = appTimeMap.keys.toList()
        val durations = appTimeMap.values.toFloatArray()

        // Stacked bar needs ONE BarEntry with an array of values
        val entries = listOf(
            BarEntry(0f, durations) // one row, stacked with all durations
        )

        val dataSet = BarDataSet(entries, "Apps").apply {
            setColors(
                Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.RED
            ) // add more colors if more apps
            setDrawValues(true)
            stackLabels = labels.toTypedArray() // label for each stack
        }

        val data = BarData(dataSet)

        barChart.apply {
            this.data = data
            setFitBars(true)
            description.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.apply {
                granularity = 1f
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(listOf("")) // single bar label
            }
            legend.isEnabled = true
            animateY(1000)
            invalidate()
            legend.textColor=Color.WHITE
        }

    }

    private fun setupPieChart(logs: List<ScreenTimeData>) {
        val appTimeMap = logs.groupBy { it.app_name }
            .mapValues { entry ->
                entry.value.sumOf { it.end_time - it.start_time }.toFloat() / 1000f // seconds
            }

        val entries = mutableListOf<PieEntry>()
        appTimeMap.forEach { (appName, duration) ->
            entries.add(PieEntry(duration, appName))
        }

        // Pass "" instead of "Total Screen Time"
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.RED)
            setDrawValues(true)
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        dataSet.setDrawValues(false)



        val data = PieData(dataSet)

        pieChart.apply {
            this.data = data
            description.isEnabled = false
            isRotationEnabled = true
            setEntryLabelColor(Color.WHITE)
            legend.isEnabled = true
            legend.textColor = Color.WHITE
            animateY(1000)
            invalidate()
        }
        pieChart.setDrawEntryLabels(false)

    }
    private fun setupDeviceBreakdown(logs: List<ScreenTimeData>) {
        // Group by device_type and calculate total hours
        val deviceTimeMap = logs.groupBy { it.device_type }
            .mapValues { entry ->
                entry.value.sumOf { it.end_time - it.start_time }.toFloat() / (1000f * 60f * 60f) // convert ms → hours
            }

        val deviceList = deviceTimeMap.map { (device, hours) ->
            DeviceUsage(
                name = device,
                usageTime = formatHours(hours),
                iconRes = when (device.lowercase()) {
                    "android" -> R.drawable.mobile
                    "tablet" -> R.drawable.tablet
                    "desktop" -> R.drawable.laptop
//                    "desktop" -> R.drawable.ic_desktop
                    else -> R.drawable.mobile
                }
            )
        }

        deviceRecyclerView.adapter = DeviceUsageAdapter(deviceList)
    }

    private fun formatHours(hours: Float): String {
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "${h}h ${m}min"
    }


}
