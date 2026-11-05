package com.fueltracker.app.model

import kotlinx.serialization.Serializable

@Serializable
data class FuelRecord(
    val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long,
    val odometer: Double,
    val fuelAmount: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val fuelType: String,
    val isFull: Boolean = true,
    val isMissedPrevious: Boolean = false,
    val calculatedConsumption: Double? = null, // L/100km
    val distanceTraveled: Double? = null,      // km since previous record
    val stationName: String? = null,
    val note: String? = null
)
