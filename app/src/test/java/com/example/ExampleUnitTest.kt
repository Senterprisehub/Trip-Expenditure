package com.example

import com.example.data.local.DepositEntity
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.SplitType
import com.example.data.local.TravelerEntity
import com.example.data.local.TripEntity
import com.example.domain.ExpenseCalculator
import com.example.domain.TripDateUtils
import com.example.domain.TripExportDataBundle
import com.example.domain.TripReportExporter
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `deposits by traveler name and pool expenses compute accurate pool and net balances`() {
        val trip = TripEntity(
            id = 1L,
            title = "Group Trip",
            destination = "Manali",
            startDate = "01/11/2026",
            endDate = "05/11/2026",
            daysCount = 5,
            baseCurrency = "INR",
            totalBudget = 20000.0,
            membersCsv = "Rahul,Priya",
            tagline = "2 Travelers",
            offlineRegionName = "",
            offlineRegionDownloaded = false,
            offlineSizeMb = 0,
            centerLat = 0.0,
            centerLng = 0.0
        )

        val deposits = listOf(
            DepositEntity(
                id = 1L,
                tripId = 1L,
                travelerName = "Rahul",
                amount = 5000.0,
                currency = "INR",
                amountInBase = 5000.0,
                paymentMethod = "UPI",
                note = "Advance"
            ),
            DepositEntity(
                id = 2L,
                tripId = 1L,
                travelerName = "Priya",
                amount = 3000.0,
                currency = "INR",
                amountInBase = 3000.0,
                paymentMethod = "Bank Transfer",
                note = "Advance"
            )
        )

        val expenses = listOf(
            ExpenseEntity(
                id = 10L,
                tripId = 1L,
                title = "Hotel Stay",
                category = ExpenseCategory.LODGING.name,
                amount = 4000.0,
                currency = "INR",
                amountInBase = 4000.0,
                paidByMember = ExpenseCalculator.DEPOSIT_POOL_PAYER,
                splitAmongCsv = "Rahul,Priya",
                splitType = SplitType.EQUAL.name
            )
        )

        val summary = ExpenseCalculator.calculateTripFinancials(
            trip = trip,
            expenses = expenses,
            deposits = deposits,
            overrideCurrency = "INR"
        )

        assertEquals(8000.0, summary.totalDepositedInBase, 0.01)
        assertEquals(4000.0, summary.totalSpentInBase, 0.01)
        assertEquals(4000.0, summary.remainingPoolBalance, 0.01)

        val rahul = summary.memberBalances.first { it.memberName == "Rahul" }
        val priya = summary.memberBalances.first { it.memberName == "Priya" }

        assertEquals(5000.0, rahul.totalDepositedInBase, 0.01)
        assertEquals(2000.0, rahul.fairShareInBase, 0.01)
        assertEquals(3000.0, rahul.netBalanceInBase, 0.01)

        assertEquals(3000.0, priya.totalDepositedInBase, 0.01)
        assertEquals(2000.0, priya.fairShareInBase, 0.01)
        assertEquals(1000.0, priya.netBalanceInBase, 0.01)

        // Verify Detailed CSV Export contains all 8 structured sections
        val bundle = TripExportDataBundle(
            trip = trip,
            appCurrency = "INR",
            financials = summary,
            travelers = listOf(
                TravelerEntity(id = 1L, tripId = 1L, name = "Rahul", phoneOrEmail = "9876543210", roleOrNote = "Admin"),
                TravelerEntity(id = 2L, tripId = 1L, name = "Priya", phoneOrEmail = "priya@mail.com", roleOrNote = "Member")
            ),
            deposits = deposits,
            expenses = expenses,
            stops = emptyList(),
            documents = emptyList()
        )
        val csv = TripReportExporter.generateDetailedCsv(bundle)
        assertTrue(csv.contains("SECTION 1: TRIP OVERVIEW & FINANCIAL SUMMARY"))
        assertTrue(csv.contains("SECTION 2: TRAVELERS ROSTER & FINANCIAL STANDING"))
        assertTrue(csv.contains("SECTION 3: RECOMMENDED SETTLEMENT TRANSFERS"))
        assertTrue(csv.contains("SECTION 4: DEPOSITS LEDGER (BY TRAVELER NAME)"))
        assertTrue(csv.contains("SECTION 5: DETAILED EXPENSES & SPLITS LEDGER"))
        assertTrue(csv.contains("SECTION 6: EXPENDITURE BY CATEGORY"))
        assertTrue(csv.contains("SECTION 7: COLLABORATIVE ITINERARY SCHEDULE"))
        assertTrue(csv.contains("SECTION 8: TRAVEL DOCUMENTS & BOOKING REFERENCES"))
        assertTrue(csv.contains("Hotel Stay"))
        assertTrue(csv.contains("Rahul"))
    }

    @Test
    fun `trip date utils formats DD MMM YYYY in UTC and calculates inclusive days and nights`() {
        fun utcMillis(year: Int, monthZeroBased: Int, day: Int): Long {
            return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, monthZeroBased, day, 0, 0, 0)
            }.timeInMillis
        }

        val start = utcMillis(2026, Calendar.NOVEMBER, 20)
        val end = utcMillis(2026, Calendar.NOVEMBER, 24)
        val sameDay = utcMillis(2026, Calendar.NOVEMBER, 20)
        val crossYearStart = utcMillis(2026, Calendar.DECEMBER, 30)
        val crossYearEnd = utcMillis(2027, Calendar.JANUARY, 2)

        assertEquals("20 Nov 2026", TripDateUtils.formatDisplayDate(start))
        assertEquals("24 Nov 2026", TripDateUtils.formatDisplayDate(end))
        assertEquals(5, TripDateUtils.calculateInclusiveDays(start, end))
        assertEquals(4, TripDateUtils.calculateNights(start, end))
        assertEquals("5 Days / 4 Nights", TripDateUtils.formatDurationLabel(5))

        // Same-day trip: 1 Day / 0 Nights
        assertEquals(1, TripDateUtils.calculateInclusiveDays(start, sameDay))
        assertEquals(0, TripDateUtils.calculateNights(start, sameDay))
        assertEquals("1 Day / 0 Nights", TripDateUtils.formatDurationLabel(1))

        // 2-day trip: singular Night label
        assertEquals("2 Days / 1 Night", TripDateUtils.formatDurationLabel(2))

        // Cross-year boundary: 30 Dec, 31 Dec, 1 Jan, 2 Jan = 4 Days / 3 Nights
        assertEquals(4, TripDateUtils.calculateInclusiveDays(crossYearStart, crossYearEnd))
        assertEquals(3, TripDateUtils.calculateNights(crossYearStart, crossYearEnd))

        // Reversed range safely clamped to 1 day / 0 nights
        assertEquals(1, TripDateUtils.calculateInclusiveDays(end, start))
        assertEquals(0, TripDateUtils.calculateNights(end, start))
    }
}
