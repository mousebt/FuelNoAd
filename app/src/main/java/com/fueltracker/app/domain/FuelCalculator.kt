package com.fueltracker.app.domain

import com.fueltracker.app.model.DashboardStats
import com.fueltracker.app.model.FuelRecord
import kotlin.math.round

object FuelCalculator {

    /**
     * Recalculates fuel consumption and distance traveled for a list of records.
     * Records MUST be sorted in ascending order (by odometer or timestamp).
     */
    fun recalculateRecords(records: List<FuelRecord>): List<FuelRecord> {
        if (records.isEmpty()) return emptyList()

        val updated = mutableListOf<FuelRecord>()
        var lastFullIndex = -1
        var pendingFuelAmount = 0.0

        for (i in records.indices) {
            val curr = records[i]

            // First record or user flagged missed previous record -> reset baseline
            if (i == 0 || curr.isMissedPrevious) {
                if (curr.isFull) {
                    lastFullIndex = i
                } else {
                    lastFullIndex = -1
                }
                pendingFuelAmount = 0.0

                updated.add(
                    curr.copy(
                        calculatedConsumption = null,
                        distanceTraveled = null
                    )
                )
                continue
            }

            val prev = records[i - 1]
            val deltaKm = if (curr.odometer > prev.odometer) curr.odometer - prev.odometer else null

            if (curr.isFull) {
                val consumption = if (lastFullIndex != -1) {
                    val anchorRecord = records[lastFullIndex]
                    val totalDistance = curr.odometer - anchorRecord.odometer
                    val totalFuel = pendingFuelAmount + curr.fuelAmount

                    if (totalDistance > 0) {
                        roundToTwoDecimals((totalFuel / totalDistance) * 100.0)
                    } else {
                        null
                    }
                } else {
                    null
                }

                lastFullIndex = i
                pendingFuelAmount = 0.0

                updated.add(
                    curr.copy(
                        calculatedConsumption = consumption,
                        distanceTraveled = deltaKm
                    )
                )
            } else {
                // Partial fill-up: accumulate fuel volume and wait for the next full fill-up
                pendingFuelAmount += curr.fuelAmount
                updated.add(
                    curr.copy(
                        calculatedConsumption = null,
                        distanceTraveled = deltaKm
                    )
                )
            }
        }

        return updated
    }

    /**
     * Aggregates stats for the dashboard summary
     */
    fun calculateDashboardStats(records: List<FuelRecord>): DashboardStats {
        if (records.isEmpty()) return DashboardStats()

        val validConsumptions = records.mapNotNull { it.calculatedConsumption }
        val avgConsumption = if (validConsumptions.isNotEmpty()) {
            roundToTwoDecimals(validConsumptions.average())
        } else {
            null
        }

        val latestConsumption = records.reversed().firstOrNull { it.calculatedConsumption != null }?.calculatedConsumption

        val totalExpense = records.sumOf { it.totalPrice }
        val totalFuel = records.sumOf { it.fuelAmount }

        val firstOdo = records.first().odometer
        val lastOdo = records.last().odometer
        val totalDistance = if (lastOdo > firstOdo) lastOdo - firstOdo else 0.0

        val costPerKm = if (totalDistance > 0) {
            roundToTwoDecimals(totalExpense / totalDistance)
        } else {
            null
        }

        return DashboardStats(
            avgConsumption = avgConsumption,
            latestConsumption = latestConsumption,
            totalExpense = totalExpense,
            totalFuelAmount = totalFuel,
            totalDistance = totalDistance,
            costPerKm = costPerKm,
            recordCount = records.size
        )
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return round(value * 100.0) / 100.0
    }
}
