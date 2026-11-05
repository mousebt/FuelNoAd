package com.fueltracker.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.DashboardStats
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    stats: DashboardStats,
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    val validConsumptions = records.mapNotNull { it.calculatedConsumption }
    val minConsumption = validConsumptions.minOrNull()
    val maxConsumption = validConsumptions.maxOrNull()

    val avgPrice = if (records.isNotEmpty()) {
        records.map { it.unitPrice }.average()
    } else 0.0

    val avgDistance = if (records.size > 1) {
        val deltas = records.mapNotNull { it.distanceTraveled }
        if (deltas.isNotEmpty()) deltas.average() else 0.0
    } else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "用车费用与数据统计",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Monthly Expense Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "月度燃油支出 (元)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Group by month
                val monthFormat = SimpleDateFormat("M月", Locale.getDefault())
                val monthlyMap = records.groupBy { monthFormat.format(Date(it.timestamp)) }
                    .mapValues { entry -> entry.value.sumOf { it.totalPrice } }

                val months = if (monthlyMap.isNotEmpty()) monthlyMap.keys.toList().takeLast(5) else listOf("本月")
                val amounts = if (monthlyMap.isNotEmpty()) months.map { monthlyMap[it] ?: 0.0 } else listOf(0.0)
                val maxAmount = (amounts.maxOrNull() ?: 1000.0).coerceAtLeast(500.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingBottom = 30f
                    val graphHeight = height - paddingBottom
                    val barWidth = 50f
                    val slotWidth = width / months.size

                    months.indices.forEach { i ->
                        val centerX = (i * slotWidth) + (slotWidth / 2)
                        val amount = amounts[i]
                        val barHeight = ((amount / maxAmount) * (graphHeight - 20f)).toFloat()
                        val topY = (graphHeight - barHeight).coerceAtLeast(10f)

                        val barColor = if (i == months.size - 1) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.4f)

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(centerX - barWidth / 2, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }

                // Month labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    months.forEachIndexed { i, m ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "¥${amounts[i].toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = m,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Key Metrics Analysis List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "关键指标分析",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatRow(
                    label = "最低单箱油耗",
                    value = if (minConsumption != null) "$minConsumption L/100km" else "--",
                    valueColor = Color(0xFF059669)
                )
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                StatRow(
                    label = "最高单箱油耗",
                    value = if (maxConsumption != null) "$maxConsumption L/100km" else "--",
                    valueColor = Color(0xFFF59E0B)
                )
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                StatRow(
                    label = "平均加油单价",
                    value = if (avgPrice > 0) "¥${String.format("%.2f", avgPrice)} / L" else "--"
                )
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                StatRow(
                    label = "平均每箱行驶里程",
                    value = if (avgDistance > 0) "${avgDistance.toInt()} km" else "--"
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
