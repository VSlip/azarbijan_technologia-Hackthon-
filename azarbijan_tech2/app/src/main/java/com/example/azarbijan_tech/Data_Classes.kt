package com.example.azarbijan_tech

data class UsageSession(
    val appName: String,
    val packageName: String,
    val startTime: Long,
    val endTime: Long
)


// Response when registering a device
data class DeviceResponse(
    val user_id: String,  // ✅ change from Int to String
    val device_type: String,
    val name: String,
    val created_at: String
)
data class ActivityItem(
    val name: String,
    val duration: String,
)

// Response when logging screen time
data class LogResponse(
    val id: Int,
    val user_id: String,
    val app_name: String,
    val start_time: Long,
    val end_time: Long,
    val device_type: String,
    val created_at: String
)
data class ScreenTimeData(
    val user_id: String,
    val app_name: String,
    val start_time: Long,
    val end_time: Long,
    val device_type: String
)
data class ScreenTimeLog(
    val user_id: Int,
    val app_name: String,
    val start_time: Long,
    val end_time: Long,
    val device_type: String
) {
    fun sessionDuration(): Long = end_time - start_time
}

data class HealthResponse(
    val status: String,
    val server_time: String,
    val database_time: String
)

data class DeviceUsage(
    val name: String,      // e.g., "Phone"
    val usageTime: String, // e.g., "3h 15m"
    val iconRes: Int       // drawable resource for icon
)

data class DevicertUsage(
    val deviceType: String,
    val totalHours: Float
)
