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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
            // Keep all records chronologically sorted (oldest first, newest last)
            val validRecords = remember(records) {
                records.filter { it.calculatedConsumption != null }
                    .sortedWith(compareBy({ it.odometer }, { it.timestamp }))
            }

            if (validRecords.size < 2) {
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
                            text = "油耗变化趋势",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
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
                return@Column
            }

            // Calculations
            val consumptions = remember(validRecords) { validRecords.map { it.calculatedConsumption!! } }
            val avgConsumption = remember(consumptions) { consumptions.average() }
            val rawMin = remember(consumptions) { consumptions.minOrNull() ?: 5.0 }
            val rawMax = remember(consumptions) { consumptions.maxOrNull() ?: 9.0 }
            val minVal = (rawMin - 0.6).coerceAtLeast(0.0)
            val maxVal = rawMax + 0.6

            // Pan state (defaults to NEGATIVE_INFINITY to snap to the latest 12 items on rightmost end)
            var scrollOffset by remember { mutableStateOf(Float.NEGATIVE_INFINITY) }

            // Selected point for inspection
            var selectedIndex by remember { mutableStateOf<Int?>(null) }

            LaunchedEffect(validRecords.size) {
                selectedIndex = null
                scrollOffset = Float.NEGATIVE_INFINITY
            }

            val activeRecord = if (selectedIndex != null && selectedIndex in validRecords.indices) {
                validRecords[selectedIndex!!]
            } else {
                validRecords.lastOrNull()
            }

            val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
            val axisDateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

            // Header Row: Title & Subtitle Badge
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
                        text = "油耗变化趋势",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (validRecords.size > 12 && !scrollOffset.isInfinite() && scrollOffset > -1000f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .clickable {
                                    scrollOffset = Float.NEGATIVE_INFINITY
                                    selectedIndex = validRecords.size - 1
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
                            text = "默认近12次 · 可左右拖动",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tooltip preview panel
            if (activeRecord != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF10B981).copy(alpha = 0.2f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateFormat.format(Date(activeRecord.timestamp)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeRecord.odometer.toLong()} km" + (if (activeRecord.distanceTraveled != null) " (+${activeRecord.distanceTraveled!!.toInt()}km)" else ""),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "油耗: ${activeRecord.calculatedConsumption} 升/百公里",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                            Text(
                                text = "平均参考: ${String.format(Locale.US, "%.2f", avgConsumption)} 升/百公里",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Canvas with X/Y Axis values and Point Numbers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(validRecords) {
                            detectTapGestures { tapOffset ->
                                val paddingLeft = 38.dp.toPx()
                                val paddingRight = 20.dp.toPx()
                                val availableW = (size.width.toFloat() - paddingLeft - paddingRight).coerceAtLeast(100f)
                                // Exactly 12 points visible across availableW (11 intervals)
                                val pointSpacing = if (validRecords.size <= 12) {
                                    availableW / (validRecords.size - 1).coerceAtLeast(1)
                                } else {
                                    availableW / 11f
                                }
                                val totalW = (validRecords.size - 1) * pointSpacing + paddingLeft + paddingRight
                                val maxScroll = (totalW - size.width.toFloat()).coerceAtLeast(0f)
                                val baseOffset = if (scrollOffset.isInfinite()) -maxScroll else scrollOffset
                                val currentOffset = baseOffset.coerceIn(-maxScroll, 0f)

                                var closestIdx = -1
                                var minDiff = Float.MAX_VALUE
                                validRecords.indices.forEach { i ->
                                    val ptX = paddingLeft + i * pointSpacing + currentOffset
                                    val diff = abs(ptX - tapOffset.x)
                                    if (diff < minDiff && diff < pointSpacing) {
                                        minDiff = diff
                                        closestIdx = i
                                    }
                                }
                                if (closestIdx != -1) {
                                    selectedIndex = closestIdx
                                }
                            }
                        }
                        .pointerInput(validRecords) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val paddingLeft = 38.dp.toPx()
                                val paddingRight = 20.dp.toPx()
                                val availableW = (size.width.toFloat() - paddingLeft - paddingRight).coerceAtLeast(100f)
                                val pointSpacing = if (validRecords.size <= 12) {
                                    availableW / (validRecords.size - 1).coerceAtLeast(1)
                                } else {
                                    availableW / 11f
                                }
                                val totalW = (validRecords.size - 1) * pointSpacing + paddingLeft + paddingRight
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
                    val paddingLeft = 38.dp.toPx() // Room for Y-axis numbers
                    val paddingRight = 20.dp.toPx()
                    val availableW = (width - paddingLeft - paddingRight).coerceAtLeast(100f)
                    
                    // Fixed 12-item visible scale: 12 data points span availableW exactly
                    val pointSpacing = if (validRecords.size <= 12) {
                        availableW / (validRecords.size - 1).coerceAtLeast(1)
                    } else {
                        availableW / 11f
                    }

                    val totalContentWidth = (validRecords.size - 1) * pointSpacing + paddingLeft + paddingRight
                    val maxScroll = (totalContentWidth - width).coerceAtLeast(0f)

                    // Default to rightmost end (-maxScroll) to show latest 12 records
                    val currentOffset = if (scrollOffset.isInfinite()) {
                        -maxScroll
                    } else {
                        scrollOffset.coerceIn(-maxScroll, 0f)
                    }

                    // Text Paints for Y-axis, X-axis, and Data Points
                    val yAxisPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 21f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }

                    val pointValuePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#059669")
                        textSize = 21f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }

                    val selectedValuePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#047857")
                        textSize = 24f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }

                    val xAxisPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    // 1. Draw Horizontal Dotted Grid Lines and Y-Axis Scale Values
                    val gridSteps = 4
                    drawIntoCanvas { canvas ->
                        for (s in 0..gridSteps) {
                            val y = paddingTop + (graphHeight / gridSteps) * s
                            val scaleVal = maxVal - (s.toFloat() / gridSteps) * (maxVal - minVal)

                            // Horizontal dashed line
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.35f),
                                start = Offset(paddingLeft - 4f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Y-axis label text on the left
                            canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%.1f", scaleVal),
                                paddingLeft - 8f,
                                y + 7f,
                                yAxisPaint
                            )
                        }
                    }

                    // 2. Draw Average Reference Line (Yellow / Orange Dashed)
                    val avgRatio = ((avgConsumption - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                    val avgY = paddingTop + graphHeight - (avgRatio * graphHeight)
                    drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                        start = Offset(paddingLeft - 4f, avgY),
                        end = Offset(width, avgY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    // 3. Point Coordinates
                    val points = validRecords.indices.map { i ->
                        val x = paddingLeft + i * pointSpacing + currentOffset
                        val ratio = ((validRecords[i].calculatedConsumption!! - minVal) / (maxVal - minVal)).toFloat()
                        val y = paddingTop + graphHeight - (ratio * graphHeight)
                        Offset(x, y)
                    }

                    // 4. Cubic Bezier Curve, Fill, Points & Labels (Clipped to prevent bleeding over Y-axis)
                    clipRect(left = paddingLeft - 2f, top = 0f, right = width + 20f, bottom = height) {
                        if (points.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    val midX = (prev.x + curr.x) / 2
                                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                }
                            }

                            // Gradient Fill Area
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(points.last().x, paddingTop + graphHeight)
                                lineTo(points.first().x, paddingTop + graphHeight)
                                close()
                            }

                            val areaGradient = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.28f),
                                    Color(0xFF10B981).copy(alpha = 0.02f)
                                ),
                                startY = paddingTop,
                                endY = paddingTop + graphHeight
                            )

                            drawPath(path = fillPath, brush = areaGradient)

                            // Stroke path
                            drawPath(
                                path = path,
                                color = Color(0xFF10B981),
                                style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // 5. Draw Points, Numbers on Points, Guidelines, and X-Axis Dates
                            drawIntoCanvas { canvas ->
                                points.forEachIndexed { i, pt ->
                                    if (pt.x >= paddingLeft - 20f && pt.x <= width + 20f) {
                                        val isSelected = (i == selectedIndex) || (selectedIndex == null && i == points.size - 1)
                                        val r = validRecords[i]

                                        if (isSelected) {
                                            // Vertical guideline
                                            drawLine(
                                                color = Color(0xFF10B981).copy(alpha = 0.5f),
                                                start = Offset(pt.x, paddingTop),
                                                end = Offset(pt.x, paddingTop + graphHeight),
                                                strokeWidth = 2f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                            )
                                        }

                                        // Point Dot
                                        drawCircle(
                                            color = Color.White,
                                            radius = if (isSelected) 6.5f else 4.5f,
                                            center = pt
                                        )
                                        drawCircle(
                                            color = if (isSelected) Color(0xFF047857) else Color(0xFF10B981),
                                            radius = if (isSelected) 6.5f else 4.5f,
                                            center = pt,
                                            style = Stroke(width = if (isSelected) 3.5f else 2.5f)
                                        )

                                        // Number on Data Point
                                        val consumptionText = String.format(Locale.US, "%.1f", r.calculatedConsumption)
                                        canvas.nativeCanvas.drawText(
                                            consumptionText,
                                            pt.x,
                                            pt.y - 12f,
                                            if (isSelected) selectedValuePaint else pointValuePaint
                                        )

                                        // X-Axis Date label below point
                                        val dateText = axisDateFormat.format(Date(r.timestamp))
                                        canvas.nativeCanvas.drawText(
                                            dateText,
                                            pt.x,
                                            height - 4f,
                                            xAxisPaint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend & Extremes & Quick Jump
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
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
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("油耗 (L/100km)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("平均参考: ${String.format(Locale.US, "%.2f", avgConsumption)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Text(
                    text = "极值: ${rawMin} ~ ${rawMax}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (validRecords.size > 12) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👈 左右拖动浏览全部 ${validRecords.size} 笔记录 👉",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "最早",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    scrollOffset = 0f
                                    selectedIndex = 0
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "最新",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    scrollOffset = Float.NEGATIVE_INFINITY
                                    selectedIndex = validRecords.size - 1
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
