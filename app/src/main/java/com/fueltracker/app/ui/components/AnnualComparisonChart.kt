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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.util.Calendar
import java.util.Locale

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

        map.mapValues { (_, monthMap) ->
            monthMap.mapValues { (_, list) -> list.average() }
        }
    }

    val availableYears = remember(yearMonthData) {
        yearMonthData.keys.sortedDescending()
    }

    if (availableYears.size < 2) {
        return
    }

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
                    .height(170.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingTop = 26f
                val paddingBottom = 26f
                val graphHeight = height - paddingTop - paddingBottom
                val paddingLeft = 38.dp.toPx()
                val paddingRight = 16.dp.toPx()
                val graphWidth = width - paddingLeft - paddingRight

                val yAxisPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 21f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                val xAxisPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 20f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val bluePointPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#2563EB")
                    textSize = 20f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }

                val pinkPointPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#DB2777")
                    textSize = 20f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }

                // 1. Draw Y-axis dotted grid lines and Y-axis values
                val gridSteps = 4
                drawIntoCanvas { canvas ->
                    for (s in 0..gridSteps) {
                        val y = paddingTop + (graphHeight / gridSteps) * s
                        val value = maxVal - (s.toFloat() / gridSteps) * (maxVal - minVal)

                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(paddingLeft - 4f, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f", value),
                            paddingLeft - 8f,
                            y + 7f,
                            yAxisPaint
                        )
                    }
                }

                data class MonthPoint(val month: Int, val value: Double, val offset: Offset)

                fun getPointsForYear(data: Map<Int, Double>): List<MonthPoint> {
                    val points = mutableListOf<MonthPoint>()
                    for (m in 1..12) {
                        val v = data[m]
                        if (v != null) {
                            val x = paddingLeft + ((m - 1).toFloat() / 11f) * graphWidth
                            val ratio = ((v - minVal) / (maxVal - minVal)).toFloat()
                            val y = paddingTop + graphHeight - (ratio * graphHeight)
                            points.add(MonthPoint(m, v, Offset(x, y)))
                        }
                    }
                    return points
                }

                val pointsYear2 = getPointsForYear(year2Data) // Older year (Blue)
                val pointsYear1 = getPointsForYear(year1Data) // Newer year (Pink)

                // 2. Draw lines, dots, and numbers for older year (Blue: #3B82F6)
                if (pointsYear2.size >= 2) {
                    val path2 = Path().apply {
                        moveTo(pointsYear2[0].offset.x, pointsYear2[0].offset.y)
                        for (i in 1 until pointsYear2.size) {
                            val prev = pointsYear2[i - 1].offset
                            val curr = pointsYear2[i].offset
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path2,
                        color = Color(0xFF3B82F6),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // 3. Draw lines, dots, and numbers for newer year (Magenta: #EC4899)
                if (pointsYear1.size >= 2) {
                    val path1 = Path().apply {
                        moveTo(pointsYear1[0].offset.x, pointsYear1[0].offset.y)
                        for (i in 1 until pointsYear1.size) {
                            val prev = pointsYear1[i - 1].offset
                            val curr = pointsYear1[i].offset
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path1,
                        color = Color(0xFFEC4899),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw dots and numbers on data points
                drawIntoCanvas { canvas ->
                    pointsYear2.forEach { mp ->
                        drawCircle(color = Color.White, radius = 5f, center = mp.offset)
                        drawCircle(color = Color(0xFF3B82F6), radius = 5f, center = mp.offset, style = Stroke(width = 2.5f))

                        canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f", mp.value),
                            mp.offset.x,
                            mp.offset.y - 10f,
                            bluePointPaint
                        )
                    }

                    pointsYear1.forEach { mp ->
                        drawCircle(color = Color.White, radius = 5f, center = mp.offset)
                        drawCircle(color = Color(0xFFEC4899), radius = 5f, center = mp.offset, style = Stroke(width = 2.5f))

                        canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f", mp.value),
                            mp.offset.x,
                            mp.offset.y - 10f,
                            pinkPointPaint
                        )
                    }

                    // 4. Draw X-axis month numbers at bottom (1月 ~ 12月)
                    for (m in 1..12) {
                        val x = paddingLeft + ((m - 1).toFloat() / 11f) * graphWidth
                        canvas.nativeCanvas.drawText(
                            "${m}月",
                            x,
                            height - 4f,
                            xAxisPaint
                        )
                    }
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
