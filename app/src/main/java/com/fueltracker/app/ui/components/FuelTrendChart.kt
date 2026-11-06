package com.fueltracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
            val validRecords = remember(records) {
                records.filter { it.calculatedConsumption != null }
            }

            if (validRecords.size < 2) {
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
                            text = "百公里油耗走势 (L/100km)",
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
            val rawMin = consumptions.minOrNull() ?: 5.0
            val rawMax = consumptions.maxOrNull() ?: 10.0
            val minVal = (rawMin - 0.6).coerceAtLeast(0.0)
            val maxVal = rawMax + 0.6

            // Zoom & Pan state
            // zoomLevel: 1.0 (default), 1.5, 2.0, 3.0, or 0.5
            var zoomLevel by remember { mutableStateOf(1.0f) }
            var scrollOffset by remember { mutableStateOf(0f) } // in pixels
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }

            // Selected point for inspection
            var selectedIndex by remember { mutableStateOf<Int?>(null) }
            val activeRecord = if (selectedIndex != null && selectedIndex in validRecords.indices) {
                validRecords[selectedIndex!!]
            } else {
                validRecords.lastOrNull()
            }

            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            val shortDateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }

            // Header Row with Title and Zoom Controls
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
                        text = "百公里油耗走势",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Zoom controls (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "缩放",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    IconButton(
                        onClick = {
                            zoomLevel = (zoomLevel - 0.3f).coerceAtLeast(0.5f)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "缩小",
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${(zoomLevel * 100).roundToInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            zoomLevel = (zoomLevel + 0.3f).coerceAtMost(4.0f)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "放大",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Point Inspection Panel (Active or Latest Record)
            if (activeRecord != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dateFormat.format(Date(activeRecord.timestamp)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeRecord.odometer.toLong()} km",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (activeRecord.distanceTraveled != null) {
                                    Text(
                                        text = " (+${activeRecord.distanceTraveled.toInt()}km)",
                                        fontSize = 10.sp,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                            Text(
                                text = "加注 ${activeRecord.fuelAmount}L · ¥${String.format(Locale.US, "%.2f", activeRecord.totalPrice)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${activeRecord.calculatedConsumption} L/100km",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                            Text(
                                text = "平均: ${String.format(Locale.US, "%.2f", avgConsumption)} L/100km",
                                fontSize = 10.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Canvas Container
            val baseSpacingPx = 48.dp
            val basePaddingPx = 36.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .onSizeChanged { canvasSize = it }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(validRecords, zoomLevel) {
                            detectTapGestures { tapOffset ->
                                val viewportW = size.width.toFloat()
                                val pointSpacing = 50.dp.toPx() * zoomLevel
                                val totalW = (validRecords.size - 1) * pointSpacing + 2 * 36.dp.toPx()
                                val maxScroll = (totalW - viewportW).coerceAtLeast(0f)
                                val currentOffset = scrollOffset.coerceIn(-maxScroll, 0f)

                                // Find closest point
                                var closestIdx = -1
                                var minDiff = Float.MAX_VALUE
                                validRecords.indices.forEach { i ->
                                    val ptX = 36.dp.toPx() + i * pointSpacing + currentOffset
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
                        .pointerInput(validRecords, zoomLevel) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val viewportW = size.width.toFloat()
                                val pointSpacing = 50.dp.toPx() * zoomLevel
                                val totalW = (validRecords.size - 1) * pointSpacing + 2 * 36.dp.toPx()
                                val maxScroll = (totalW - viewportW).coerceAtLeast(0f)
                                val newOffset = (scrollOffset + dragAmount).coerceIn(-maxScroll, 0f)
                                scrollOffset = newOffset
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingTop = 20f
                    val paddingBottom = 35f
                    val graphHeight = height - paddingTop - paddingBottom
                    val pointSpacing = 50.dp.toPx() * zoomLevel
                    val paddingSide = 36.dp.toPx()

                    val totalContentWidth = (validRecords.size - 1) * pointSpacing + 2 * paddingSide
                    val maxScroll = (totalContentWidth - width).coerceAtLeast(0f)

                    // Clamp scroll offset
                    if (scrollOffset < -maxScroll) scrollOffset = -maxScroll
                    if (scrollOffset > 0f) scrollOffset = 0f

                    val currentOffset = scrollOffset

                    // 1. Draw Average Line across entire viewport
                    val avgRatio = ((avgConsumption - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                    val avgY = paddingTop + graphHeight - (avgRatio * graphHeight)

                    // Dashed line effect
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                        start = Offset(0f, avgY),
                        end = Offset(width, avgY),
                        strokeWidth = 2.5f,
                        pathEffect = pathEffect
                    )

                    // Calculate point coordinates
                    val points = validRecords.indices.map { i ->
                        val x = paddingSide + i * pointSpacing + currentOffset
                        val ratio = ((validRecords[i].calculatedConsumption!! - minVal) / (maxVal - minVal)).toFloat()
                        val y = paddingTop + graphHeight - (ratio * graphHeight)
                        Offset(x, y)
                    }

                    // 2. Build Cubic Bezier Path
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
                                Color(0xFF10B981).copy(alpha = 0.30f),
                                Color(0xFF10B981).copy(alpha = 0.0f)
                            ),
                            startY = paddingTop,
                            endY = paddingTop + graphHeight
                        )

                        drawPath(path = fillPath, brush = areaGradient)

                        // Stroke path
                        drawPath(
                            path = path,
                            color = Color(0xFF10B981),
                            style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 3. Draw Points & Labels
                        points.forEachIndexed { i, pt ->
                            // Only draw visible or near-visible points for performance
                            if (pt.x >= -30f && pt.x <= width + 30f) {
                                val isSelected = (i == selectedIndex) || (selectedIndex == null && i == points.size - 1)

                                if (isSelected) {
                                    // Vertical guideline
                                    drawLine(
                                        color = Color(0xFF10B981).copy(alpha = 0.4f),
                                        start = Offset(pt.x, paddingTop),
                                        end = Offset(pt.x, paddingTop + graphHeight),
                                        strokeWidth = 2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                    )

                                    // Outer ring
                                    drawCircle(
                                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                                        radius = 16f,
                                        center = pt
                                    )
                                }

                                // Dot
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 8f else 5.5f,
                                    center = pt
                                )
                                drawCircle(
                                    color = if (isSelected) Color(0xFF059669) else Color(0xFF10B981),
                                    radius = if (isSelected) 8f else 5.5f,
                                    center = pt,
                                    style = Stroke(width = if (isSelected) 4.5f else 3f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Scale / Jump Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickJumpPill(
                        label = "最新",
                        onClick = {
                            val pointSpacing = 50.dp.value * zoomLevel
                            val totalW = (validRecords.size - 1) * pointSpacing + 72
                            scrollOffset = -totalW * 2f
                            selectedIndex = validRecords.size - 1
                        }
                    )
                    QuickJumpPill(
                        label = "最早",
                        onClick = {
                            scrollOffset = 0f
                            selectedIndex = 0
                        }
                    )
                    QuickJumpPill(
                        label = "均值: ${String.format(Locale.US, "%.2f", avgConsumption)}",
                        isHighlight = true,
                        onClick = {}
                    )
                }

                Text(
                    text = "👈 左右拖动图表查看 ${validRecords.size} 次历史 👉",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
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
