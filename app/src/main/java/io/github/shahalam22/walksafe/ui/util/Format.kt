package io.github.shahalam22.walksafe.ui.util

import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun parseTime(iso: String?): OffsetDateTime? = iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

fun formatDateTime(iso: String?): String =
    parseTime(iso)?.atZoneSameInstant(ZoneId.systemDefault())?.format(dateTime) ?: "—"

fun formatDuration(startIso: String?, endIso: String?): String {
    val start = parseTime(startIso) ?: return "—"
    val end = parseTime(endIso) ?: return "—"
    val s = Duration.between(start, end).seconds.coerceAtLeast(0)
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min ${s % 60} s"
        else -> "${s / 3600} h ${(s % 3600 + 30) / 60} min"
    }
}

fun formatNumber(n: Number?): String = n?.let { NumberFormat.getIntegerInstance().format(it) } ?: "—"

/** "walk_forward" → "walk forward". */
fun pretty(value: String?): String = value?.replace('_', ' ') ?: "—"
