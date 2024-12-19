package com.example.carpool.utils

import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun parseTimeRange(timeRange: String): Pair<LocalTime, LocalTime> {
    val times = timeRange.split(" - ")
    val formatter = DateTimeFormatter.ofPattern("H:mm")
    val startTime = LocalTime.parse(times[0].trim(), formatter)
    val endTime = LocalTime.parse(times[1].trim(), formatter)
    return Pair(startTime, endTime)
}

fun isCurrentTimeInRange(timeRange: String): Boolean {
    val (startTime, endTime) = parseTimeRange(timeRange)
    val currentTime = LocalTime.now()
    return currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
}