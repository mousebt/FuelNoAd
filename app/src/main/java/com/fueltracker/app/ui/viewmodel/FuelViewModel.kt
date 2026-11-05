package com.fueltracker.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.data.backup.DataBackupManager
import com.fueltracker.app.data.repository.FuelRepository
import com.fueltracker.app.domain.FuelCalculator
import com.fueltracker.app.model.DashboardStats
import com.fueltracker.app.model.FuelRecord
import com.fueltracker.app.model.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FuelViewModel(
    private val repository: FuelRepository
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedVehicleId = MutableStateFlow<Long?>(null)
    val selectedVehicleId: StateFlow<Long?> = _selectedVehicleId.asStateFlow()

    init {
        // Automatically select the default or first vehicle
        viewModelScope.launch {
            vehicles.collect { list ->
                if (_selectedVehicleId.value == null && list.isNotEmpty()) {
                    val defaultVehicle = list.find { it.isDefault } ?: list.first()
                    _selectedVehicleId.value = defaultVehicle.id
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentRecords: StateFlow<List<FuelRecord>> = _selectedVehicleId
        .filterNotNull()
        .flatMapLatest { vehicleId ->
            repository.getRecords(vehicleId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = currentRecords
        .map { records ->
            FuelCalculator.calculateDashboardStats(records)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun selectVehicle(vehicleId: Long) {
        _selectedVehicleId.value = vehicleId
    }

    fun saveRecord(
        odometer: Double,
        fuelAmount: Double,
        unitPrice: Double,
        totalPrice: Double,
        fuelType: String,
        isFull: Boolean,
        isMissedPrevious: Boolean,
        stationName: String? = null,
        note: String? = null
    ) {
        val vehicleId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            val record = FuelRecord(
                vehicleId = vehicleId,
                timestamp = System.currentTimeMillis(),
                odometer = odometer,
                fuelAmount = fuelAmount,
                unitPrice = unitPrice,
                totalPrice = totalPrice,
                fuelType = fuelType,
                isFull = isFull,
                isMissedPrevious = isMissedPrevious,
                stationName = stationName?.ifBlank { null },
                note = note?.ifBlank { null }
            )
            repository.saveRecordAndRecalculate(record)
        }
    }

    fun deleteRecord(record: FuelRecord) {
        viewModelScope.launch {
            repository.deleteRecordAndRecalculate(record)
        }
    }

    fun addVehicle(name: String, plateNumber: String, fuelType: String, tankCapacity: Float, initialMileage: Double) {
        viewModelScope.launch {
            val isFirst = vehicles.value.isEmpty()
            val newId = repository.insertVehicle(
                Vehicle(
                    name = name,
                    plateNumber = plateNumber,
                    fuelType = fuelType,
                    tankCapacity = tankCapacity,
                    initialMileage = initialMileage,
                    isDefault = isFirst
                )
            )
            _selectedVehicleId.value = newId
        }
    }

    fun exportBackup(context: Context, uri: Uri, isJson: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val records = currentRecords.value
                val content = if (isJson) {
                    DataBackupManager.exportToJson(records)
                } else {
                    DataBackupManager.exportToCsv(records)
                }
                val result = DataBackupManager.writeToUri(context, uri, content)
                if (result.isSuccess) {
                    onResult(true, "导出成功！")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "导出失败")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "未知错误")
            }
        }
    }

    fun importBackup(context: Context, uri: Uri, isJson: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val readResult = DataBackupManager.readFromUri(context, uri)
                if (readResult.isFailure) {
                    onResult(false, readResult.exceptionOrNull()?.message ?: "读取文件失败")
                    return@launch
                }
                val content = readResult.getOrThrow()
                if (isJson) {
                    val parsed = DataBackupManager.importFromJson(content)
                    repository.restoreAllRecords(parsed)
                    onResult(true, "成功恢复 ${parsed.size} 条记录！")
                } else {
                    onResult(false, "暂仅支持从 JSON 完整备份文件直接恢复数据")
                }
            } catch (e: Exception) {
                onResult(false, "解析备份文件失败: ${e.localizedMessage}")
            }
        }
    }
}
