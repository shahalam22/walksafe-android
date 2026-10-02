package io.github.shahalam22.walksafe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.shahalam22.walksafe.data.model.LabelCount
import io.github.shahalam22.walksafe.data.model.SpeedPoint
import io.github.shahalam22.walksafe.ui.theme.StatusColors
import io.github.shahalam22.walksafe.ui.util.formatNumber
import io.github.shahalam22.walksafe.ui.util.pretty

private data class Risk(val color: Color, val icon: String)

private val RISK = mapOf(
    "critical" to Risk(StatusColors.critical, "‼"),
    "high" to Risk(StatusColors.serious, "!"),
    "moderate" to Risk(StatusColors.warning, "•"),
    "low" to Risk(StatusColors.good, "✓"),
)

/** Horizontal bars, one per label. Risk levels use status colours with an icon. */
@Composable
fun BarChart(items: List<LabelCount>, modifier: Modifier = Modifier, risk: Boolean = false) {
    val rows = items.filter { it.count > 0 }
    if (rows.isEmpty()) {
        EmptyText("No data yet", modifier)
        return
    }
    val max = rows.maxOf { it.count }.toFloat()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { item ->
            val r = if (risk) RISK[item.label] else null
            val label = pretty(item.label)
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = "$label: ${item.count}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (r?.icon?.let { "$it " } ?: "") + label,
                    Modifier.width(120.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .fillMaxWidth((item.count / max).coerceAtLeast(0.01f) * 0.82f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .background(r?.color ?: MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(formatNumber(item.count), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Speed over the session's frames. */
@Composable
fun SpeedChart(points: List<SpeedPoint>, modifier: Modifier = Modifier) {
    val pts = points.mapNotNull { p -> p.egoSpeedMs?.let { p.frameIdx.toFloat() to it.toFloat() } }
    if (pts.size < 2) {
        EmptyText("Not enough frames to draw", modifier)
        return
    }
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val x0 = pts.first().first
    val x1 = pts.last().first
    val yMax = niceMax(pts.maxOf { it.second })
    val peak = pts.maxOf { it.second }

    Column(modifier) {
        Text(
            "Highest ${"%.2f".format(peak)} m/s · scale 0–${"%.1f".format(yMax)} m/s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier.fillMaxWidth().height(180.dp)
                .semantics { contentDescription = "Speed chart, ${pts.size} points, highest ${"%.2f".format(peak)} m/s" },
        ) {
            val sx = { x: Float -> (x - x0) / (x1 - x0).coerceAtLeast(1f) * size.width }
            val sy = { y: Float -> (1 - y / yMax) * size.height }
            for (i in 0..4) {
                val y = size.height * i / 4
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val path = Path().apply {
                pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(sx(x), sy(y)) else lineTo(sx(x), sy(y)) }
            }
            val area = Path().apply {
                addPath(path)
                lineTo(sx(x1), size.height)
                lineTo(sx(x0), size.height)
                close()
            }
            drawPath(area, line.copy(alpha = 0.1f))
            drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(line, radius = 4.dp.toPx(), center = Offset(sx(x1), sy(pts.last().second)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Frame ${x0.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Frame ${x1.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun niceMax(v: Float): Float {
    val value = v.coerceAtLeast(0.1f)
    val p = Math.pow(10.0, Math.floor(Math.log10(value.toDouble()))).toFloat()
    return listOf(1f, 2f, 2.5f, 5f, 10f).map { it * p }.first { it >= value }
}

@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}
