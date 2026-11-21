package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TripExpenditureDatabase
import com.example.data.repository.TripRepository
import com.example.domain.ExpenseCalculator
import com.example.domain.TripExportDataBundle
import com.example.domain.TripReportExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: TripExpenditureDatabase
    private lateinit var repository: TripRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripExpenditureDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TripRepository(db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Trip Expenditure", appName)
    }

    @Test
    fun `database starts with zero pre-filled data and supports trip delete traveler add-delete deposits and PDF CSV export`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify zero pre-filled data
        val initialTrips = repository.allTrips.first()
        assertTrue("Expected zero pre-filled trips on startup", initialTrips.isEmpty())

        // 2. Verify permanent settings persistence
        repository.saveAppSettings(appCurrency = "EUR", themeId = "MINIMALIST_NORDIC")
        val settings = repository.getOrInitSettings("INR", "MODERN_HORIZON")
        assertEquals("EUR", settings.appCurrency)
        assertEquals("MINIMALIST_NORDIC", settings.themeId)

        // 3. Create trip & add/delete travelers with details
        val tripId = repository.createTrip(
            title = "Goa Friends Trip",
            destination = "Goa",
            startDate = "10/12/2026",
            endDate = "15/12/2026",
            daysCount = 5,
            baseCurrency = "INR",
            totalBudget = 50000.0,
            members = emptyList(),
            offlineRegionName = "",
            centerLat = 0.0,
            centerLng = 0.0
        )

        var trip = repository.allTrips.first().first { it.id == tripId }
        repository.addTravelerWithDetails(trip, "Rahul", "9876543210", "Organizer")
        trip = repository.allTrips.first().first { it.id == tripId }
        repository.addTravelerWithDetails(trip, "Priya", "priya@example.com", "Navigator")

        val travelers = repository.observeTravelersForTrip(tripId).first()
        assertEquals(2, travelers.size)

        // 4. Add deposit by traveler name
        trip = repository.allTrips.first().first { it.id == tripId }
        val depositId = repository.addDepositByName(
            trip = trip,
            travelerName = "Rahul",
            amount = 10000.0,
            currency = "INR",
            paymentMethod = "UPI",
            note = "Initial pool deposit"
        )
        val deposits = repository.observeDepositsForTrip(tripId).first()
        assertEquals(1, deposits.size)
        assertEquals("Rahul", deposits.first().travelerName)
        assertEquals(10000.0, deposits.first().amount, 0.01)

        // 5. Verify Detailed PDF & CSV File Export
        val financials = ExpenseCalculator.calculateTripFinancials(
            trip = trip,
            expenses = emptyList(),
            deposits = deposits,
            overrideCurrency = "INR"
        )
        val exportBundle = TripExportDataBundle(
            trip = trip,
            appCurrency = "INR",
            financials = financials,
            travelers = travelers,
            deposits = deposits,
            expenses = emptyList(),
            stops = emptyList(),
            documents = emptyList()
        )
        val pdfFile = TripReportExporter.exportPdfToCacheFile(context, exportBundle)
        val csvFile = TripReportExporter.exportCsvToCacheFile(context, exportBundle)

        assertTrue("Expected generated PDF file to exist", pdfFile.exists() && pdfFile.length() > 0L)
        assertTrue("Expected generated CSV file to exist", csvFile.exists() && csvFile.length() > 0L)
        assertTrue("Expected CSV to include traveler deposit", csvFile.readText().contains("Initial pool deposit"))

        // Delete deposit
        repository.deleteDeposit(depositId)
        assertTrue(repository.observeDepositsForTrip(tripId).first().isEmpty())

        // Delete traveler
        repository.deleteTraveler(trip, travelers.first())
        assertEquals(1, repository.observeTravelersForTrip(tripId).first().size)

        // 6. Delete entire trip
        repository.deleteTrip(tripId)
        assertTrue("Expected trip to be deleted", repository.allTrips.first().isEmpty())
    }
}
