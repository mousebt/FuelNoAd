package com.fueltracker.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: Long = 0,
    val name: String,
    val plateNumber: String = "",
    val fuelType: String = "95#",
    val tankCapacity: Float = 50f,
    val initialMileage: Double = 0.0,
    val isDefault: Boolean = false
)
