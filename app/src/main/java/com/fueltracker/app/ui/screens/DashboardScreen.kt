package com.fueltracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.DashboardStats
import com.fueltracker.app.model.FuelRecord
import com.fueltracker.app.model.Vehicle
import com.fueltracker.app.ui.components.AnnualComparisonChart
import com.fueltracker.app.ui.components.FuelTrendChart
import com.fueltracker.app.ui.components.MonthlyExpenseChart
import com.fueltracker.app.ui.components.RecordItemCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class DashboardStatsFilter(val label: String) {
    ALL("全部"),
    HALF_YEAR("半年"),
    THIS_YEAR("今年"),
    QUARTER("季度")
}

@Composable
fun DashboardScreen(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    stats: DashboardStats,
    records: List<FuelRecord>,
    onSelectVehicle: (Long) -> Unit,
    onEditRecord: (FuelRecord) -> Unit,
    onDeleteRecord: (FuelRecord) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit = onNavigateToHistory,
    modifier: Modifier = Modifier
) {
    var expandedVehicleDropdown by remember { mutableStateOf(false) }
    var recordPendingDelete by remember { mutableStateOf<FuelRecord?>(null) }
    val currentVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.firstOrNull()

    // 1. Time Filter for the 2x3 Statistics Card
    var statsFilter by remember { mutableStateOf(DashboardStatsFilter.HALF_YEAR) }
    var expandedStatsDropdown by remember { mutableStateOf(false) }

    val filteredStatsRecords = remember(records, statsFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (statsFilter) {
            DashboardStatsFilter.ALL -> records
            DashboardStatsFilter.HALF_YEAR -> {
                val halfYearAgo = now - 182L * 24 * 3600 * 1000
                records.filter { it.timestamp >= halfYearAgo }
            }
            DashboardStatsFilter.THIS_YEAR -> {
                val year = cal.get(Calendar.YEAR)
                records.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == year
                }
            }
            DashboardStatsFilter.QUARTER -> {
                val quarterAgo = now - 91L * 24 * 3600 * 1000
                records.filter { it.timestamp >= quarterAgo }
            }
        }
    }

    // Dynamic metrics for the selected time filter
    val validConsumptions = remember(filteredStatsRecords) { filteredStatsRecords.mapNotNull { it.calculatedConsumption } }
    val displayAvgConsumption = remember(validConsumptions) {
        if (validConsumptions.isNotEmpty()) validConsumptions.average() else stats.avgConsumption ?: 0.0
    }
    val latestConsumption = remember(records) {
        records.reversed().firstOrNull { it.calculatedConsumption != null }?.calculatedConsumption ?: displayAvgConsumption
    }

    val displayDistance = remember(filteredStatsRecords) {
        if (filteredStatsRecords.size > 1) {
            (filteredStatsRecords.last().odometer - filteredStatsRecords.first().odometer).coerceAtLeast(0.0)
        } else if (filteredStatsRecords.size == 1 && filteredStatsRecords[0].distanceTraveled != null) {
            filteredStatsRecords[0].distanceTraveled!!
        } else {
            0.0
        }
    }

    val displayTotalExpense = remember(filteredStatsRecords) { filteredStatsRecords.sumOf { it.totalPrice } }
    val displayTotalFuel = remember(filteredStatsRecords) { filteredStatsRecords.sumOf { it.fuelAmount } }

    val daysSpan = remember(filteredStatsRecords) {
        if (filteredStatsRecords.size > 1) {
            val diffMs = filteredStatsRecords.last().timestamp - filteredStatsRecords.first().timestamp
            max(1, (diffMs / (1000L * 60 * 60 * 24)).toInt())
        } else {
            1
        }
    }

    val dailyMileage = remember(displayDistance, daysSpan) {
        if (displayDistance > 0 && daysSpan > 0) (displayDistance / daysSpan).roundToInt() else 0
    }

    val costPerKm = remember(displayTotalExpense, displayDistance) {
        if (displayDistance > 0) displayTotalExpense / displayDistance else 0.0
    }

    // Driving efficiency rating & title badge
    val (titleBadge, starCount) = remember(displayAvgConsumption) {
        when {
            displayAvgConsumption <= 0.0 -> "初试身手 🔰" to 3
            displayAvgConsumption < 6.0 -> "黄金右脚 🏆" to 5
            displayAvgConsumption < 7.0 -> "节能先锋 ⭐" to 4
            displayAvgConsumption < 8.0 -> "中规中矩 🚗" to 3
            displayAvgConsumption < 9.0 -> "略显油腻 ⛽" to 2
            else -> "首席拖拉机手 🚜" to 1
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Gradient Hero Header Area (Inspired by reference screenshot)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0284C7),
                                Color(0xFF0EA5E9),
                                Color(0xFF38BDF8).copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 10.dp)
            ) {
                Column {
                    // Top City & Reference Fuel Price Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📍 本地油价", fontSize = 11.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentVehicle?.fuelType ?: "92#"} 8.05 元/升",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "纯本地离线",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vehicle Identity & Switcher
                    Box {
                        Column(
                            modifier = Modifier
                                .clickable { expandedVehicleDropdown = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentVehicle?.name ?: "选择车辆",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "切换车辆",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "${currentVehicle?.plateNumber?.ifBlank { "无车牌" }} · ${currentVehicle?.fuelType} · 油箱 ${currentVehicle?.tankCapacity?.toInt()}L",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        DropdownMenu(
                            expanded = expandedVehicleDropdown,
                            onDismissRequest = { expandedVehicleDropdown = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${v.name} (${v.plateNumber.ifBlank { v.fuelType }})",
                                            fontWeight = if (v.id == selectedVehicleId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectVehicle(v.id)
                                        expandedVehicleDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hero Consumption Card ("最新油耗" & "驾驶头衔")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Latest consumption
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "最新油耗",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = ">",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = if (latestConsumption > 0) String.format(Locale.US, "%.2f", latestConsumption) else "--",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "升/百公里",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            // Right side: Efficiency title & Stars
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = titleBadge,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= starCount) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2x3 Statistics Grid Card ("统计" with Time Switcher)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        Text(
                            text = "统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .clickable { expandedStatsDropdown = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statsFilter.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            DropdownMenu(
                                expanded = expandedStatsDropdown,
                                onDismissRequest = { expandedStatsDropdown = false }
                            ) {
                                DashboardStatsFilter.values().forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label) },
                                        onClick = {
                                            statsFilter = filter
                                            expandedStatsDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 1: 平均油耗, 平均行程, 平均油费
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardMetricCell(
                            label = "平均油耗",
                            value = if (displayAvgConsumption > 0) String.format(Locale.US, "%.2f", displayAvgConsumption) else "--",
                            unit = "升/百公里",
                            valueColor = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCell(
                            label = "平均行程",
                            value = if (dailyMileage > 0) dailyMileage.toString() else "--",
                            unit = "公里/天",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCell(
                            label = "平均油费",
                            value = if (costPerKm > 0) String.format(Locale.US, "%.2f", costPerKm) else "--",
                            unit = "元/公里",
                            valueColor = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    // Row 2: 统计里程, 累计油费, 累计加注
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardMetricCell(
                            label = "统计里程",
                            value = displayDistance.toLong().toString(),
                            unit = "公里",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCell(
                            label = "累计油费",
                            value = String.format(Locale.US, "%.2f", displayTotalExpense),
                            unit = "元",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCell(
                            label = "累计加注",
                            value = String.format(Locale.US, "%.1f", displayTotalFuel),
                            unit = "升",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Card 1: "油耗变化趋势" (FuelTrendChart)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                FuelTrendChart(records = records)
            }
        }

        // Card 2: "油费月度统计" (MonthlyExpenseChart)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MonthlyExpenseChart(records = records)
            }
        }

        // Card 3: "油耗年度对比统计" (AnnualComparisonChart)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                AnnualComparisonChart(records = records)
            }
        }

        // Detailed Report Banner Entry
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToAnalytics() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "更多详细统计报告",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "按季度、半年、年度、自定义时间段深度统算",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToAnalytics,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("点击查看", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Records Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⛽ 最近加油明细",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onNavigateToHistory) {
                    Text(
                        text = "查看全部 →",
                        fontSize = 12.sp,
                        color = Color(0xFF059669)
                    )
                }
            }
        }

        // Recent records (last 4 items, reversed)
        val recentList = records.reversed().take(4)
        if (recentList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无加油记录，点击底部 + 开始第一笔记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(recentList, key = { it.id }) { record ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RecordItemCard(
                        record = record,
                        onEdit = { onEditRecord(record) },
                        onDelete = { recordPendingDelete = record }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (recordPendingDelete != null) {
        val target = recordPendingDelete!!
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(target.timestamp))
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = {
                Text(text = "删除加油记录", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "确定要删除 ${dateStr}（里程 ${target.odometer.toLong()} km，金额 ¥${String.format(Locale.US, "%.2f", target.totalPrice)}）这条加油记录吗？\n\n删除后系统将自动重新计算相关的油耗与统计数据。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecord(target)
                        recordPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DashboardMetricCell(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = unit,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}
