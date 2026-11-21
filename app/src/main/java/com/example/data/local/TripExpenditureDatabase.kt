package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppSettingsEntity::class,
        TripEntity::class,
        TravelerEntity::class,
        DepositEntity::class,
        ItineraryStopEntity::class,
        ExpenseEntity::class,
        TravelDocumentEntity::class,
        CustomMapPinEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TripExpenditureDatabase : RoomDatabase() {
    abstract fun dao(): TripExpenditureDao

    companion object {
        @Volatile
        private var INSTANCE: TripExpenditureDatabase? = null

        fun getInstance(context: Context): TripExpenditureDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TripExpenditureDatabase::class.java,
                    "trip_expenditure_user_v2.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
