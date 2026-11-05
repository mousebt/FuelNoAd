package com.fueltracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FuelTrendChart(
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "百公里油耗走势 (L/100km)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "近几次加满",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val validRecords = records.filter { it.calculatedConsumption != null }

            if (validRecords.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "至少需要2次加满记录以生成走势折线图",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                val chartRecords = validRecords.takeLast(6)
                val consumptions = chartRecords.map { it.calculatedConsumption!! }
                val minVal = (consumptions.minOrNull() ?: 5.0) - 0.5
                val maxVal = (consumptions.maxOrNull() ?: 10.0) + 0.5

                val lineColor = Color(0xFF10B981)
                val areaGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.35f),
                        Color(0xFF10B981).copy(alpha = 0.0f)
                    )
                )

                val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingHorizontal = 32f
                    val paddingTop = 20f
                    val paddingBottom = 20f

                    val graphWidth = width - paddingHorizontal * 2
                    val graphHeight = height - paddingTop - paddingBottom

                    val points = chartRecords.indices.map { i ->
                        val x = paddingHorizontal + (i.toFloat() / (chartRecords.size - 1)) * graphWidth
                        val ratio = ((chartRecords[i].calculatedConsumption!! - minVal) / (maxVal - minVal)).toFloat()
                        val y = paddingTop + graphHeight - (ratio * graphHeight)
                        Offset(x, y)
                    }

                    // Build smooth cubic Bezier path
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }

                    // Fill gradient area below path
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, paddingTop + graphHeight)
                        lineTo(points.first().x, paddingTop + graphHeight)
                        close()
                    }
                    drawPath(path = fillPath, brush = areaGradient)

                    // Draw stroke line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw point dots
                    points.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = pt
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 8f,
                            center = pt,
                            style = Stroke(width = 4f)
                        )
                    }
                }

                // X-Axis Date Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    chartRecords.forEach { r ->
                        Text(
                            text = dateFormat.format(Date(r.timestamp)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
