package com.fueltracker.app.data.repository

import com.fueltracker.app.data.local.dao.FuelRecordDao
import com.fueltracker.app.data.local.dao.VehicleDao
import com.fueltracker.app.data.local.entity.FuelRecordEntity
import com.fueltracker.app.data.local.entity.VehicleEntity
import com.fueltracker.app.domain.FuelCalculator
import com.fueltracker.app.model.FuelRecord
import com.fueltracker.app.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FuelRepository(
    private val vehicleDao: VehicleDao,
    private val fuelRecordDao: FuelRecordDao
) {
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles().map { list ->
        list.map { it.toModel() }
    }

    fun getRecords(vehicleId: Long): Flow<List<FuelRecord>> =
        fuelRecordDao.getRecordsByVehicle(vehicleId).map { list ->
            list.map { it.toModel() }
        }

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        return vehicleDao.insertVehicle(VehicleEntity.fromModel(vehicle))
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        vehicleDao.updateVehicle(VehicleEntity.fromModel(vehicle))
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        vehicleDao.deleteVehicle(VehicleEntity.fromModel(vehicle))
    }

    suspend fun setDefaultVehicle(vehicleId: Long) {
        vehicleDao.setDefaultVehicle(vehicleId)
    }

    /**
     * Inserts or updates a fuel record, and automatically recalculates
     * the consumption series for that vehicle so cached metrics stay correct.
     */
    suspend fun saveRecordAndRecalculate(record: FuelRecord) {
        val existing = fuelRecordDao.getRecordsListByVehicle(record.vehicleId).map { it.toModel() }.toMutableList()
        val index = existing.indexOfFirst { it.id == record.id && record.id != 0L }

        if (index != -1) {
            existing[index] = record
        } else {
            existing.add(record)
        }

        // Sort by odometer
        val sorted = existing.sortedWith(compareBy({ it.odometer }, { it.timestamp }))
        val recalculated = FuelCalculator.recalculateRecords(sorted)

        fuelRecordDao.insertRecords(recalculated.map { FuelRecordEntity.fromModel(it) })
    }

    suspend fun deleteRecordAndRecalculate(record: FuelRecord) {
        fuelRecordDao.deleteRecord(FuelRecordEntity.fromModel(record))

        // Recalculate remaining
        val remaining = fuelRecordDao.getRecordsListByVehicle(record.vehicleId).map { it.toModel() }
        val sorted = remaining.sortedWith(compareBy({ it.odometer }, { it.timestamp }))
        val recalculated = FuelCalculator.recalculateRecords(sorted)

        fuelRecordDao.insertRecords(recalculated.map { FuelRecordEntity.fromModel(it) })
    }

    suspend fun getAllRecordsForVehicleSync(vehicleId: Long): List<FuelRecord> {
        return fuelRecordDao.getRecordsListByVehicle(vehicleId).map { it.toModel() }
    }

    suspend fun restoreAllRecords(records: List<FuelRecord>) {
        val sorted = records.sortedWith(compareBy({ it.odometer }, { it.timestamp }))
        val recalculated = FuelCalculator.recalculateRecords(sorted)
        fuelRecordDao.insertRecords(recalculated.map { FuelRecordEntity.fromModel(it) })
    }
}
