package com.fueltracker.app.data.local.dao

import androidx.room.*
import com.fueltracker.app.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultVehicle(): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearDefaultVehicles()

    @Transaction
    suspend fun setDefaultVehicle(vehicleId: Long) {
        clearDefaultVehicles()
        setVehicleAsDefault(vehicleId)
    }

    @Query("UPDATE vehicles SET isDefault = 1 WHERE id = :vehicleId")
    suspend fun setVehicleAsDefault(vehicleId: Long)
}
