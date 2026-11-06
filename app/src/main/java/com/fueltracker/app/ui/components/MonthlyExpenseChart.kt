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
    THIS_YEAR("今年"),
    ALL("全部")
}

@Composable
fun MonthlyExpenseChart(
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(MonthlyChartFilter.THIS_YEAR) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    val monthlyItems = remember(records, selectedFilter) {
        val cal = Calendar.getInstance()
        val formatKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val formatLabel = SimpleDateFormat("M月", Locale.getDefault())

        val filtered = when (selectedFilter) {
            MonthlyChartFilter.THIS_YEAR -> records.filter {
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.YEAR) == currentYear
            }
            MonthlyChartFilter.ALL -> records
        }

        filtered.groupBy { formatKey.format(Date(it.timestamp)) }
            .map { (key, list) ->
                val date = Date(list.first().timestamp)
                val year = key.substring(0, 4)
                Triple(key, "${formatLabel.format(date)}", list.sumOf { it.totalPrice })
            }
            .sortedBy { it.first }
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
                                val slotW = 46.dp.toPx()
                                val totalW = monthlyItems.size * slotW + 2 * 16.dp.toPx()
                                val maxScroll = (totalW - size.width.toFloat()).coerceAtLeast(0f)
                                val currentOffset = scrollOffset.coerceIn(-maxScroll, 0f)

                                val idx = ((tapOffset.x - currentOffset - 16.dp.toPx()) / slotW).toInt()
                                if (idx in monthlyItems.indices) {
                                    selectedItemIndex = idx
                                }
                            }
                        }
                        .pointerInput(monthlyItems) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val slotW = 46.dp.toPx()
                                val totalW = monthlyItems.size * slotW + 2 * 16.dp.toPx()
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
                    val slotW = 46.dp.toPx()
                    val barW = 24.dp.toPx()
                    val paddingSide = 16.dp.toPx()

                    val totalW = monthlyItems.size * slotW + 2 * paddingSide
                    val maxScroll = (totalW - width).coerceAtLeast(0f)
                    if (scrollOffset < -maxScroll) scrollOffset = -maxScroll
                    if (scrollOffset > 0f) scrollOffset = 0f
                    val currentOffset = scrollOffset

                    // Draw Average dashed red line
                    if (avgMonthlyExpense > 0) {
                        val avgY = paddingTop + graphHeight - ((avgMonthlyExpense / maxAmount) * graphHeight).toFloat()
                        drawLine(
                            color = Color(0xFFF43F5E).copy(alpha = 0.8f),
                            start = Offset(0f, avgY),
                            end = Offset(width, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // Draw avg label on right using native paint
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#F43F5E")
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f", avgMonthlyExpense),
                            width - 8f,
                            avgY - 6f,
                            paint
                        )
                    }

                    // Draw Bars, Top Amounts, and Bottom Month Labels
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    monthlyItems.forEachIndexed { i, (_, label, amount) ->
                        val centerX = paddingSide + i * slotW + (slotW / 2) + currentOffset
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

                            // Top amount
                            drawContext.canvas.nativeCanvas.drawText(
                                amount.roundToInt().toString(),
                                centerX,
                                topY - 8f,
                                textPaint
                            )

                            // Bottom month label
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                centerX,
                                height - 4f,
                                labelPaint
                            )
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
