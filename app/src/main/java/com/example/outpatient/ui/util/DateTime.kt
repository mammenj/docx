package com.example.outpatient.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatTimestamp(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
