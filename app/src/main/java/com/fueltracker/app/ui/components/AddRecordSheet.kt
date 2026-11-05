package com.fueltracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.FuelRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordSheet(
    lastRecord: FuelRecord?,
    onDismiss: () -> Unit,
    onSave: (
        odometer: Double,
        fuelAmount: Double,
        unitPrice: Double,
        totalPrice: Double,
        fuelType: String,
        isFull: Boolean,
        isMissedPrevious: Boolean,
        stationName: String?,
        note: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lastOdo = lastRecord?.odometer ?: 10000.0
    val suggestedOdo = (lastOdo + 450.0).toLong().toString()

    var odometerText by remember { mutableStateOf(suggestedOdo) }
    var volumeText by remember { mutableStateOf("42.0") }
    var unitPriceText by remember { mutableStateOf("8.15") }
    var totalPriceText by remember { mutableStateOf("342.30") }

    var selectedFuelType by remember { mutableStateOf("95#") }
    var isFull by remember { mutableStateOf(true) }
    var isMissedPrevious by remember { mutableStateOf(false) }

    var stationName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-update total price when volume or unit price changes
    fun recalculateTotal() {
        val vol = volumeText.toDoubleOrNull() ?: 0.0
        val price = unitPriceText.toDoubleOrNull() ?: 0.0
        totalPriceText = String.format("%.2f", vol * price)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "记一笔加油",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Odometer input
            OutlinedTextField(
                value = odometerText,
                onValueChange = {
                    odometerText = it
                    errorMessage = null
                },
                label = { Text("当前仪表盘总里程 (km)") },
                trailingIcon = { Text("km", modifier = Modifier.padding(end = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(
                        text = "上次记录: ${lastOdo.toLong()} km",
                        color = Color(0xFF059669)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Volume & Unit price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = volumeText,
                    onValueChange = {
                        volumeText = it
                        recalculateTotal()
                    },
                    label = { Text("加油量 (升)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = {
                        unitPriceText = it
                        recalculateTotal()
                    },
                    label = { Text("单价 (元/升)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total price input
            OutlinedTextField(
                value = totalPriceText,
                onValueChange = { totalPriceText = it },
                label = { Text("实付总金额 (元)") },
                prefix = { Text("¥ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Fuel Type Selection Chips
            Text(
                text = "燃油标号",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val fuelTypes = listOf("92#", "95#", "98#", "柴油")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fuelTypes.forEach { type ->
                    val isSelected = selectedFuelType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedFuelType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options container
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "是否加满油箱", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "用于推算真实百公里油耗",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = isFull,
                            onCheckedChange = { isFull = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "漏记了上一笔加油", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "本次加满将重置为新的计算基点",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Checkbox(
                            checked = isMissedPrevious,
                            onCheckedChange = { isMissedPrevious = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFF59E0B)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gas Station & Notes
            OutlinedTextField(
                value = stationName,
                onValueChange = { stationName = it },
                label = { Text("加油站名称 (选填)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注 (选填)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Error display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val odo = odometerText.toDoubleOrNull()
                    val vol = volumeText.toDoubleOrNull()
                    val price = unitPriceText.toDoubleOrNull()
                    val total = totalPriceText.toDoubleOrNull()

                    if (odo == null || odo <= 0) {
                        errorMessage = "请输入有效的仪表盘里程"
                        return@Button
                    }
                    if (odo <= lastOdo) {
                        errorMessage = "当前里程 (${odo.toLong()}km) 必须大于上一笔里程 (${lastOdo.toLong()}km)"
                        return@Button
                    }
                    if (vol == null || vol <= 0) {
                        errorMessage = "请输入有效的加油升数"
                        return@Button
                    }
                    if (price == null || price <= 0) {
                        errorMessage = "请输入有效的单价"
                        return@Button
                    }
                    val finalTotal = total ?: (vol * price)

                    onSave(odo, vol, price, finalTotal, selectedFuelType, isFull, isMissedPrevious, stationName, note)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                Text(
                    text = "保存记录",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
