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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.roundToInt

enum class TrendTimeFilter(val label: String) {
    ALL("全部"),
    ONE_YEAR("近1年"),
    HALF_YEAR("近半年")
}

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
            var selectedFilter by remember { mutableStateOf(TrendTimeFilter.ALL) }
            var expandedFilterDropdown by remember { mutableStateOf(false) }

            val validRecords = remember(records, selectedFilter) {
                val base = records.filter { it.calculatedConsumption != null }
                when (selectedFilter) {
                    TrendTimeFilter.ALL -> base
                    TrendTimeFilter.ONE_YEAR -> {
                        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 3600 * 1000
                        base.filter { it.timestamp >= oneYearAgo }
                    }
                    TrendTimeFilter.HALF_YEAR -> {
                        val halfYearAgo = System.currentTimeMillis() - 182L * 24 * 3600 * 1000
                        base.filter { it.timestamp >= halfYearAgo }
                    }
                }
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

            // Zoom & Pan state
            var zoomLevel by remember { mutableStateOf(1.0f) }
            var scrollOffset by remember { mutableStateOf(0f) }

            // Selected point for inspection
            var selectedIndex by remember { mutableStateOf<Int?>(null) }
            val activeRecord = if (selectedIndex != null && selectedIndex in validRecords.indices) {
                validRecords[selectedIndex!!]
            } else {
                validRecords.lastOrNull()
            }

            val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
            val shortDateFormat = remember { SimpleDateFormat("yy/MM", Locale.getDefault()) }

            // Header Row: Title & Filter / Zoom controls
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time filter dropdown
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { expandedFilterDropdown = true }
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
                            expanded = expandedFilterDropdown,
                            onDismissRequest = { expandedFilterDropdown = false }
                        ) {
                            TrendTimeFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.label) },
                                    onClick = {
                                        selectedFilter = filter
                                        expandedFilterDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Zoom (+ / -)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f) },
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "缩小", modifier = Modifier.size(12.dp))
                        }
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.0f) },
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "放大", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tooltip Card hovering / preview panel
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

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
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
                                scrollOffset = (scrollOffset + dragAmount).coerceIn(-maxScroll, 0f)
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingTop = 15f
                    val paddingBottom = 30f
                    val graphHeight = height - paddingTop - paddingBottom
                    val pointSpacing = 50.dp.toPx() * zoomLevel
                    val paddingSide = 36.dp.toPx()

                    val totalContentWidth = (validRecords.size - 1) * pointSpacing + 2 * paddingSide
                    val maxScroll = (totalContentWidth - width).coerceAtLeast(0f)

                    if (scrollOffset < -maxScroll) scrollOffset = -maxScroll
                    if (scrollOffset > 0f) scrollOffset = 0f

                    val currentOffset = scrollOffset

                    // Draw Horizontal Dotted Grid Lines (5, 6, 7, 8, 9)
                    val gridSteps = 4
                    for (s in 0..gridSteps) {
                        val y = paddingTop + (graphHeight / gridSteps) * s
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Draw Average Reference Line (Yellow / Orange Dashed)
                    val avgRatio = ((avgConsumption - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                    val avgY = paddingTop + graphHeight - (avgRatio * graphHeight)
                    drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = 0.75f),
                        start = Offset(0f, avgY),
                        end = Offset(width, avgY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    // Point Coordinates
                    val points = validRecords.indices.map { i ->
                        val x = paddingSide + i * pointSpacing + currentOffset
                        val ratio = ((validRecords[i].calculatedConsumption!! - minVal) / (maxVal - minVal)).toFloat()
                        val y = paddingTop + graphHeight - (ratio * graphHeight)
                        Offset(x, y)
                    }

                    // Cubic Bezier Curve
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
                            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Points & guidelines
                        points.forEachIndexed { i, pt ->
                            if (pt.x >= -30f && pt.x <= width + 30f) {
                                val isSelected = (i == selectedIndex) || (selectedIndex == null && i == points.size - 1)

                                if (isSelected) {
                                    // Vertical guideline
                                    drawLine(
                                        color = Color(0xFF10B981).copy(alpha = 0.5f),
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

                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 7.5f else 5f,
                                    center = pt
                                )
                                drawCircle(
                                    color = if (isSelected) Color(0xFF059669) else Color(0xFF10B981),
                                    radius = if (isSelected) 7.5f else 5f,
                                    center = pt,
                                    style = Stroke(width = if (isSelected) 4f else 3f)
                                )
                            }
                        }
                    }
                }
            }

            // Legend & Extremes
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
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("油耗", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("平均油耗参考", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Text(
                    text = "极值: ${rawMin} ~ ${rawMax}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
