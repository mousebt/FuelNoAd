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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

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
            viewModel.importBackup(context, uri) { success, msg ->
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
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("从 CSV / JSON 文件导入数据", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = "我的车库 (${vehicles.size} 辆)",
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = v.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(Color(0xFF10B981))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
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
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${v.plateNumber.ifBlank { "无车牌" }} · ${v.fuelType} · 油箱 ${v.tankCapacity.toInt()}L · 初始 ${v.initialMileage.toLong()}km",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { vehicleToEdit = v },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "修改车辆",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { vehicleToDelete = v },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除车辆",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // About & Version Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "关于应用与版本",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "应用名称",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "油耗管家 (FuelNoAd)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "当前软件版本",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "v${com.fueltracker.app.BuildConfig.VERSION_NAME} (Build ${com.fueltracker.app.BuildConfig.VERSION_CODE})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF059669)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "架构与隐私",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "纯本地离线 · SAF 免权限 · 零广告",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        VehicleFormDialog(
            title = "添加新车辆",
            initialName = "",
            initialPlate = "",
            initialFuelType = "95#",
            initialCapacity = 50f,
            initialMileage = 0.0,
            confirmButtonText = "确定添加",
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { name, plate, type, capacity, initialMileage ->
                viewModel.addVehicle(name, plate, type, capacity, initialMileage)
                showAddVehicleDialog = false
            }
        )
    }

    // Edit Vehicle Dialog
    if (vehicleToEdit != null) {
        val target = vehicleToEdit!!
        VehicleFormDialog(
            title = "修改车辆信息",
            initialName = target.name,
            initialPlate = target.plateNumber,
            initialFuelType = target.fuelType,
            initialCapacity = target.tankCapacity,
            initialMileage = target.initialMileage,
            confirmButtonText = "保存修改",
            onDismiss = { vehicleToEdit = null },
            onConfirm = { name, plate, type, capacity, initialMileage ->
                val updated = target.copy(
                    name = name,
                    plateNumber = plate,
                    fuelType = type,
                    tankCapacity = capacity,
                    initialMileage = initialMileage
                )
                viewModel.updateVehicle(updated)
                vehicleToEdit = null
                Toast.makeText(context, "车辆信息修改成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Vehicle Confirmation Dialog
    if (vehicleToDelete != null) {
        val target = vehicleToDelete!!
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = {
                Text("删除车辆", fontWeight = FontWeight.Bold)
            },
            text = {
                if (vehicles.size <= 1) {
                    Text("当前仅剩一辆车辆，无法删除！如需更换车辆，请先添加新车辆后再执行删除。")
                } else {
                    Text("确定要删除车辆【${target.name}】（${target.plateNumber.ifBlank { "无车牌" }}）吗？\n\n⚠️ 警告：该操作将同时清空该车辆下的所有历史加油记录，且不可撤销！")
                }
            },
            confirmButton = {
                if (vehicles.size > 1) {
                    Button(
                        onClick = {
                            viewModel.deleteVehicle(target)
                            vehicleToDelete = null
                            Toast.makeText(context, "车辆已删除", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确定删除", color = Color.White)
                    }
                } else {
                    Button(onClick = { vehicleToDelete = null }) {
                        Text("知道了")
                    }
                }
            },
            dismissButton = {
                if (vehicles.size > 1) {
                    TextButton(onClick = { vehicleToDelete = null }) {
                        Text("取消")
                    }
                }
            }
        )
    }
}

@Composable
private fun VehicleFormDialog(
    title: String,
    initialName: String,
    initialPlate: String,
    initialFuelType: String,
    initialCapacity: Float,
    initialMileage: Double,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, plate: String, fuelType: String, capacity: Float, initialMileage: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var plate by remember { mutableStateOf(initialPlate) }
    var fuelType by remember { mutableStateOf(initialFuelType) }
    var capacityText by remember { mutableStateOf(initialCapacity.toInt().toString()) }
    var initialMileageText by remember { mutableStateOf(initialMileage.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("车辆名称 (如: 雅阁 260)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("车牌号 (选填)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text("燃油标号 (如 92# / 95#)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = { Text("油箱容积 (L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialMileageText,
                    onValueChange = { initialMileageText = it },
                    label = { Text("初始里程数 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            plate.trim(),
                            fuelType.trim(),
                            capacityText.toFloatOrNull() ?: 50f,
                            initialMileageText.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
