package com.fueltracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fueltracker.app.model.Vehicle

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val plateNumber: String,
    val fuelType: String,
    val tankCapacity: Float,
    val initialMileage: Double,
    val isDefault: Boolean
) {
    fun toModel(): Vehicle = Vehicle(
        id = id,
        name = name,
        plateNumber = plateNumber,
        fuelType = fuelType,
        tankCapacity = tankCapacity,
        initialMileage = initialMileage,
        isDefault = isDefault
    )

    companion object {
        fun fromModel(model: Vehicle): VehicleEntity = VehicleEntity(
            id = model.id,
            name = model.name,
            plateNumber = model.plateNumber,
            fuelType = model.fuelType,
            tankCapacity = model.tankCapacity,
            initialMileage = model.initialMileage,
            isDefault = model.isDefault
        )
    }
}
