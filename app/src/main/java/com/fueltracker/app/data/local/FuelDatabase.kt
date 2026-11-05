package com.fueltracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fueltracker.app.data.local.dao.FuelRecordDao
import com.fueltracker.app.data.local.dao.VehicleDao
import com.fueltracker.app.data.local.entity.FuelRecordEntity
import com.fueltracker.app.data.local.entity.VehicleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VehicleEntity::class, FuelRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FuelDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelRecordDao(): FuelRecordDao

    companion object {
        @Volatile
        private var INSTANCE: FuelDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FuelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelDatabase::class.java,
                    "fuel_tracker.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.vehicleDao(), database.fuelRecordDao())
                    }
                }
            }

            private suspend fun populateInitialData(vehicleDao: VehicleDao, fuelRecordDao: FuelRecordDao) {
                // Seed default vehicle
                val vehicleId = vehicleDao.insertVehicle(
                    VehicleEntity(
                        name = "家用轿车",
                        plateNumber = "粤B·88888",
                        fuelType = "95#",
                        tankCapacity = 55f,
                        initialMileage = 10000.0,
                        isDefault = true
                    )
                )

                val now = System.currentTimeMillis()
                val dayMillis = 86400000L

                // Seed initial records for testing
                val initialRecords = listOf(
                    FuelRecordEntity(
                        id = 0,
                        vehicleId = vehicleId,
                        timestamp = now - 25 * dayMillis,
                        odometer = 10200.0,
                        fuelAmount = 45.0,
                        unitPrice = 8.20,
                        totalPrice = 369.0,
                        fuelType = "95#",
                        isFull = true,
                        isMissedPrevious = false,
                        calculatedConsumption = null,
                        distanceTraveled = null,
                        stationName = "中石化科技园站",
                        note = "新车首次加满作为基准"
                    ),
                    FuelRecordEntity(
                        id = 0,
                        vehicleId = vehicleId,
                        timestamp = now - 18 * dayMillis,
                        odometer = 10760.0,
                        fuelAmount = 42.5,
                        unitPrice = 8.15,
                        totalPrice = 346.38,
                        fuelType = "95#",
                        isFull = true,
                        isMissedPrevious = false,
                        calculatedConsumption = 7.59,
                        distanceTraveled = 560.0,
                        stationName = "中石油高新站",
                        note = "市区通勤加满"
                    ),
                    FuelRecordEntity(
                        id = 0,
                        vehicleId = vehicleId,
                        timestamp = now - 10 * dayMillis,
                        odometer = 11330.0,
                        fuelAmount = 41.0,
                        unitPrice = 8.08,
                        totalPrice = 331.28,
                        fuelType = "95#",
                        isFull = true,
                        isMissedPrevious = false,
                        calculatedConsumption = 7.19,
                        distanceTraveled = 570.0,
                        stationName = "中石化科技园站",
                        note = "周末短途自驾"
                    ),
                    FuelRecordEntity(
                        id = 0,
                        vehicleId = vehicleId,
                        timestamp = now - 4 * dayMillis,
                        odometer = 11800.0,
                        fuelAmount = 30.0,
                        unitPrice = 8.10,
                        totalPrice = 243.0,
                        fuelType = "95#",
                        isFull = false,
                        isMissedPrevious = false,
                        calculatedConsumption = null,
                        distanceTraveled = 470.0,
                        stationName = "壳牌加油站",
                        note = "赶时间未加满"
                    ),
                    FuelRecordEntity(
                        id = 0,
                        vehicleId = vehicleId,
                        timestamp = now - 1 * dayMillis,
                        odometer = 12280.0,
                        fuelAmount = 36.8,
                        unitPrice = 8.15,
                        totalPrice = 299.92,
                        fuelType = "95#",
                        isFull = true,
                        isMissedPrevious = false,
                        calculatedConsumption = 7.03, // (30.0 + 36.8) / (12280 - 11330) * 100 = 66.8 / 950 * 100 = 7.03
                        distanceTraveled = 480.0,
                        stationName = "中石化科技园站",
                        note = "再次加满，自动统算上一笔未加满油耗"
                    )
                )

                fuelRecordDao.insertRecords(initialRecords)
            }
        }
    }
}
