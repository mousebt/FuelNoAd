package com.fueltracker.app.data.local.dao

import androidx.room.*
import com.fueltracker.app.data.local.entity.FuelRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelRecordDao {

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY odometer ASC, timestamp ASC")
    fun getRecordsByVehicle(vehicleId: Long): Flow<List<FuelRecordEntity>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY odometer ASC, timestamp ASC")
    suspend fun getRecordsListByVehicle(vehicleId: Long): List<FuelRecordEntity>

    @Query("SELECT * FROM fuel_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): FuelRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FuelRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<FuelRecordEntity>)

    @Update
    suspend fun updateRecord(record: FuelRecordEntity)

    @Update
    suspend fun updateRecords(records: List<FuelRecordEntity>)

    @Delete
    suspend fun deleteRecord(record: FuelRecordEntity)

    @Query("DELETE FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun deleteRecordsByVehicle(vehicleId: Long)

    @Query("DELETE FROM fuel_records")
    suspend fun clearAll()
}
