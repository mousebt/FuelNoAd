package com.fueltracker.app.domain

import com.fueltracker.app.model.FuelRecord
import org.junit.Assert.*
import org.junit.Test

class FuelCalculatorTest {

    @Test
    fun testFirstRecordHasNoConsumption() {
        val records = listOf(
            createRecord(id = 1, odo = 10000.0, vol = 40.0, isFull = true)
        )
        val result = FuelCalculator.recalculateRecords(records)

        assertEquals(1, result.size)
        assertNull(result[0].calculatedConsumption)
        assertNull(result[0].distanceTraveled)
    }

    @Test
    fun testStandardFullToFullCalculation() {
        val records = listOf(
            createRecord(id = 1, odo = 10000.0, vol = 40.0, isFull = true),
            // Travel 500 km, add 35 Liters
            createRecord(id = 2, odo = 10500.0, vol = 35.0, isFull = true)
        )
        val result = FuelCalculator.recalculateRecords(records)

        assertEquals(2, result.size)
        assertNull(result[0].calculatedConsumption)

        // 35 L / 500 km * 100 = 7.00 L/100km
        assertEquals(7.0, result[1].calculatedConsumption!!, 0.01)
        assertEquals(500.0, result[1].distanceTraveled!!, 0.01)
    }

    @Test
    fun testPartialFillUpAccumulatesUntilNextFull() {
        val records = listOf(
            createRecord(id = 1, odo = 10000.0, vol = 40.0, isFull = true),
            // Partial fill-up (+200km, 15L, not full)
            createRecord(id = 2, odo = 10200.0, vol = 15.0, isFull = false),
            // Full fill-up (+300km, 20L, full)
            createRecord(id = 3, odo = 10500.0, vol = 20.0, isFull = true)
        )
        val result = FuelCalculator.recalculateRecords(records)

        assertEquals(3, result.size)
        assertNull(result[0].calculatedConsumption)
        assertNull(result[1].calculatedConsumption) // partial fill-up has no consumption yet

        // Total distance: 10500 - 10000 = 500km
        // Total fuel: 15L + 20L = 35L
        // 35L / 500km * 100 = 7.00 L/100km
        assertEquals(7.0, result[2].calculatedConsumption!!, 0.01)
        assertEquals(300.0, result[2].distanceTraveled!!, 0.01)
    }

    @Test
    fun testMissedRecordResetsBaseline() {
        val records = listOf(
            createRecord(id = 1, odo = 10000.0, vol = 40.0, isFull = true),
            createRecord(id = 2, odo = 10500.0, vol = 35.0, isFull = true),
            // Missed a previous record!
            createRecord(id = 3, odo = 12000.0, vol = 40.0, isFull = true, isMissed = true),
            createRecord(id = 4, odo = 12500.0, vol = 36.0, isFull = true)
        )
        val result = FuelCalculator.recalculateRecords(records)

        // Record 2 is valid
        assertEquals(7.0, result[1].calculatedConsumption!!, 0.01)

        // Record 3 was marked missed, so its consumption cannot be accurately calculated
        assertNull(result[2].calculatedConsumption)

        // Record 4 uses Record 3 as the new baseline: 36L / 500km * 100 = 7.2 L/100km
        assertEquals(7.2, result[3].calculatedConsumption!!, 0.01)
    }

    @Test
    fun testDashboardStatsAggregation() {
        val records = listOf(
            createRecord(id = 1, odo = 10000.0, vol = 40.0, unitPrice = 8.0, isFull = true),
            createRecord(id = 2, odo = 10500.0, vol = 35.0, unitPrice = 8.0, isFull = true),
            createRecord(id = 3, odo = 11000.0, vol = 37.0, unitPrice = 8.0, isFull = true)
        )
        val calculatedRecords = FuelCalculator.recalculateRecords(records)
        val stats = FuelCalculator.calculateDashboardStats(calculatedRecords)

        // Record 2 consumption: 7.0 L/100km
        // Record 3 consumption: 7.4 L/100km
        // Average: 7.2 L/100km
        assertEquals(7.2, stats.avgConsumption!!, 0.01)
        assertEquals(7.4, stats.latestConsumption!!, 0.01)
        assertEquals(1000.0, stats.totalDistance, 0.01)
        assertEquals(112.0, stats.totalFuelAmount, 0.01)
        assertEquals(896.0, stats.totalExpense, 0.01)
    }

    private fun createRecord(
        id: Long,
        odo: Double,
        vol: Double,
        unitPrice: Double = 8.0,
        isFull: Boolean = true,
        isMissed: Boolean = false
    ): FuelRecord {
        return FuelRecord(
            id = id,
            vehicleId = 1L,
            timestamp = 1000L * id,
            odometer = odo,
            fuelAmount = vol,
            unitPrice = unitPrice,
            totalPrice = vol * unitPrice,
            fuelType = "95#",
            isFull = isFull,
            isMissedPrevious = isMissed
        )
    }
}
