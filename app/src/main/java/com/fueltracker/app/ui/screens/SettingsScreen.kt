package com.fueltracker.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.model.Vehicle
import com.fueltracker.app.ui.viewmodel.FuelViewModel

@Composable
fun SettingsScreen(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddVehicleDialog by remember { mutableStateOf(false) }

    // SAF Launchers for Export and Import
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(context, uri, isJson = true) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(context, uri, isJson = false) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(context, uri, isJson = true) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "车辆与本地数据",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Local Storage Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💾", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "纯本地离线存储",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "所有数据均保存在本机 Room 数据库中，不上传任何云端服务器",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportJsonLauncher.launch("fuel_backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("导出 JSON", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { exportCsvLauncher.launch("fuel_records_${System.currentTimeMillis()}.csv") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("导出 CSV", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("从 JSON 备份文件恢复数据", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Vehicles List Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的车库",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = { showAddVehicleDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加车辆", fontSize = 12.sp, color = Color(0xFF059669))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                vehicles.forEach { v ->
                    val isSelected = v.id == selectedVehicleId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF10B981).copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectVehicle(v.id) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = v.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${v.plateNumber.ifBlank { "无车牌" }} · ${v.fuelType} · 油箱 ${v.tankCapacity.toInt()}L",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF10B981))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "使用中",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { name, plate, type, capacity, initialMileage ->
                viewModel.addVehicle(name, plate, type, capacity, initialMileage)
                showAddVehicleDialog = false
            }
        )
    }
}

@Composable
private fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, plate: String, fuelType: String, capacity: Float, initialMileage: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("95#") }
    var capacityText by remember { mutableStateOf("50") }
    var initialMileageText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新车辆", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("车辆名称 (如: 雅阁 260)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("车牌号 (选填)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text("燃油标号 (如 92# / 95#)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = { Text("油箱容积 (L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = initialMileageText,
                    onValueChange = { initialMileageText = it },
                    label = { Text("初始里程数 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            plate,
                            fuelType,
                            capacityText.toFloatOrNull() ?: 50f,
                            initialMileageText.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("确定添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
