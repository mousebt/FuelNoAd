package com.fueltracker.app.model

data class DashboardStats(
    val avgConsumption: Double? = null,      // Average L/100km
    val latestConsumption: Double? = null,   // Latest L/100km
    val totalExpense: Double = 0.0,          // Total ¥ spent
    val totalFuelAmount: Double = 0.0,       // Total Liters
    val totalDistance: Double = 0.0,         // Total km tracked
    val costPerKm: Double? = null,           // ¥ / km
    val recordCount: Int = 0
)
