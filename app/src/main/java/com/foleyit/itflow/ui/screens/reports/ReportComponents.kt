package com.foleyit.itflow.ui.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A labeled value with a bar proportional to [fraction] (0f..1f) of the series' max — used
 * for client/tech/category/priority/status breakdowns across the reports section. */
@Composable
fun ReportBarRow(
    label: String,
    value: String,
    fraction: Float,
    subtitle: String? = null,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium, maxLines = 1)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Text(value, fontWeight = FontWeight.SemiBold, color = barColor)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(barColor, MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

private val MONTH_LABELS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

/** Lightweight month-over-month bar chart (no external charting dependency). [values] must have 12 entries. */
@Composable
fun MonthlyTrendChart(values: List<Double>, modifier: Modifier = Modifier, barColor: Color = MaterialTheme.colorScheme.primary) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val barCount = values.size.coerceAtLeast(1)
            val gap = size.width * 0.015f
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            values.forEachIndexed { i, v ->
                val barHeight = (v / max).toFloat().coerceIn(0f, 1f) * size.height
                val x = i * (barWidth + gap)
                drawRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height)
                )
                if (barHeight > 0f) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            MONTH_LABELS.forEach { m ->
                Text(
                    m,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** Simple prev/year/next selector shared by every `?year=` report. */
@Composable
fun YearPicker(year: Int, onYearChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onYearChange(year - 1) }) {
            Icon(Icons.Outlined.ChevronLeft, "Previous year")
        }
        Text(year.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { onYearChange(year + 1) }) {
            Icon(Icons.Outlined.ChevronRight, "Next year")
        }
    }
}

fun formatSeconds(seconds: Long?): String {
    if (seconds == null) return "-"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
