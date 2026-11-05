package com.fueltracker.app

import android.app.Application
import com.fueltracker.app.data.local.FuelDatabase
import com.fueltracker.app.data.repository.FuelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class FuelApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { FuelDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { FuelRepository(database.vehicleDao(), database.fuelRecordDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
