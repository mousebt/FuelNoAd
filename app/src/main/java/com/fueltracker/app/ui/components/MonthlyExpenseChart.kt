package com.fueltracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MonthlyExpenseChart(
    records: List<FuelRecord>,
    modifier: Modifier = Modifier
) {
    // Group records by yyyy-MM across all available records, sorted chronologically
    val monthlyItems = remember(records) {
        val formatKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val formatLabel = SimpleDateFormat("M月", Locale.getDefault())

        records.groupBy { formatKey.format(Date(it.timestamp)) }
            .map { (key, list) ->
                val date = Date(list.first().timestamp)
                Triple(key, formatLabel.format(date), list.sumOf { it.totalPrice })
            }
            .sortedBy { it.first } // Chronological ascending: oldest first, newest last
    }

    if (monthlyItems.isEmpty()) return

    val avgMonthlyExpense = remember(monthlyItems) {
        monthlyItems.map { it.third }.average()
    }

    val maxAmount = remember(monthlyItems) {
        (monthlyItems.maxOf { it.third } * 1.25).coerceAtLeast(800.0)
    }

    // Pan state (defaults to NEGATIVE_INFINITY to snap to latest 12 months)
    var scrollOffset by remember { mutableStateOf(Float.NEGATIVE_INFINITY) }
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(monthlyItems.size) {
        selectedItemIndex = null
        scrollOffset = Float.NEGATIVE_INFINITY
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (monthlyItems.size > 12 && !scrollOffset.isInfinite() && scrollOffset > -1000f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .clickable {
                                    scrollOffset = Float.NEGATIVE_INFINITY
                                    selectedItemIndex = monthlyItems.size - 1
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "👉 回到最新",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "默认近12月 · 可左右拖动",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
                                // Exactly 12 columns visible across availableW
                                val slotW = if (monthlyItems.size <= 12) {
                                    availableW / monthlyItems.size.coerceAtLeast(1)
                                } else {
                                    availableW / 12f
                                }
                                val totalW = monthlyItems.size * slotW + paddingLeft + paddingRight
                                val maxScroll = (totalW - size.width.toFloat()).coerceAtLeast(0f)
                                val baseOffset = if (scrollOffset.isInfinite()) -maxScroll else scrollOffset
                                val currentOffset = baseOffset.coerceIn(-maxScroll, 0f)

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
                                    availableW / 12f
                                }
                                val totalW = monthlyItems.size * slotW + paddingLeft + paddingRight
                                val maxScroll = (totalW - size.width.toFloat()).coerceAtLeast(0f)
                                val baseOffset = if (scrollOffset.isInfinite()) -maxScroll else scrollOffset
                                scrollOffset = (baseOffset + dragAmount).coerceIn(-maxScroll, 0f)
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
                    
                    // Fixed 12-item visible scale: 12 bars span availableW exactly
                    val slotW = if (monthlyItems.size <= 12) {
                        availableW / monthlyItems.size.coerceAtLeast(1)
                    } else {
                        availableW / 12f
                    }
                    val barW = (slotW * 0.52f).coerceIn(14f, 28f)

                    val totalW = monthlyItems.size * slotW + paddingLeft + paddingRight
                    val maxScroll = (totalW - width).coerceAtLeast(0f)
                    
                    // Default to rightmost end (-maxScroll) to show latest 12 months
                    val currentOffset = if (scrollOffset.isInfinite()) {
                        -maxScroll
                    } else {
                        scrollOffset.coerceIn(-maxScroll, 0f)
                    }

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

                    // Draw Monthly Average Reference Line (Red / Orange Dashed)
                    if (avgMonthlyExpense > 0) {
                        val avgRatio = (avgMonthlyExpense / maxAmount).toFloat().coerceIn(0f, 1f)
                        val avgY = paddingTop + graphHeight - (avgRatio * graphHeight)
                        drawLine(
                            color = Color(0xFFE11D48).copy(alpha = 0.8f),
                            start = Offset(paddingLeft - 4f, avgY),
                            end = Offset(width, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }

                    // Draw Bars & Labels (Clipped to prevent bleeding over Y-axis)
                    clipRect(left = paddingLeft - 2f, top = 0f, right = width + 10f, bottom = height) {
                        drawIntoCanvas { canvas ->
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 18f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }

                            val valPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#E11D48")
                                textSize = 18f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                                isAntiAlias = true
                            }

                            monthlyItems.forEachIndexed { i, item ->
                                val centerX = paddingLeft + i * slotW + (slotW / 2) + currentOffset
                                if (centerX >= -barW && centerX <= width + barW) {
                                    val isSelected = (selectedItemIndex == i) || (selectedItemIndex == null && i == monthlyItems.size - 1)
                                    val barH = ((item.third / maxAmount) * graphHeight).toFloat().coerceAtLeast(6f)
                                    val topY = paddingTop + graphHeight - barH

                                    // Bar Rect
                                    val barColor = if (isSelected) Color(0xFFE11D48) else Color(0xFFE11D48).copy(alpha = 0.45f)
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(centerX - barW / 2, topY),
                                        size = Size(barW, barH),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )

                                    // Amount text on top
                                    canvas.nativeCanvas.drawText(
                                        "¥${item.third.roundToInt()}",
                                        centerX,
                                        topY - 6f,
                                        valPaint
                                    )

                                    // Month label below
                                    canvas.nativeCanvas.drawText(
                                        item.second,
                                        centerX,
                                        height - 4f,
                                        textPaint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE11D48))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("月度支出 (元)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE11D48).copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("月均参考: ¥${avgMonthlyExpense.roundToInt()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                val activeMonth = if (selectedItemIndex != null && selectedItemIndex in monthlyItems.indices) {
                    monthlyItems[selectedItemIndex!!]
                } else {
                    monthlyItems.lastOrNull()
                }

                if (activeMonth != null) {
                    Text(
                        text = "${activeMonth.first}: ¥${activeMonth.third.roundToInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48)
                    )
                }
            }

            if (monthlyItems.size > 12) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👈 左右拖动浏览全部 ${monthlyItems.size} 个月 👉",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "最早",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    scrollOffset = 0f
                                    selectedItemIndex = 0
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "最新",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    scrollOffset = Float.NEGATIVE_INFINITY
                                    selectedItemIndex = monthlyItems.size - 1
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
