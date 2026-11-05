package com.fueltracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fueltracker.app.model.FuelRecord

@Entity(
    tableName = "fuel_records",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"])]
)
data class FuelRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long,
    val odometer: Double,
    val fuelAmount: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val fuelType: String,
    val isFull: Boolean,
    val isMissedPrevious: Boolean,
    val calculatedConsumption: Double?,
    val distanceTraveled: Double?,
    val stationName: String?,
    val note: String?
) {
    fun toModel(): FuelRecord = FuelRecord(
        id = id,
        vehicleId = vehicleId,
        timestamp = timestamp,
        odometer = odometer,
        fuelAmount = fuelAmount,
        unitPrice = unitPrice,
        totalPrice = totalPrice,
        fuelType = fuelType,
        isFull = isFull,
        isMissedPrevious = isMissedPrevious,
        calculatedConsumption = calculatedConsumption,
        distanceTraveled = distanceTraveled,
        stationName = stationName,
        note = note
    )

    companion object {
        fun fromModel(model: FuelRecord): FuelRecordEntity = FuelRecordEntity(
            id = model.id,
            vehicleId = model.vehicleId,
            timestamp = model.timestamp,
            odometer = model.odometer,
            fuelAmount = model.fuelAmount,
            unitPrice = model.unitPrice,
            totalPrice = model.totalPrice,
            fuelType = model.fuelType,
            isFull = model.isFull,
            isMissedPrevious = model.isMissedPrevious,
            calculatedConsumption = model.calculatedConsumption,
            distanceTraveled = model.distanceTraveled,
            stationName = model.stationName,
            note = model.note
        )
    }
}
