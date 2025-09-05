package com.example.azarbijan_tech

data class UsageSession(
    val appName: String,
    val packageName: String,
    val startTime: Long,
    val endTime: Long
)