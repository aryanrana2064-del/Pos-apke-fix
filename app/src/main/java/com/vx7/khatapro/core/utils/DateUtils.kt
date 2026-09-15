package com.vx7.khatapro.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val defaultDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val fullDateTimeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val numericDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun formatDate(timestampMs: Long, pattern: String = "dd MMM yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestampMs))
        } catch (e: Exception) {
            defaultDateFormatter.format(Date(timestampMs))
        }
    }

    fun formatDateTime(timestampMs: Long): String {
        return fullDateTimeFormatter.format(Date(timestampMs))
    }

    fun formatNumericDate(timestampMs: Long): String {
        return numericDateFormatter.format(Date(timestampMs))
    }

    fun getStartOfDay(timestampMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfDay(timestampMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMs
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun getStartOfMonth(timestampMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMs
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
