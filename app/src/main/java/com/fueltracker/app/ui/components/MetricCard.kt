package com.fueltracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.DashboardStats

@Composable
fun MetricCard(
    stats: DashboardStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF0D9488),
                        Color(0xFF0F766E)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "综合平均百公里油耗",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stats.avgConsumption?.toString() ?: "--",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "L/100km",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Latest Consumption Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "最近: ${stats.latestConsumption?.toString() ?: "--"} L",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Sub metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricSubItem(
                    label = "每公里费用",
                    value = if (stats.costPerKm != null) "¥${stats.costPerKm}" else "--",
                    modifier = Modifier.weight(1f)
                )
                MetricSubItem(
                    label = "累计加油",
                    value = "${String.format("%.1f", stats.totalFuelAmount)} L",
                    modifier = Modifier.weight(1f)
                )
                MetricSubItem(
                    label = "总计支出",
                    value = "¥${String.format("%.0f", stats.totalExpense)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricSubItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
