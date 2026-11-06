package com.fueltracker.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.DashboardStats
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AnalyticsTimeFilterType(val label: String) {
    ALL("全部"),
    YEARLY("按年度"),
    HALF_YEAR("按半年"),
    QUARTERLY("按季度"),
    CUSTOM("自定义")
}

data class MonthExpenseItem(
    val key: String,       // "2024-09"
    val label: String,     // "24/09"
    val yearMonth: String, // "2024年9月"
    val totalCost: Double,
    val totalFuel: Double,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    stats: DashboardStats,
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    // 1. Time Filter State
    var filterType by remember { mutableStateOf(AnalyticsTimeFilterType.ALL) }

    // Discover available years, half-years, and quarters from actual records
    val years = remember(records) {
        val ySet = mutableSetOf<Int>()
        val cal = Calendar.getInstance()
        records.forEach {
            cal.timeInMillis = it.timestamp
            ySet.add(cal.get(Calendar.YEAR))
        }
        ySet.sortedDescending()
    }

    var selectedYear by remember(years) { mutableStateOf(years.firstOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedHalfYear by remember { mutableStateOf(2) } // 1: 上半年 (H1), 2: 下半年 (H2)
    var selectedQuarter by remember { mutableStateOf(4) }  // 1: Q1, 2: Q2, 3: Q3, 4: Q4

    // Custom date range state (in ms)
    var customStartDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.MONTH, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        )
    }
    var customEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // 2. Filter records based on selected time window
    val filteredRecords = remember(records, filterType, selectedYear, selectedHalfYear, selectedQuarter, customStartDate, customEndDate) {
        val cal = Calendar.getInstance()
        when (filterType) {
            AnalyticsTimeFilterType.ALL -> records
            AnalyticsTimeFilterType.YEARLY -> {
                records.filter {
                    cal.timeInMillis = it.timestamp
                    cal.get(Calendar.YEAR) == selectedYear
                }
            }
            AnalyticsTimeFilterType.HALF_YEAR -> {
                records.filter {
                    cal.timeInMillis = it.timestamp
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH) // 0-based
                    val half = if (m < 6) 1 else 2
                    y == selectedYear && half == selectedHalfYear
                }
            }
            AnalyticsTimeFilterType.QUARTERLY -> {
                records.filter {
                    cal.timeInMillis = it.timestamp
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH)
                    val q = (m / 3) + 1
                    y == selectedYear && q == selectedQuarter
                }
            }
            AnalyticsTimeFilterType.CUSTOM -> {
                records.filter { it.timestamp in customStartDate..customEndDate }
            }
        }
    }

    // 3. Calculate Aggregated Metrics for the filtered range
    val totalExpense = remember(filteredRecords) { filteredRecords.sumOf { it.totalPrice } }
    val totalFuelLiters = remember(filteredRecords) { filteredRecords.sumOf { it.fuelAmount } }
    val totalDistance = remember(filteredRecords) {
        if (filteredRecords.size > 1) {
            (filteredRecords.last().odometer - filteredRecords.first().odometer).coerceAtLeast(0.0)
        } else if (filteredRecords.size == 1 && filteredRecords[0].distanceTraveled != null) {
            filteredRecords[0].distanceTraveled!!
        } else {
            0.0
        }
    }

    val validConsumptions = remember(filteredRecords) { filteredRecords.mapNotNull { it.calculatedConsumption } }
    val avgConsumption = remember(validConsumptions) {
        if (validConsumptions.isNotEmpty()) validConsumptions.average() else 0.0
    }
    val minConsumption = remember(validConsumptions) { validConsumptions.minOrNull() }
    val maxConsumption = remember(validConsumptions) { validConsumptions.maxOrNull() }

    val avgCostPerKm = remember(totalExpense, totalDistance) {
        if (totalDistance > 0) totalExpense / totalDistance else 0.0
    }
    val avgUnitPrice = remember(totalExpense, totalFuelLiters) {
        if (totalFuelLiters > 0) totalExpense / totalFuelLiters else 0.0
    }

    // 4. Monthly Expenses Grouping (accurate by yyyy-MM across all available records)
    var monthlyFilterMode by remember { mutableStateOf("近12月") }

    val allMonthlyList = remember(records) {
        val cal = Calendar.getInstance()
        val formatKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val formatLabel = SimpleDateFormat("yy/MM", Locale.getDefault())
        val formatYearMonth = SimpleDateFormat("yyyy年M月", Locale.getDefault())

        records.groupBy { formatKey.format(Date(it.timestamp)) }
            .map { (key, list) ->
                val firstDate = Date(list.first().timestamp)
                MonthExpenseItem(
                    key = key,
                    label = formatLabel.format(firstDate),
                    yearMonth = formatYearMonth.format(firstDate),
                    totalCost = list.sumOf { it.totalPrice },
                    totalFuel = list.sumOf { it.fuelAmount },
                    count = list.size
                )
            }
            .sortedBy { it.key } // chronological ascending: oldest first, newest last
    }

    val monthlyList = remember(allMonthlyList, monthlyFilterMode) {
        if (monthlyFilterMode == "近12月") {
            allMonthlyList.takeLast(12)
        } else {
            allMonthlyList
        }
    }

    val avgMonthlyExpense = remember(monthlyList) {
        if (monthlyList.isNotEmpty()) monthlyList.map { it.totalCost }.average() else 0.0
    }

    // Monthly chart pan state
    var barScrollOffset by remember { mutableStateOf(0f) }
    var selectedMonthItem by remember { mutableStateOf<MonthExpenseItem?>(null) }

    LaunchedEffect(monthlyList) {
        selectedMonthItem = monthlyList.lastOrNull()
        barScrollOffset = if (monthlyList.size <= 12) 0f else -99999f
    }

    val activeMonthItem = selectedMonthItem ?: monthlyList.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title
        Text(
            text = "用车费用与数据统计",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 1. Time Filter Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "统计时间范围",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "当前区间 ${filteredRecords.size} 笔加油",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnalyticsTimeFilterType.values().forEach { type ->
                        val isSelected = filterType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF10B981)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable { filterType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Sub-filter options (Year, Half-Year, Quarter, or Custom)
                if (filterType != AnalyticsTimeFilterType.ALL) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(10.dp))

                    when (filterType) {
                        AnalyticsTimeFilterType.YEARLY -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("选择年份:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                years.forEach { y ->
                                    val isSelected = selectedYear == y
                                    SubOptionPill(
                                        title = "${y}年",
                                        isSelected = isSelected,
                                        onClick = { selectedYear = y }
                                    )
                                }
                            }
                        }

                        AnalyticsTimeFilterType.HALF_YEAR -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("年份:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    years.forEach { y ->
                                        SubOptionPill(
                                            title = "${y}年",
                                            isSelected = selectedYear == y,
                                            onClick = { selectedYear = y }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("半年:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    SubOptionPill(
                                        title = "上半年 (1-6月)",
                                        isSelected = selectedHalfYear == 1,
                                        onClick = { selectedHalfYear = 1 }
                                    )
                                    SubOptionPill(
                                        title = "下半年 (7-12月)",
                                        isSelected = selectedHalfYear == 2,
                                        onClick = { selectedHalfYear = 2 }
                                    )
                                }
                            }
                        }

                        AnalyticsTimeFilterType.QUARTERLY -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("年份:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    years.forEach { y ->
                                        SubOptionPill(
                                            title = "${y}年",
                                            isSelected = selectedYear == y,
                                            onClick = { selectedYear = y }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("季度:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    listOf(1 to "第1季度", 2 to "第2季度", 3 to "第3季度", 4 to "第4季度").forEach { (q, label) ->
                                        SubOptionPill(
                                            title = label,
                                            isSelected = selectedQuarter == q,
                                            onClick = { selectedQuarter = q }
                                        )
                                    }
                                }
                            }
                        }

                        AnalyticsTimeFilterType.CUSTOM -> {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                    .clickable { showCustomDateDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${dateFormat.format(Date(customStartDate))} 至 ${dateFormat.format(Date(customEndDate))}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("修改区间 ✎", fontSize = 11.sp, color = Color(0xFF059669))
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }

        // 2. Core Metrics Summary Grid (支出、升数、公里数、百公里油耗)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "区间燃油开销统计",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Hero Row: Total Fuel Expense
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "💳 燃油总支出",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¥${String.format(Locale.US, "%,.2f", totalExpense)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "每公里成本",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (avgCostPerKm > 0) "¥${String.format(Locale.US, "%.2f", avgCostPerKm)}/km" else "--",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Grid for Fuel Liters, Kilometers, Average Consumption, Average Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fuel Liters Card
                    MetricBox(
                        title = "⛽ 加注总升数",
                        value = "${String.format(Locale.US, "%,.1f", totalFuelLiters)} L",
                        subtitle = "平均单价 ¥${if (avgUnitPrice > 0) String.format(Locale.US, "%.2f", avgUnitPrice) else "--"}/L",
                        modifier = Modifier.weight(1f)
                    )

                    // Kilometers Card
                    MetricBox(
                        title = "🛣️ 行驶总公里数",
                        value = "${totalDistance.toLong()} km",
                        subtitle = "共记录 ${filteredRecords.size} 笔加油",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Average Consumption
                    MetricBox(
                        title = "⚡ 综合百公里油耗",
                        value = if (avgConsumption > 0) "${String.format(Locale.US, "%.2f", avgConsumption)} L" else "--",
                        subtitle = "百公里平均油耗",
                        valueColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )

                    // Extreme range
                    MetricBox(
                        title = "📊 油耗极值区间",
                        value = if (minConsumption != null) "${minConsumption} ~ ${maxConsumption}" else "--",
                        subtitle = "最低 ~ 最高单箱",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Monthly Fuel Expense Chart Card (with Average Line, Drag & Zoom)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Zoom Controls
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
                            text = "月度燃油支出走势 (元)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Filter pill (近12月 / 全部)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable {
                                monthlyFilterMode = if (monthlyFilterMode == "近12月") "全部" else "近12月"
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthlyFilterMode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Month Inspection Card
                if (activeMonthItem != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeMonthItem.yearMonth,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "共 ${activeMonthItem.count} 笔加油 · 耗油 ${String.format(Locale.US, "%.1f", activeMonthItem.totalFuel)} L",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "¥${String.format(Locale.US, "%,.2f", activeMonthItem.totalCost)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = "月均参考: ¥${String.format(Locale.US, "%.0f", avgMonthlyExpense)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly Chart Canvas
                if (monthlyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "暂无月度数据", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    val maxCost = (monthlyList.maxOf { it.totalCost } * 1.25).coerceAtLeast(800.0)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(monthlyList) {
                                    detectTapGestures { tapOffset ->
                                        val viewportW = size.width.toFloat()
                                        val paddingLeft = 36.dp.toPx()
                                        val paddingRight = 16.dp.toPx()
                                        val availableW = (viewportW - paddingLeft - paddingRight).coerceAtLeast(100f)
                                        val slotW = if (monthlyList.size <= 12) {
                                            availableW / monthlyList.size.coerceAtLeast(1)
                                        } else {
                                            44.dp.toPx()
                                        }
                                        val totalW = monthlyList.size * slotW + paddingLeft + paddingRight
                                        val maxScroll = (totalW - viewportW).coerceAtLeast(0f)
                                        val currentOffset = barScrollOffset.coerceIn(-maxScroll, 0f)

                                        val tappedIdx = ((tapOffset.x - currentOffset - paddingLeft) / slotW).toInt()
                                        if (tappedIdx in monthlyList.indices) {
                                            selectedMonthItem = monthlyList[tappedIdx]
                                        }
                                    }
                                }
                                .pointerInput(monthlyList) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        val viewportW = size.width.toFloat()
                                        val paddingLeft = 36.dp.toPx()
                                        val paddingRight = 16.dp.toPx()
                                        val availableW = (viewportW - paddingLeft - paddingRight).coerceAtLeast(100f)
                                        val slotW = if (monthlyList.size <= 12) {
                                            availableW / monthlyList.size.coerceAtLeast(1)
                                        } else {
                                            44.dp.toPx()
                                        }
                                        val totalW = monthlyList.size * slotW + paddingLeft + paddingRight
                                        val maxScroll = (totalW - viewportW).coerceAtLeast(0f)
                                        barScrollOffset = (barScrollOffset + dragAmount).coerceIn(-maxScroll, 0f)
                                    }
                                }
                        ) {
                            val width = size.width
                            val height = size.height

                            val paddingTop = 24f
                            val paddingBottom = 26f
                            val graphHeight = height - paddingTop - paddingBottom
                            val paddingLeft = 36.dp.toPx()
                            val paddingRight = 16.dp.toPx()
                            val availableW = (width - paddingLeft - paddingRight).coerceAtLeast(100f)
                            val slotW = if (monthlyList.size <= 12) {
                                availableW / monthlyList.size.coerceAtLeast(1)
                            } else {
                                44.dp.toPx()
                            }
                            val barW = (slotW * 0.48f).coerceIn(14f, 32f)

                            val totalW = monthlyList.size * slotW + paddingLeft + paddingRight
                            val maxScroll = (totalW - width).coerceAtLeast(0f)
                            if (barScrollOffset < -maxScroll) barScrollOffset = -maxScroll
                            if (barScrollOffset > 0f) barScrollOffset = 0f

                            val currentOffset = barScrollOffset

                            val yAxisPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }

                            // 1. Draw Horizontal Dotted Grid Lines and Y-Axis Values
                            val gridSteps = 3
                            drawIntoCanvas { canvas ->
                                for (s in 0..gridSteps) {
                                    val y = paddingTop + (graphHeight / gridSteps) * s
                                    val scaleVal = maxCost - (s.toFloat() / gridSteps) * maxCost

                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.35f),
                                        start = Offset(paddingLeft - 4f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                    )

                                    canvas.nativeCanvas.drawText(
                                        scaleVal.roundToInt().toString(),
                                        paddingLeft - 6f,
                                        y + 7f,
                                        yAxisPaint
                                    )
                                }
                            }

                            // 2. Draw Average dashed line across chart
                            if (avgMonthlyExpense > 0) {
                                val avgRatio = (avgMonthlyExpense / maxCost).toFloat().coerceIn(0f, 1f)
                                val avgY = paddingTop + graphHeight - (avgRatio * graphHeight)
                                drawLine(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                                    start = Offset(paddingLeft - 4f, avgY),
                                    end = Offset(width, avgY),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )

                                val avgPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#D97706")
                                    textSize = 21f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    isAntiAlias = true
                                    isFakeBoldText = true
                                }
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawText(
                                        "月均 ¥${avgMonthlyExpense.roundToInt()}",
                                        width - 8f,
                                        avgY - 6f,
                                        avgPaint
                                    )
                                }
                            }

                            // 3. Draw Bars, Top Amounts, and Bottom Month Labels (Clipped to prevent bleeding over Y-axis)
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            }

                            val labelPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }

                            clipRect(left = paddingLeft - 2f, top = 0f, right = width + 10f, bottom = height) {
                                monthlyList.forEachIndexed { i, item ->
                                    val centerX = paddingLeft + i * slotW + (slotW / 2) + currentOffset
                                    if (centerX >= -barW && centerX <= width + barW) {
                                        val isSelected = (selectedMonthItem == item) || (selectedMonthItem == null && i == monthlyList.size - 1)
                                        val barH = ((item.totalCost / maxCost) * graphHeight).toFloat().coerceAtLeast(6f)
                                        val topY = paddingTop + graphHeight - barH

                                        val barColor = if (isSelected) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.45f)

                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = Offset(centerX - barW / 2, topY),
                                            size = Size(barW, barH),
                                            cornerRadius = CornerRadius(6f, 6f)
                                        )

                                        drawIntoCanvas { canvas ->
                                            // Amount on top of each bar
                                            canvas.nativeCanvas.drawText(
                                                item.totalCost.roundToInt().toString(),
                                                centerX,
                                                topY - 6f,
                                                textPaint
                                            )

                                            // Month label below each bar (X-axis)
                                            val monthNum = try { item.key.substring(5).toInt() } catch (_: Exception) { 1 }
                                            val yearStr = try { item.key.substring(2, 4) } catch (_: Exception) { "" }
                                            val monthLabel = if (monthNum == 1 || i == 0) "${yearStr}年${monthNum}月" else "${monthNum}月"

                                            canvas.nativeCanvas.drawText(
                                                monthLabel,
                                                centerX,
                                                height - 6f,
                                                labelPaint
                                            )
                                        }

                                        if (isSelected) {
                                            // Indicator dot below selected bar
                                            drawCircle(
                                                color = Color(0xFF059669),
                                                radius = 3.5f,
                                                center = Offset(centerX, height - 1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom hints and controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickJumpPill(
                                label = "本月",
                                onClick = {
                                    barScrollOffset = -99999f
                                    selectedMonthItem = monthlyList.lastOrNull()
                                }
                            )
                            QuickJumpPill(
                                label = "最早",
                                onClick = {
                                    barScrollOffset = 0f
                                    selectedMonthItem = monthlyList.firstOrNull()
                                }
                            )
                            QuickJumpPill(
                                label = "月均: ¥${avgMonthlyExpense.roundToInt()}",
                                isHighlight = true,
                                onClick = {}
                            )
                        }

                        if (monthlyList.size > 12) {
                            Text(
                                text = "👈 左右拖动查看 ${monthlyList.size} 个月数据 👉",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            initialStart = customStartDate,
            initialEnd = customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                customStartDate = start
                customEndDate = end
                showCustomDateDialog = false
            }
        )
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun SubOptionPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickJumpPill(
    label: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isHighlight) Color(0xFFF59E0B).copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isHighlight) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CustomDateRangeDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var startStr by remember { mutableStateOf(dateFormat.format(Date(initialStart))) }
    var endStr by remember { mutableStateOf(dateFormat.format(Date(initialEnd))) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义时间区间", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("输入格式为 YYYY-MM-DD，如 2025-01-01", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = startStr,
                    onValueChange = { startStr = it; error = null },
                    label = { Text("起始日期") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endStr,
                    onValueChange = { endStr = it; error = null },
                    label = { Text("结束日期") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cal = Calendar.getInstance()
                    val today = cal.timeInMillis

                    QuickJumpPill(label = "近3个月") {
                        cal.timeInMillis = today
                        cal.add(Calendar.MONTH, -3)
                        startStr = dateFormat.format(Date(cal.timeInMillis))
                        endStr = dateFormat.format(Date(today))
                    }
                    QuickJumpPill(label = "近半年") {
                        cal.timeInMillis = today
                        cal.add(Calendar.MONTH, -6)
                        startStr = dateFormat.format(Date(cal.timeInMillis))
                        endStr = dateFormat.format(Date(today))
                    }
                    QuickJumpPill(label = "近1年") {
                        cal.timeInMillis = today
                        cal.add(Calendar.YEAR, -1)
                        startStr = dateFormat.format(Date(cal.timeInMillis))
                        endStr = dateFormat.format(Date(today))
                    }
                }

                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val parsedStart = dateFormat.parse(startStr.trim())?.time
                        val parsedEnd = dateFormat.parse(endStr.trim())?.time

                        if (parsedStart == null || parsedEnd == null) {
                            error = "请输入正确的日期格式 (如 2025-01-01)"
                            return@Button
                        }
                        if (parsedStart > parsedEnd) {
                            error = "起始日期不能晚于结束日期"
                            return@Button
                        }
                        // Set end date to end of that day (23:59:59)
                        val endCal = Calendar.getInstance().apply {
                            timeInMillis = parsedEnd
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        onConfirm(parsedStart, endCal.timeInMillis)
                    } catch (e: Exception) {
                        error = "日期解析错误，请检查格式"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("确定筛选")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
