package io.github.shahalam22.walksafe.data.admin

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Rows as CSV, with the columns of the first row. */
fun toCsv(rows: List<JsonObject>): String {
    if (rows.isEmpty()) return ""
    val cols = rows.first().keys.toList()
    val lines = mutableListOf(cols.joinToString(","))
    rows.mapTo(lines) { row -> cols.joinToString(",") { cell(row[it]) } }
    return lines.joinToString("\n")
}

private fun cell(value: Any?): String {
    val s = when (value) {
        null, JsonNull -> ""
        is JsonPrimitive -> value.content
        else -> value.toString()
    }
    return if (s.any { it == '"' || it == ',' || it == '\n' }) "\"${s.replace("\"", "\"\"")}\"" else s
}
