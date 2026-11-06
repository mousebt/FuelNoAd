package com.fueltracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.util.Calendar

@Composable
fun AnnualComparisonChart(
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    // Group records by year and month
    val yearMonthData = remember(records) {
        val cal = Calendar.getInstance()
        val map = mutableMapOf<Int, MutableMap<Int, MutableList<Double>>>()

        records.filter { it.calculatedConsumption != null }.forEach { r ->
            cal.timeInMillis = r.timestamp
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1 // 1-12
            val yearMap = map.getOrPut(year) { mutableMapOf() }
            val list = yearMap.getOrPut(month) { mutableListOf() }
            list.add(r.calculatedConsumption!!)
        }

        // Return year -> (month -> avgConsumption)
        map.mapValues { (_, monthMap) ->
            monthMap.mapValues { (_, list) -> list.average() }
        }
    }

    val availableYears = remember(yearMonthData) {
        yearMonthData.keys.sortedDescending()
    }

    if (availableYears.size < 2) {
        // Less than 2 years of data, don't show or show subtle card
        return
    }

    // Pick top 2 years for comparison (e.g. 2026 & 2025)
    val year1 = availableYears[0] // e.g. 2026
    val year2 = availableYears[1] // e.g. 2025

    val year1Data = yearMonthData[year1] ?: emptyMap()
    val year2Data = yearMonthData[year2] ?: emptyMap()

    val allVals = (year1Data.values + year2Data.values)
    val minVal = (allVals.minOrNull() ?: 5.0) - 0.5
    val maxVal = (allVals.maxOrNull() ?: 9.0) + 0.5

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp, 14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "油耗年度对比统计",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "两年对比",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas for Multi-line Annual Comparison
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingTop = 15f
                val paddingBottom = 30f
                val graphHeight = height - paddingTop - paddingBottom
                val paddingLeft = 30f
                val paddingRight = 20f
                val graphWidth = width - paddingLeft - paddingRight

                // Draw Y-axis dotted grid lines
                val gridSteps = 4
                for (s in 0..gridSteps) {
                    val y = paddingTop + (graphHeight / gridSteps) * s
                    val value = maxVal - (s.toFloat() / gridSteps) * (maxVal - minVal)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                fun getPointsForYear(data: Map<Int, Double>): List<Offset> {
                    val points = mutableListOf<Offset>()
                    for (m in 1..12) {
                        val v = data[m]
                        if (v != null) {
                            val x = paddingLeft + ((m - 1).toFloat() / 11f) * graphWidth
                            val ratio = ((v - minVal) / (maxVal - minVal)).toFloat()
                            val y = paddingTop + graphHeight - (ratio * graphHeight)
                            points.add(Offset(x, y))
                        }
                    }
                    return points
                }

                val pointsYear2 = getPointsForYear(year2Data) // Older year (Blue)
                val pointsYear1 = getPointsForYear(year1Data) // Newer year (Pink)

                // Draw lines and dots for older year (Blue: #3B82F6)
                if (pointsYear2.size >= 2) {
                    val path2 = Path().apply {
                        moveTo(pointsYear2[0].x, pointsYear2[0].y)
                        for (i in 1 until pointsYear2.size) {
                            val prev = pointsYear2[i - 1]
                            val curr = pointsYear2[i]
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path2,
                        color = Color(0xFF3B82F6),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    pointsYear2.forEach { pt ->
                        drawCircle(color = Color.White, radius = 5f, center = pt)
                        drawCircle(color = Color(0xFF3B82F6), radius = 5f, center = pt, style = Stroke(width = 2.5f))
                    }
                }

                // Draw lines and dots for newer year (Magenta: #EC4899)
                if (pointsYear1.size >= 2) {
                    val path1 = Path().apply {
                        moveTo(pointsYear1[0].x, pointsYear1[0].y)
                        for (i in 1 until pointsYear1.size) {
                            val prev = pointsYear1[i - 1]
                            val curr = pointsYear1[i]
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path1,
                        color = Color(0xFFEC4899),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    pointsYear1.forEach { pt ->
                        drawCircle(color = Color.White, radius = 5f, center = pt)
                        drawCircle(color = Color(0xFFEC4899), radius = 5f, center = pt, style = Stroke(width = 2.5f))
                    }
                }
            }

            // Month Labels (1月, 3月, 6月, 9月, 12月)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("1月", "3月", "6月", "9月", "12月").forEach { m ->
                    Text(
                        text = m,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${year2}年",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEC4899))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${year1}年",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
