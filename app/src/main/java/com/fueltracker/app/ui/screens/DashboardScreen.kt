package com.fueltracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.DashboardStats
import com.fueltracker.app.model.FuelRecord
import com.fueltracker.app.model.Vehicle
import com.fueltracker.app.ui.components.FuelTrendChart
import com.fueltracker.app.ui.components.MetricCard
import com.fueltracker.app.ui.components.RecordItemCard

@Composable
fun DashboardScreen(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    stats: DashboardStats,
    records: List<FuelRecord>,
    onSelectVehicle: (Long) -> Unit,
    onDeleteRecord: (FuelRecord) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedVehicleDropdown by remember { mutableStateOf(false) }
    val currentVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vehicle Switcher Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { expandedVehicleDropdown = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🚗", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentVehicle?.name ?: "选择车辆",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "切换车辆",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

                // Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "纯本地离线",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF059669)
                    )
                }
            }
        }

        // Hero Metric Card
        item {
            MetricCard(stats = stats)
        }

        // Fuel Trend Chart Card
        item {
            FuelTrendChart(records = records)
        }

        // Recent Records Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无加油记录，点击右下角 + 开始第一笔记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(recentList, key = { it.id }) { record ->
                RecordItemCard(
                    record = record,
                    onDelete = { onDeleteRecord(record) }
                )
            }
        }
    }
}
