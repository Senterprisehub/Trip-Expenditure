package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripExpenditureDao {

    // Persistent Application Settings (Survives Reboot)
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppSettings(settings: AppSettingsEntity)

    // Trips
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripRecord(tripId: Long)

    @Query("DELETE FROM travelers WHERE tripId = :tripId")
    suspend fun deleteTravelersForTrip(tripId: Long)

    @Query("DELETE FROM deposits WHERE tripId = :tripId")
    suspend fun deleteDepositsForTrip(tripId: Long)

    @Query("DELETE FROM itinerary_stops WHERE tripId = :tripId")
    suspend fun deleteStopsForTrip(tripId: Long)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteExpensesForTrip(tripId: Long)

    @Query("DELETE FROM travel_documents WHERE tripId = :tripId")
    suspend fun deleteDocumentsForTrip(tripId: Long)

    @Query("DELETE FROM custom_map_pins WHERE tripId = :tripId")
    suspend fun deleteCustomPinsForTrip(tripId: Long)

    @Transaction
    suspend fun deleteTripAndAllData(tripId: Long) {
        deleteTravelersForTrip(tripId)
        deleteDepositsForTrip(tripId)
        deleteStopsForTrip(tripId)
        deleteExpensesForTrip(tripId)
        deleteDocumentsForTrip(tripId)
        deleteCustomPinsForTrip(tripId)
        deleteTripRecord(tripId)
    }

    // Travelers Details (Add & Delete)
    @Query("SELECT * FROM travelers WHERE tripId = :tripId ORDER BY createdAt ASC")
    fun observeTravelersForTrip(tripId: Long): Flow<List<TravelerEntity>>

    @Query("SELECT * FROM travelers WHERE tripId = :tripId ORDER BY createdAt ASC")
    suspend fun getTravelersForTripOnce(tripId: Long): List<TravelerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraveler(traveler: TravelerEntity): Long

    @Query("DELETE FROM travelers WHERE id = :travelerId")
    suspend fun deleteTravelerById(travelerId: Long)

    // Deposits by Traveler Name
    @Query("SELECT * FROM deposits WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeDepositsForTrip(tripId: Long): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    fun observeAllDeposits(): Flow<List<DepositEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: DepositEntity): Long

    @Query("DELETE FROM deposits WHERE id = :depositId")
    suspend fun deleteDepositById(depositId: Long)

    // Itinerary Stops
    @Query("SELECT * FROM itinerary_stops WHERE tripId = :tripId ORDER BY dayNumber ASC, startTime ASC")
    fun observeStopsForTrip(tripId: Long): Flow<List<ItineraryStopEntity>>

    @Query("SELECT * FROM itinerary_stops")
    fun observeAllStops(): Flow<List<ItineraryStopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: ItineraryStopEntity): Long

    @Update
    suspend fun updateStop(stop: ItineraryStopEntity)

    @Query("DELETE FROM itinerary_stops WHERE id = :stopId")
    suspend fun deleteStopById(stopId: Long)

    // Expenses
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeExpensesForTrip(tripId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    // Travel Documents
    @Query("SELECT * FROM travel_documents WHERE tripId = :tripId ORDER BY id ASC")
    fun observeDocumentsForTrip(tripId: Long): Flow<List<TravelDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: TravelDocumentEntity): Long

    @Query("DELETE FROM travel_documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: Long)

    // Custom Offline Map Pins
    @Query("SELECT * FROM custom_map_pins WHERE tripId = :tripId ORDER BY createdAt ASC")
    fun observeCustomPinsForTrip(tripId: Long): Flow<List<CustomMapPinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPin(pin: CustomMapPinEntity): Long

    @Query("DELETE FROM custom_map_pins WHERE id = :pinId")
    suspend fun deleteCustomPinById(pinId: Long)
}
