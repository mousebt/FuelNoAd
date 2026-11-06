package com.fueltracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class MonthlyChartFilter(val label: String) {
    LATEST_12("近12月"),
    THIS_YEAR("今年"),
    ALL("全部")
}

@Composable
fun MonthlyExpenseChart(
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(MonthlyChartFilter.LATEST_12) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    val monthlyItems = remember(records, selectedFilter) {
        val cal = Calendar.getInstance()
        val formatKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val formatLabel = SimpleDateFormat("M月", Locale.getDefault())

        val allGrouped = records.groupBy { formatKey.format(Date(it.timestamp)) }
            .map { (key, list) ->
                val date = Date(list.first().timestamp)
                val year = key.substring(0, 4)
                Triple(key, "${formatLabel.format(date)}", list.sumOf { it.totalPrice })
            }
            .sortedBy { it.first } // Chronological ascending: oldest first, newest last

        when (selectedFilter) {
            MonthlyChartFilter.LATEST_12 -> allGrouped.takeLast(12)
            MonthlyChartFilter.THIS_YEAR -> allGrouped.filter {
                cal.timeInMillis = records.firstOrNull { r -> formatKey.format(Date(r.timestamp)) == it.first }?.timestamp ?: 0L
                it.first.startsWith(currentYear.toString())
            }
            MonthlyChartFilter.ALL -> allGrouped
        }
    }

    if (monthlyItems.isEmpty()) return

    val avgMonthlyExpense = remember(monthlyItems) {
        monthlyItems.map { it.third }.average()
    }

    val maxAmount = remember(monthlyItems) {
        (monthlyItems.maxOf { it.third } * 1.25).coerceAtLeast(800.0)
    }

    var scrollOffset by remember { mutableStateOf(0f) }
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(monthlyItems) {
        // Default to focusing on the newest month
        selectedItemIndex = null
        scrollOffset = if (monthlyItems.size <= 12) 0f else -99999f
    }

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
                        text = "油费月度统计",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable { expandedDropdown = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFilter.label,
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

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        MonthlyChartFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.label) },
                                onClick = {
                                    selectedFilter = filter
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bar Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyItems) {
                            detectTapGestures { tapOffset ->
                                val paddingLeft = 36.dp.toPx()
                                val paddingRight = 16.dp.toPx()
                                val availableW = (size.width.toFloat() - paddingLeft - paddingRight).coerceAtLeast(100f)
                                val slotW = if (monthlyItems.size <= 12) {
                                    availableW / monthlyItems.size.coerceAtLeast(1)
                                } else {
                                    42.dp.toPx()
                                }
                                val totalW = monthlyItems.size * slotW + paddingLeft + paddingRight
                                val maxScroll = (totalW - size.width.toFloat()).coerceAtLeast(0f)
                                val currentOffset = scrollOffset.coerceIn(-maxScroll, 0f)

                                val idx = ((tapOffset.x - currentOffset - paddingLeft) / slotW).toInt()
                                if (idx in monthlyItems.indices) {
                                    selectedItemIndex = idx
                                }
                            }
                        }
                        .pointerInput(monthlyItems) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val paddingLeft = 36.dp.toPx()
                                val paddingRight = 16.dp.toPx()
                                val availableW = (size.width.toFloat() - paddingLeft - paddingRight).coerceAtLeast(100f)
                                val slotW = if (monthlyItems.size <= 12) {
                                    availableW / monthlyItems.size.coerceAtLeast(1)
                                } else {
                                    42.dp.toPx()
                                }
                                val totalW = monthlyItems.size * slotW + paddingLeft + paddingRight
                                val maxScroll = (totalW - size.width.toFloat()).coerceAtLeast(0f)
                                scrollOffset = (scrollOffset + dragAmount).coerceIn(-maxScroll, 0f)
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingTop = 26f
                    val paddingBottom = 26f
                    val graphHeight = height - paddingTop - paddingBottom
                    val paddingLeft = 36.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val availableW = (width - paddingLeft - paddingRight).coerceAtLeast(100f)
                    val slotW = if (monthlyItems.size <= 12) {
                        availableW / monthlyItems.size.coerceAtLeast(1)
                    } else {
                        42.dp.toPx()
                    }
                    val barW = (slotW * 0.52f).coerceIn(14f, 30f)

                    val totalW = monthlyItems.size * slotW + paddingLeft + paddingRight
                    val maxScroll = (totalW - width).coerceAtLeast(0f)
                    if (scrollOffset < -maxScroll) scrollOffset = -maxScroll
                    if (scrollOffset > 0f) scrollOffset = 0f
                    val currentOffset = scrollOffset

                    val yAxisPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }

                    // Draw Horizontal Dotted Grid Lines and Y-Axis Values
                    val gridSteps = 3
                    drawIntoCanvas { canvas ->
                        for (s in 0..gridSteps) {
                            val y = paddingTop + (graphHeight / gridSteps) * s
                            val scaleVal = maxAmount - (s.toFloat() / gridSteps) * maxAmount

                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
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

                    // Draw Average dashed red line
                    if (avgMonthlyExpense > 0) {
                        val avgY = paddingTop + graphHeight - ((avgMonthlyExpense / maxAmount) * graphHeight).toFloat()
                        drawLine(
                            color = Color(0xFFF43F5E).copy(alpha = 0.8f),
                            start = Offset(paddingLeft - 4f, avgY),
                            end = Offset(width, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // Draw avg label on right using native paint
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#F43F5E")
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "月均 ${String.format(Locale.US, "%.0f", avgMonthlyExpense)}",
                                width - 8f,
                                avgY - 6f,
                                paint
                            )
                        }
                    }

                    // Draw Bars, Top Amounts, and Bottom Month Labels (Clipped to prevent bleeding over Y-axis)
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 21f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    clipRect(left = paddingLeft - 2f, top = 0f, right = width + 10f, bottom = height) {
                        monthlyItems.forEachIndexed { i, (_, label, amount) ->
                            val centerX = paddingLeft + i * slotW + (slotW / 2) + currentOffset
                            if (centerX >= -barW && centerX <= width + barW) {
                                val barH = ((amount / maxAmount) * graphHeight).toFloat().coerceAtLeast(4f)
                                val topY = paddingTop + graphHeight - barH

                                // Vibrant Pink / Rose color like reference screenshot
                                val barColor = Color(0xFFF43F5E)

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(centerX - barW / 2, topY),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )

                                drawIntoCanvas { canvas ->
                                    // Top amount
                                    canvas.nativeCanvas.drawText(
                                        amount.roundToInt().toString(),
                                        centerX,
                                        topY - 8f,
                                        textPaint
                                    )

                                    // Bottom month label
                                    canvas.nativeCanvas.drawText(
                                        label,
                                        centerX,
                                        height - 4f,
                                        labelPaint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Year indicator dot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF43F5E))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${currentYear}年",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
