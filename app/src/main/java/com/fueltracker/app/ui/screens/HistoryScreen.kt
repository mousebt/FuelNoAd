package com.fueltracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord
import com.fueltracker.app.ui.components.RecordItemCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistoryFilter {
    ALL,
    ONLY_FULL,
    ONLY_PARTIAL
}

@Composable
fun HistoryScreen(
    records: List<FuelRecord>,
    onEditRecord: (FuelRecord) -> Unit,
    onDeleteRecord: (FuelRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var recordPendingDelete by remember { mutableStateOf<FuelRecord?>(null) }

    val filteredRecords = remember(records, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> records
            HistoryFilter.ONLY_FULL -> records.filter { it.isFull }
            HistoryFilter.ONLY_PARTIAL -> records.filter { !it.isFull }
        }.reversed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "全部加油记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "共 ${filteredRecords.size} 笔 (点击卡片可修改)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill(
                title = "全部",
                isSelected = selectedFilter == HistoryFilter.ALL,
                onClick = { selectedFilter = HistoryFilter.ALL }
            )
            FilterPill(
                title = "仅已加满",
                isSelected = selectedFilter == HistoryFilter.ONLY_FULL,
                onClick = { selectedFilter = HistoryFilter.ONLY_FULL }
            )
            FilterPill(
                title = "未加满",
                isSelected = selectedFilter == HistoryFilter.ONLY_PARTIAL,
                onClick = { selectedFilter = HistoryFilter.ONLY_PARTIAL }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "没有符合条件的记录",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
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
private fun FilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) Color(0xFF10B981)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
