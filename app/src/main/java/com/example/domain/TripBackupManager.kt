package com.example.domain

import android.content.Context
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomMapPinEntity
import com.example.data.local.DepositEntity
import com.example.data.local.ItineraryStopEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.TravelDocumentEntity
import com.example.data.local.TravelerEntity
import com.example.data.local.TripEntity
import com.example.data.repository.TripRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object TripBackupManager {

    private val fileSafeTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun defaultBackupFileName(): String {
        val ts = fileSafeTimestampFormat.format(Date())
        return "TripExpenditure_Backup_$ts.json.gz"
    }

    suspend fun exportCompressedBackup(context: Context, repository: TripRepository, outputStream: FileOutputStream) {
        val settings = repository.appSettings.first()
        val trips = repository.allTrips.first()
        val expenses = repository.allExpenses.first()
        val deposits = repository.allDeposits.first()
        val stops = repository.allStops.first()

        val jsonRoot = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            settings?.let { s ->
                put("settings", JSONObject().apply {
                    put("appCurrency", s.appCurrency)
                    put("themeId", s.themeId)
                    put("lastSelectedTripId", s.lastSelectedTripId ?: JSONObject.NULL)
                })
            }

            val tripsArr = JSONArray()
            trips.forEach { t ->
                tripsArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("destination", t.destination)
                    put("startDate", t.startDate)
                    put("endDate", t.endDate)
                    put("daysCount", t.daysCount)
                    put("baseCurrency", t.baseCurrency)
                    put("totalBudget", t.totalBudget)
                    put("membersCsv", t.membersCsv)
                    put("tagline", t.tagline)
                    put("offlineRegionName", t.offlineRegionName)
                    put("offlineRegionDownloaded", t.offlineRegionDownloaded)
                    put("offlineSizeMb", t.offlineSizeMb)
                    put("centerLat", t.centerLat)
                    put("centerLng", t.centerLng)
                    put("createdAt", t.createdAt)
                })
            }
            put("trips", tripsArr)

            val expensesArr = JSONArray()
            expenses.forEach { e ->
                expensesArr.put(JSONObject().apply {
                    put("id", e.id)
                    put("tripId", e.tripId)
                    put("title", e.title)
                    put("category", e.category)
                    put("amount", e.amount)
                    put("currency", e.currency)
                    put("amountInBase", e.amountInBase)
                    put("paidByMember", e.paidByMember)
                    put("splitAmongCsv", e.splitAmongCsv)
                    put("splitType", e.splitType)
                    put("customSharesCsv", e.customSharesCsv)
                    put("notes", e.notes)
                    put("isSettlement", e.isSettlement)
                    put("createdAt", e.createdAt)
                })
            }
            put("expenses", expensesArr)

            val depositsArr = JSONArray()
            deposits.forEach { d ->
                depositsArr.put(JSONObject().apply {
                    put("id", d.id)
                    put("tripId", d.tripId)
                    put("travelerName", d.travelerName)
                    put("amount", d.amount)
                    put("currency", d.currency)
                    put("amountInBase", d.amountInBase)
                    put("paymentMethod", d.paymentMethod)
                    put("note", d.note)
                    put("createdAt", d.createdAt)
                })
            }
            put("deposits", depositsArr)

            val stopsArr = JSONArray()
            stops.forEach { st ->
                stopsArr.put(JSONObject().apply {
                    put("id", st.id)
                    put("tripId", st.tripId)
                    put("dayNumber", st.dayNumber)
                    put("title", st.title)
                    put("category", st.category)
                    put("startTime", st.startTime)
                    put("locationName", st.locationName)
                    put("latitude", st.latitude)
                    put("longitude", st.longitude)
                    put("estimatedCost", st.estimatedCost)
                    put("currency", st.currency)
                    put("notes", st.notes)
                    put("votersCsv", st.votersCsv)
                    put("isVisited", st.isVisited)
                })
            }
            put("stops", stopsArr)
        }

        val jsonString = jsonRoot.toString()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(jsonString.toByteArray(Charsets.UTF_8))
            gzip.finish()
        }
    }

    fun saveColdArchive(context: Context, repository: TripRepository) {
        try {
            val archiveFile = File(context.filesDir, "trip_expenditure_cold_archive.json.gz")
            FileOutputStream(archiveFile).use { fos ->
                kotlinx.coroutines.runBlocking {
                    exportCompressedBackup(context, repository, fos)
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun importCompressedBackup(context: Context, repository: TripRepository, inputStream: java.io.InputStream) {
        val jsonString = try {
            GZIPInputStream(inputStream).use { gzip ->
                gzip.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (_: Exception) {
            // fallback if uncompressed json
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        }

        val root = JSONObject(jsonString)
        // Clear or upsert data into database
        // For simplicity, we can parse trips, expenses, deposits, stops and insert or restore
        val tripsJson = root.optJSONArray("trips") ?: JSONArray()
        for (i in 0 until tripsJson.length()) {
            val tObj = tripsJson.getJSONObject(i)
            val title = tObj.optString("title", "Trip")
            val dest = tObj.optString("destination", "")
            val start = tObj.optString("startDate", "")
            val end = tObj.optString("endDate", "")
            val days = tObj.optInt("daysCount", 3)
            val baseCur = tObj.optString("baseCurrency", "INR")
            val budget = tObj.optDouble("totalBudget", 0.0)
            val members = tObj.optString("membersCsv", "")

            repository.createTrip(
                title = title,
                destination = dest,
                startDate = start,
                endDate = end,
                daysCount = days,
                baseCurrency = baseCur,
                totalBudget = budget,
                members = members.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                offlineRegionName = "",
                centerLat = 0.0,
                centerLng = 0.0
            )
        }
    }
}
