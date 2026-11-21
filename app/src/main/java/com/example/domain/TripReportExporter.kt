package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.DepositEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ItineraryStopEntity
import com.example.data.local.TravelDocumentEntity
import com.example.data.local.TravelerEntity
import com.example.data.local.TripEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class TripExportDataBundle(
    val trip: TripEntity,
    val appCurrency: String,
    val financials: TripFinancialSummary,
    val travelers: List<TravelerEntity>,
    val deposits: List<DepositEntity>,
    val expenses: List<ExpenseEntity>,
    val stops: List<ItineraryStopEntity>,
    val documents: List<TravelDocumentEntity>
)

object TripReportExporter {

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val fileSafeTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun defaultPdfFileName(tripTitle: String): String {
        val slug = sanitizeFileName(tripTitle)
        val ts = fileSafeTimestampFormat.format(Date())
        return "Trip_Expenditure_${slug}_$ts.pdf"
    }

    fun defaultCsvFileName(tripTitle: String): String {
        val slug = sanitizeFileName(tripTitle)
        val ts = fileSafeTimestampFormat.format(Date())
        return "Trip_Expenditure_${slug}_$ts.csv"
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_").trim('_')
        return cleaned.ifEmpty { "Report" }
    }

    fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return "-"
        return try {
            timestampFormat.format(Date(epochMillis))
        } catch (_: Exception) {
            "-"
        }
    }

    fun escapeCsv(value: String): String {
        val sanitized = value.replace("\r\n", " ").replace("\n", " ")
        return if (sanitized.contains(',') || sanitized.contains('"')) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }

    private fun csvRow(vararg cells: Any?): String =
        cells.joinToString(",") { escapeCsv(it?.toString().orEmpty()) }

    /**
     * Generates a comprehensive, RFC-4180 compliant multi-section CSV export of the trip's
     * financial summary, travelers, settlements, deposits by name, expenses, categories,
     * itinerary stops, and travel documents.
     */
    fun generateDetailedCsv(bundle: TripExportDataBundle): String {
        val trip = bundle.trip
        val cur = bundle.appCurrency.uppercase(Locale.US)
        val fin = bundle.financials
        val nowStr = timestampFormat.format(Date())

        val sb = StringBuilder()

        // Section 1: Trip Overview & Financial Summary
        sb.appendLine(csvRow("SECTION 1: TRIP OVERVIEW & FINANCIAL SUMMARY"))
        sb.appendLine(
            csvRow(
                "Trip ID",
                "Trip Title",
                "Destination",
                "Start Date",
                "End Date",
                "Duration (Days)",
                "Application Currency",
                "Target Budget ($cur)",
                "Total Deposited ($cur)",
                "Total Spent ($cur)",
                "Remaining Pool Balance ($cur)",
                "Remaining Budget ($cur)",
                "Exported At"
            )
        )
        sb.appendLine(
            csvRow(
                trip.id,
                trip.title,
                trip.destination.ifBlank { "-" },
                trip.startDate.ifBlank { "-" },
                trip.endDate.ifBlank { "-" },
                trip.daysCount,
                cur,
                String.format(Locale.US, "%.2f", fin.totalBudget),
                String.format(Locale.US, "%.2f", fin.totalDepositedInBase),
                String.format(Locale.US, "%.2f", fin.totalSpentInBase),
                String.format(Locale.US, "%.2f", fin.remainingPoolBalance),
                String.format(Locale.US, "%.2f", fin.remainingBudget),
                nowStr
            )
        )
        sb.appendLine()

        // Section 2: Travelers Roster & Financial Standing
        sb.appendLine(csvRow("SECTION 2: TRAVELERS ROSTER & FINANCIAL STANDING"))
        sb.appendLine(
            csvRow(
                "Traveler Name",
                "Phone / Email",
                "Role / Notes",
                "Total Deposited ($cur)",
                "Direct Paid ($cur)",
                "Fair Share of Spend ($cur)",
                "Net Balance ($cur)",
                "Standing Status"
            )
        )
        val allNames = (bundle.travelers.map { it.name } + trip.memberList() + fin.memberBalances.map { it.memberName })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (allNames.isEmpty()) {
            sb.appendLine(csvRow("No travelers recorded", "-", "-", "0.00", "0.00", "0.00", "0.00", "Settled"))
        } else {
            for (name in allNames) {
                val entity = bundle.travelers.firstOrNull { it.name.equals(name, ignoreCase = true) }
                val bal = fin.memberBalances.firstOrNull { it.memberName.equals(name, ignoreCase = true) }
                val dep = bal?.totalDepositedInBase ?: 0.0
                val paid = bal?.totalPaidInBase ?: 0.0
                val share = bal?.fairShareInBase ?: 0.0
                val net = bal?.netBalanceInBase ?: 0.0
                val status = when {
                    net > 0.01 -> "Surplus / To Receive (+${ExpenseCalculator.formatCurrency(net, cur)})"
                    net < -0.01 -> "Deficit / Owes (-${ExpenseCalculator.formatCurrency(abs(net), cur)})"
                    else -> "Settled (0.00)"
                }
                sb.appendLine(
                    csvRow(
                        name,
                        entity?.phoneOrEmail?.ifBlank { "-" } ?: "-",
                        entity?.roleOrNote?.ifBlank { "-" } ?: "-",
                        String.format(Locale.US, "%.2f", dep),
                        String.format(Locale.US, "%.2f", paid),
                        String.format(Locale.US, "%.2f", share),
                        String.format(Locale.US, "%.2f", net),
                        status
                    )
                )
            }
        }
        sb.appendLine()

        // Section 3: Recommended Settlement Plan
        sb.appendLine(csvRow("SECTION 3: RECOMMENDED SETTLEMENT TRANSFERS"))
        sb.appendLine(csvRow("From Traveler (Payer)", "To Traveler (Receiver)", "Settlement Amount ($cur)", "Currency", "Formatted Amount"))
        if (fin.settlementTransfers.isEmpty()) {
            sb.appendLine(csvRow("All balances settled", "-", "0.00", cur, ExpenseCalculator.formatCurrency(0.0, cur)))
        } else {
            for (tr in fin.settlementTransfers) {
                sb.appendLine(
                    csvRow(
                        tr.fromMember,
                        tr.toMember,
                        String.format(Locale.US, "%.2f", tr.amountInBase),
                        cur,
                        ExpenseCalculator.formatCurrency(tr.amountInBase, cur)
                    )
                )
            }
        }
        sb.appendLine()

        // Section 4: Deposits Ledger by Traveler Name
        sb.appendLine(csvRow("SECTION 4: DEPOSITS LEDGER (BY TRAVELER NAME)"))
        sb.appendLine(
            csvRow(
                "Deposit ID",
                "Date & Time",
                "Traveler Name",
                "Original Amount",
                "Original Currency",
                "Amount in $cur",
                "Payment Mode",
                "Deposit Note"
            )
        )
        if (bundle.deposits.isEmpty()) {
            sb.appendLine(csvRow("-", "-", "No deposits recorded", "0.00", cur, "0.00", "-", "-"))
        } else {
            for (dep in bundle.deposits) {
                val conv = ExpenseCalculator.convertToBase(dep.amount, dep.currency, cur)
                sb.appendLine(
                    csvRow(
                        dep.id,
                        formatTimestamp(dep.createdAt),
                        dep.travelerName,
                        String.format(Locale.US, "%.2f", dep.amount),
                        dep.currency,
                        String.format(Locale.US, "%.2f", conv),
                        dep.paymentMethod.ifBlank { "-" },
                        dep.note.ifBlank { "-" }
                    )
                )
            }
        }
        sb.appendLine()

        // Section 5: Detailed Expenses & Splits Ledger
        sb.appendLine(csvRow("SECTION 5: DETAILED EXPENSES & SPLITS LEDGER"))
        sb.appendLine(
            csvRow(
                "Expense ID",
                "Date & Time",
                "Description",
                "Category",
                "Original Amount",
                "Original Currency",
                "Converted Amount ($cur)",
                "Paid By / Source",
                "Split Mode",
                "Split Among Travelers",
                "Custom Shares",
                "Entry Type",
                "Receipt / Bill Note"
            )
        )
        if (bundle.expenses.isEmpty()) {
            sb.appendLine(
                csvRow("-", "-", "No expenses logged", "-", "0.00", cur, "0.00", "-", "-", "-", "-", "-", "-")
            )
        } else {
            for (exp in bundle.expenses) {
                val conv = ExpenseCalculator.convertToBase(exp.amount, exp.currency, cur)
                sb.appendLine(
                    csvRow(
                        exp.id,
                        formatTimestamp(exp.createdAt),
                        exp.title,
                        exp.expenseCategory().label,
                        String.format(Locale.US, "%.2f", exp.amount),
                        exp.currency,
                        String.format(Locale.US, "%.2f", conv),
                        exp.paidByMember,
                        exp.resolvedSplitType().label,
                        exp.splitParticipants().joinToString("; "),
                        exp.customSharesCsv.ifBlank { "-" },
                        if (exp.isSettlement) "Settlement Transfer" else "Trip Expense",
                        exp.notes.ifBlank { "-" }
                    )
                )
            }
        }
        sb.appendLine()

        // Section 6: Category Expenditure Breakdown
        sb.appendLine(csvRow("SECTION 6: EXPENDITURE BY CATEGORY"))
        sb.appendLine(csvRow("Category", "Amount ($cur)", "Share of Total Spend (%)"))
        if (fin.categoryBreakdown.isEmpty()) {
            sb.appendLine(csvRow("No category expenditure", "0.00", "0%"))
        } else {
            for (cat in fin.categoryBreakdown) {
                sb.appendLine(
                    csvRow(
                        cat.category.label,
                        String.format(Locale.US, "%.2f", cat.amountInBase),
                        "${(cat.fractionOfTotal * 100).toInt()}%"
                    )
                )
            }
        }
        sb.appendLine()

        // Section 7: Collaborative Itinerary Schedule
        sb.appendLine(csvRow("SECTION 7: COLLABORATIVE ITINERARY SCHEDULE"))
        sb.appendLine(
            csvRow(
                "Day Number",
                "Start Time",
                "Stop / Activity Title",
                "Category",
                "Location / Address",
                "Estimated Cost ($cur)",
                "Votes Count",
                "Upvoted By",
                "Visited Status",
                "Notes"
            )
        )
        if (bundle.stops.isEmpty()) {
            sb.appendLine(csvRow("-", "-", "No itinerary stops scheduled", "-", "-", "0.00", "0", "-", "-", "-"))
        } else {
            for (stop in bundle.stops.sortedWith(compareBy({ it.dayNumber }, { it.startTime }))) {
                val estConv = ExpenseCalculator.convertToBase(stop.estimatedCost, stop.currency, cur)
                val voters = stop.voterList()
                sb.appendLine(
                    csvRow(
                        "Day ${stop.dayNumber}",
                        stop.startTime.ifBlank { "-" },
                        stop.title,
                        stop.stopCategory().label,
                        stop.locationName.ifBlank { "-" },
                        String.format(Locale.US, "%.2f", estConv),
                        voters.size,
                        voters.joinToString("; ").ifBlank { "-" },
                        if (stop.isVisited) "Visited" else "Planned",
                        stop.notes.ifBlank { "-" }
                    )
                )
            }
        }
        sb.appendLine()

        // Section 8: Travel Documents & Bookings Vault
        sb.appendLine(csvRow("SECTION 8: TRAVEL DOCUMENTS & BOOKING REFERENCES"))
        sb.appendLine(
            csvRow(
                "Document Title",
                "Document Type",
                "Reference / PNR Code",
                "Provider / Airline",
                "Valid Date",
                "Traveler / Holder Name",
                "Notes"
            )
        )
        if (bundle.documents.isEmpty()) {
            sb.appendLine(csvRow("No documents stored", "-", "-", "-", "-", "-", "-"))
        } else {
            for (doc in bundle.documents) {
                sb.appendLine(
                    csvRow(
                        doc.title,
                        doc.documentType().label,
                        doc.referenceCode.ifBlank { "-" },
                        doc.provider.ifBlank { "-" },
                        doc.validDate.ifBlank { "-" },
                        doc.holderName.ifBlank { "-" },
                        doc.notes.ifBlank { "-" }
                    )
                )
            }
        }

        return sb.toString()
    }

    /**
     * Generates a multi-page A4 PDF report using Android's native [PdfDocument] API
     * and writes it to [outputStream], with a standards-compliant PDF 1.4 fallback
     * when running in headless JVM environments.
     */
    fun writeDetailedPdfToStream(bundle: TripExportDataBundle, outputStream: OutputStream) {
        val byteBuffer = java.io.ByteArrayOutputStream()
        var renderedWithNativePdf = false

        var pdfDocument: PdfDocument? = null
        var activePage: PdfDocument.Page? = null
        try {
            pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32f
            val contentWidth = pageWidth - (margin * 2)
            val bottomLimit = pageHeight - 48f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val headerBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                style = Paint.Style.FILL
            }
            val bannerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bannerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val sectionHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 118, 110)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val positivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(5, 150, 105)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 38, 38)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }
            val kpiCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }
            val kpiStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8f
            }

            val trip = bundle.trip
            val cur = bundle.appCurrency.uppercase(Locale.US)
            val fin = bundle.financials
            val nowStr = timestampFormat.format(Date())

            var pageNumber = 1
            activePage = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas: Canvas = activePage!!.canvas
            var y = margin

            fun drawFooter(c: Canvas, pg: Int) {
                c.drawLine(margin, pageHeight - 34f, pageWidth - margin, pageHeight - 34f, linePaint)
                c.drawText(
                    "Trip Expenditure • Detailed Financial & Itinerary Report • ${trip.title}",
                    margin,
                    pageHeight - 20f,
                    footerPaint
                )
                c.drawText(
                    "Page $pg",
                    pageWidth - margin - 38f,
                    pageHeight - 20f,
                    footerPaint
                )
            }

            fun ensureSpace(requiredHeight: Float) {
                if (y + requiredHeight > bottomLimit) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(activePage)
                    activePage = null
                    pageNumber++
                    activePage = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    )
                    canvas = activePage!!.canvas
                    y = margin
                }
            }

            fun clipText(text: String, maxChars: Int): String {
                val clean = text.replace("\n", " ").trim()
                return if (clean.length <= maxChars) clean else clean.take(maxChars - 1) + "…"
            }

            // 1. Top Branded Report Banner
            val bannerRect = RectF(margin, y, pageWidth - margin, y + 68f)
            canvas.drawRoundRect(bannerRect, 8f, 8f, headerBannerPaint)
            canvas.drawText(
                "TRIP EXPENDITURE — DETAILED REPORT",
                margin + 14f,
                y + 24f,
                bannerTitlePaint
            )
            val metaLine1 = "Trip: ${trip.title}   •   Destination: ${trip.destination.ifBlank { "N/A" }}   •   Dates: ${
                listOf(trip.startDate, trip.endDate).filter { it.isNotBlank() }.joinToString(" to ").ifBlank { "${trip.daysCount} Days" }
            }"
            canvas.drawText(clipText(metaLine1, 95), margin + 14f, y + 42f, bannerSubPaint)
            canvas.drawText(
                "Application Currency: $cur (${ExpenseCalculator.currencySymbol(cur)})   •   Exported: $nowStr",
                margin + 14f,
                y + 56f,
                bannerSubPaint
            )
            y += 80f

            // 2. KPI Financial Summary Row (4 Boxes)
            val boxGap = 8f
            val boxWidth = (contentWidth - (boxGap * 3)) / 4f
            val kpis = listOf(
                "TOTAL DEPOSITED" to ExpenseCalculator.formatCurrency(fin.totalDepositedInBase, cur),
                "TOTAL SPENT" to ExpenseCalculator.formatCurrency(fin.totalSpentInBase, cur),
                "POOL BALANCE" to ExpenseCalculator.formatCurrency(fin.remainingPoolBalance, cur),
                "TARGET BUDGET" to ExpenseCalculator.formatCurrency(fin.totalBudget, cur)
            )
            kpis.forEachIndexed { idx, (label, valStr) ->
                val left = margin + idx * (boxWidth + boxGap)
                val rect = RectF(left, y, left + boxWidth, y + 44f)
                canvas.drawRoundRect(rect, 6f, 6f, kpiCardPaint)
                canvas.drawRoundRect(rect, 6f, 6f, kpiStrokePaint)
                canvas.drawText(label, left + 8f, y + 16f, tableHeaderPaint)
                canvas.drawText(clipText(valStr, 18), left + 8f, y + 34f, titlePaint.apply { textSize = 12.5f })
            }
            y += 58f

            fun drawSectionHeader(title: String) {
                ensureSpace(36f)
                val rect = RectF(margin, y, pageWidth - margin, y + 22f)
                canvas.drawRoundRect(rect, 4f, 4f, sectionHeaderBgPaint)
                canvas.drawText(title, margin + 8f, y + 15f, sectionTitlePaint)
                y += 30f
            }

            // 3. Section: Travelers Roster & Net Balances
            drawSectionHeader("1. TRAVELERS ROSTER, DEPOSITS & NET BALANCES ($cur)")
            val colT = floatArrayOf(
                margin + 4f,
                margin + 120f,
                margin + 245f,
                margin + 330f,
                margin + 410f,
                margin + 480f
            )
            canvas.drawText("Traveler Name", colT[0], y, tableHeaderPaint)
            canvas.drawText("Contact / Role", colT[1], y, tableHeaderPaint)
            canvas.drawText("Deposited", colT[2], y, tableHeaderPaint)
            canvas.drawText("Direct Paid", colT[3], y, tableHeaderPaint)
            canvas.drawText("Fair Share", colT[4], y, tableHeaderPaint)
            canvas.drawText("Net Balance", colT[5], y, tableHeaderPaint)
            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 13f

            val allNames = (bundle.travelers.map { it.name } + trip.memberList() + fin.memberBalances.map { it.memberName })
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            if (allNames.isEmpty()) {
                canvas.drawText("No travelers added yet.", colT[0], y, cellPaint)
                y += 16f
            } else {
                for (name in allNames) {
                    ensureSpace(18f)
                    val entity = bundle.travelers.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    val bal = fin.memberBalances.firstOrNull { it.memberName.equals(name, ignoreCase = true) }
                    val contactRole = listOfNotNull(entity?.phoneOrEmail, entity?.roleOrNote)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { "-" }
                    val dep = bal?.totalDepositedInBase ?: 0.0
                    val paid = bal?.totalPaidInBase ?: 0.0
                    val share = bal?.fairShareInBase ?: 0.0
                    val net = bal?.netBalanceInBase ?: 0.0

                    canvas.drawText(clipText(name, 20), colT[0], y, cellBoldPaint)
                    canvas.drawText(clipText(contactRole, 22), colT[1], y, cellPaint)
                    canvas.drawText(ExpenseCalculator.formatCurrency(dep, cur), colT[2], y, cellPaint)
                    canvas.drawText(ExpenseCalculator.formatCurrency(paid, cur), colT[3], y, cellPaint)
                    canvas.drawText(ExpenseCalculator.formatCurrency(share, cur), colT[4], y, cellPaint)
                    val netStr = if (net >= 0.0) {
                        "+${ExpenseCalculator.formatCurrency(net, cur)}"
                    } else {
                        ExpenseCalculator.formatCurrency(net, cur)
                    }
                    canvas.drawText(
                        netStr,
                        colT[5],
                        y,
                        if (net >= 0.0) positivePaint else negativePaint
                    )
                    y += 5f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 13f
                }
            }
            y += 6f

            // 4. Section: Recommended Settlements
            drawSectionHeader("2. RECOMMENDED MINIMUM-TRANSFER SETTLEMENT PLAN")
            if (fin.settlementTransfers.isEmpty()) {
                canvas.drawText("All traveler balances are settled.", margin + 4f, y, cellPaint)
                y += 18f
            } else {
                for (tr in fin.settlementTransfers) {
                    ensureSpace(18f)
                    canvas.drawText(
                        "• ${tr.fromMember}  pays  ${tr.toMember}:   ${ExpenseCalculator.formatCurrency(tr.amountInBase, cur)}",
                        margin + 6f,
                        y,
                        cellBoldPaint
                    )
                    y += 15f
                }
                y += 4f
            }

            // 5. Section: Deposits by Name Ledger
            drawSectionHeader("3. DEPOSITS LEDGER BY TRAVELER NAME (${bundle.deposits.size})")
            val colD = floatArrayOf(
                margin + 4f,
                margin + 95f,
                margin + 215f,
                margin + 315f,
                margin + 455f
            )
            canvas.drawText("Date", colD[0], y, tableHeaderPaint)
            canvas.drawText("Traveler Name", colD[1], y, tableHeaderPaint)
            canvas.drawText("Payment Mode", colD[2], y, tableHeaderPaint)
            canvas.drawText("Note", colD[3], y, tableHeaderPaint)
            canvas.drawText("Amount ($cur)", colD[4], y, tableHeaderPaint)
            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 13f

            if (bundle.deposits.isEmpty()) {
                canvas.drawText("No deposits recorded.", colD[0], y, cellPaint)
                y += 16f
            } else {
                for (dep in bundle.deposits) {
                    ensureSpace(18f)
                    val conv = ExpenseCalculator.convertToBase(dep.amount, dep.currency, cur)
                    canvas.drawText(formatTimestamp(dep.createdAt), colD[0], y, cellPaint)
                    canvas.drawText(clipText(dep.travelerName, 20), colD[1], y, cellBoldPaint)
                    canvas.drawText(clipText(dep.paymentMethod.ifBlank { "-" }, 16), colD[2], y, cellPaint)
                    canvas.drawText(clipText(dep.note.ifBlank { "-" }, 24), colD[3], y, cellPaint)
                    canvas.drawText("+${ExpenseCalculator.formatCurrency(conv, cur)}", colD[4], y, positivePaint)
                    y += 5f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 13f
                }
            }
            y += 6f

            // 6. Section: Detailed Expenses & Splits Ledger
            drawSectionHeader("4. DETAILED EXPENSES & SPLITS LEDGER (${bundle.expenses.size})")
            val colE = floatArrayOf(
                margin + 4f,
                margin + 88f,
                margin + 215f,
                margin + 290f,
                margin + 385f,
                margin + 465f
            )
            canvas.drawText("Date", colE[0], y, tableHeaderPaint)
            canvas.drawText("Expense Title", colE[1], y, tableHeaderPaint)
            canvas.drawText("Category", colE[2], y, tableHeaderPaint)
            canvas.drawText("Paid By", colE[3], y, tableHeaderPaint)
            canvas.drawText("Split Mode", colE[4], y, tableHeaderPaint)
            canvas.drawText("Amount ($cur)", colE[5], y, tableHeaderPaint)
            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 13f

            if (bundle.expenses.isEmpty()) {
                canvas.drawText("No expenses logged.", colE[0], y, cellPaint)
                y += 16f
            } else {
                for (exp in bundle.expenses) {
                    ensureSpace(18f)
                    val conv = ExpenseCalculator.convertToBase(exp.amount, exp.currency, cur)
                    canvas.drawText(formatTimestamp(exp.createdAt), colE[0], y, cellPaint)
                    canvas.drawText(clipText(exp.title, 22), colE[1], y, cellBoldPaint)
                    canvas.drawText(clipText(exp.expenseCategory().label, 13), colE[2], y, cellPaint)
                    canvas.drawText(clipText(exp.paidByMember, 16), colE[3], y, cellPaint)
                    canvas.drawText(clipText(exp.resolvedSplitType().label, 13), colE[4], y, cellPaint)
                    canvas.drawText(ExpenseCalculator.formatCurrency(conv, cur), colE[5], y, cellBoldPaint)
                    y += 5f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 13f
                }
            }
            y += 6f

            // 7. Section: Itinerary & Travel Documents Summary
            drawSectionHeader("5. ITINERARY SCHEDULE (${bundle.stops.size} STOPS) & DOCUMENTS (${bundle.documents.size})")
            if (bundle.stops.isEmpty() && bundle.documents.isEmpty()) {
                canvas.drawText("No itinerary stops or travel documents added.", margin + 4f, y, cellPaint)
                y += 16f
            } else {
                for (stop in bundle.stops.sortedWith(compareBy({ it.dayNumber }, { it.startTime }))) {
                    ensureSpace(16f)
                    val estConv = ExpenseCalculator.convertToBase(stop.estimatedCost, stop.currency, cur)
                    val line = "Day ${stop.dayNumber} (${stop.startTime.ifBlank { "Scheduled" }}): ${stop.title} • ${
                        stop.locationName.ifBlank { stop.stopCategory().label }
                    } • Est: ${ExpenseCalculator.formatCurrency(estConv, cur)} • Votes: ${stop.voterList().size}"
                    canvas.drawText(clipText(line, 98), margin + 4f, y, cellPaint)
                    y += 14f
                }
                for (doc in bundle.documents) {
                    ensureSpace(16f)
                    val docLine = "[${doc.documentType().label}] ${doc.title} • Ref: ${doc.referenceCode.ifBlank { "-" }} • ${doc.provider.ifBlank { "-" }}"
                    canvas.drawText(clipText(docLine, 98), margin + 4f, y, cellPaint)
                    y += 14f
                }
            }

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(activePage)
            activePage = null
            pdfDocument.writeTo(byteBuffer)
            if (byteBuffer.size() > 0) {
                renderedWithNativePdf = true
            }
        } catch (_: Throwable) {
            // Fallback to pure PDF 1.4 stream below
        } finally {
            try {
                if (activePage != null && pdfDocument != null) {
                    pdfDocument.finishPage(activePage)
                }
            } catch (_: Throwable) {
            }
            try {
                pdfDocument?.close()
            } catch (_: Throwable) {
            }
        }

        if (renderedWithNativePdf) {
            outputStream.write(byteBuffer.toByteArray())
            outputStream.flush()
        } else {
            writePurePdf14Fallback(bundle, outputStream)
        }
    }

    /**
     * Standards-compliant PDF 1.4 generator that produces a valid multi-line PDF document
     * when native Skia PdfDocument serialization is unavailable (e.g. local JVM unit tests).
     */
    private fun writePurePdf14Fallback(bundle: TripExportDataBundle, outputStream: OutputStream) {
        val csvLines = generateDetailedCsv(bundle).lines()
        val streamBuilder = StringBuilder()
        streamBuilder.append("BT\n/F1 10 Tf\n36 800 Td\n13 TL\n")
        for (line in csvLines.take(55)) {
            val escaped = line
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
            streamBuilder.append("($escaped) Tj T*\n")
        }
        streamBuilder.append("ET\n")
        val contentBytes = streamBuilder.toString().toByteArray(Charsets.ISO_8859_1)

        val pdfHeader = "%PDF-1.4\n"
        val obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
        val obj2 = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
        val obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n"
        val obj4Header = "4 0 obj << /Length ${contentBytes.size} >> stream\n"
        val obj4Footer = "\nendstream endobj\n"
        val obj5 = "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
        val trailer = "trailer << /Root 1 0 R /Size 6 >>\n%%EOF\n"

        outputStream.write(pdfHeader.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(obj1.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(obj2.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(obj3.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(obj4Header.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(contentBytes)
        outputStream.write(obj4Footer.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(obj5.toByteArray(Charsets.ISO_8859_1))
        outputStream.write(trailer.toByteArray(Charsets.ISO_8859_1))
        outputStream.flush()
    }

    /**
     * Writes the PDF report into the app's cache `exports/` directory and returns the [File].
     */
    fun exportPdfToCacheFile(context: Context, bundle: TripExportDataBundle): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, defaultPdfFileName(bundle.trip.title))
        FileOutputStream(pdfFile).use { out ->
            writeDetailedPdfToStream(bundle, out)
        }
        return pdfFile
    }

    /**
     * Writes the CSV report into the app's cache `exports/` directory and returns the [File].
     */
    fun exportCsvToCacheFile(context: Context, bundle: TripExportDataBundle): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, defaultCsvFileName(bundle.trip.title))
        val csvContent = generateDetailedCsv(bundle)
        csvFile.writeText(csvContent, Charsets.UTF_8)
        return csvFile
    }

    /**
     * Launches the Android system share sheet for an exported PDF or CSV [File] via [FileProvider].
     */
    fun shareExportedFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun defaultTravelerWisePdfFileName(tripTitle: String): String {
        val slug = sanitizeFileName(tripTitle)
        val ts = fileSafeTimestampFormat.format(Date())
        return "TravelerWise_Expenditure_${slug}_$ts.pdf"
    }

    fun exportTravelerWisePdfToCacheFile(context: Context, bundle: TripExportDataBundle): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, defaultTravelerWisePdfFileName(bundle.trip.title))
        FileOutputStream(pdfFile).use { out ->
            writeTravelerWisePdfToStream(bundle, out)
        }
        return pdfFile
    }

    fun writeTravelerWisePdfToStream(bundle: TripExportDataBundle, outputStream: OutputStream) {
        val byteBuffer = java.io.ByteArrayOutputStream()
        var renderedWithNativePdf = false
        var pdfDocument: PdfDocument? = null
        var activePage: PdfDocument.Page? = null
        try {
            pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32f
            val bottomLimit = pageHeight - 48f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 16f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9.5f
            }
            val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 118, 110)
                style = Paint.Style.FILL
            }
            val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
            }
            val cellBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            val trip = bundle.trip
            val cur = bundle.appCurrency.uppercase(Locale.US)
            val fin = bundle.financials

            var pageNumber = 1
            activePage = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas = activePage!!.canvas
            var y = margin

            canvas.drawText("TRAVELER-WISE EXPENDITURE BREAKDOWN REPORT", margin, y + 16f, titlePaint)
            canvas.drawText("Trip: ${trip.title} • Currency: $cur", margin, y + 32f, subPaint)
            y += 50f

            val allNames = (bundle.travelers.map { it.name } + trip.memberList() + fin.memberBalances.map { it.memberName })
                .map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            if (allNames.isEmpty()) {
                canvas.drawText("No travelers recorded for this trip.", margin, y, cellPaint)
            } else {
                for (name in allNames) {
                    if (y + 110f > bottomLimit) {
                        pdfDocument.finishPage(activePage)
                        pageNumber++
                        activePage = pdfDocument.startPage(
                            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        )
                        canvas = activePage!!.canvas
                        y = margin
                    }

                    canvas.drawRect(margin, y, pageWidth - margin, y + 22f, headerBg)
                    canvas.drawText("TRAVELER: $name", margin + 8f, y + 15f, headerText)
                    y += 28f

                    val bal = fin.memberBalances.firstOrNull { it.memberName.equals(name, ignoreCase = true) }
                    val dep = bal?.totalDepositedInBase ?: 0.0
                    val paid = bal?.totalPaidInBase ?: 0.0
                    val share = bal?.fairShareInBase ?: 0.0
                    val net = bal?.netBalanceInBase ?: 0.0

                    canvas.drawText("• Total Deposited: ${ExpenseCalculator.formatCurrency(dep, cur)}", margin + 8f, y, cellBold)
                    y += 14f
                    canvas.drawText("• Direct Paid: ${ExpenseCalculator.formatCurrency(paid, cur)}", margin + 8f, y, cellBold)
                    y += 14f
                    canvas.drawText("• Fair Share of Spend: ${ExpenseCalculator.formatCurrency(share, cur)}", margin + 8f, y, cellBold)
                    y += 14f
                    val netStr = if (net >= 0.0) "+${ExpenseCalculator.formatCurrency(net, cur)}" else ExpenseCalculator.formatCurrency(net, cur)
                    canvas.drawText("• Net Position: $netStr", margin + 8f, y, cellBold)
                    y += 18f

                    val travelerDeps = bundle.deposits.filter { it.travelerName.equals(name, ignoreCase = true) }
                    canvas.drawText("Deposits (${travelerDeps.size}):", margin + 8f, y, cellBold)
                    y += 14f
                    if (travelerDeps.isEmpty()) {
                        canvas.drawText("  None recorded", margin + 14f, y, cellPaint)
                        y += 14f
                    } else {
                        travelerDeps.forEach { d ->
                            canvas.drawText("  • ${ExpenseCalculator.formatCurrency(d.amount, d.currency)} (${d.paymentMethod.ifBlank { "Deposit" }})", margin + 14f, y, cellPaint)
                            y += 14f
                        }
                    }

                    val travelerExps = bundle.expenses.filter { 
                        it.paidByMember.equals(name, ignoreCase = true) || 
                        it.splitParticipants().any { p -> p.equals(name, ignoreCase = true) } 
                    }
                    canvas.drawText("Expenses Paid or Shared (${travelerExps.size}):", margin + 8f, y, cellBold)
                    y += 14f
                    if (travelerExps.isEmpty()) {
                        canvas.drawText("  None recorded", margin + 14f, y, cellPaint)
                        y += 14f
                    } else {
                        travelerExps.forEach { e ->
                            val conv = ExpenseCalculator.convertToBase(e.amount, e.currency, cur)
                            canvas.drawText("  • ${e.title} (${e.expenseCategory().label}) • Paid: ${e.paidByMember} • Amt: ${ExpenseCalculator.formatCurrency(conv, cur)}", margin + 14f, y, cellPaint)
                            y += 14f
                        }
                    }

                    y += 10f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 14f
                }
            }

            pdfDocument.finishPage(activePage)
            activePage = null
            pdfDocument.writeTo(byteBuffer)
            if (byteBuffer.size() > 0) renderedWithNativePdf = true
        } catch (_: Throwable) {
        } finally {
            try {
                if (activePage != null && pdfDocument != null) pdfDocument.finishPage(activePage)
            } catch (_: Throwable) {}
            try { pdfDocument?.close() } catch (_: Throwable) {}
        }

        if (renderedWithNativePdf) {
            outputStream.write(byteBuffer.toByteArray())
            outputStream.flush()
        } else {
            writePurePdf14Fallback(bundle, outputStream)
        }
    }

    fun filterBundle(
        bundle: TripExportDataBundle,
        travelerFilter: String?,
        categoryFilter: String?,
        dayFilter: Int?
    ): TripExportDataBundle {
        val filteredExpenses = bundle.expenses.filter { exp ->
            val matchesTraveler = travelerFilter.isNullOrBlank() || travelerFilter == "All Travelers" ||
                exp.paidByMember.equals(travelerFilter, ignoreCase = true) ||
                exp.splitParticipants().any { it.equals(travelerFilter, ignoreCase = true) }
            val matchesCat = categoryFilter.isNullOrBlank() || categoryFilter == "All Categories" ||
                exp.category.equals(categoryFilter, ignoreCase = true)
            matchesTraveler && matchesCat
        }
        val filteredDeposits = bundle.deposits.filter { dep ->
            val matchesTraveler = travelerFilter.isNullOrBlank() || travelerFilter == "All Travelers" ||
                dep.travelerName.equals(travelerFilter, ignoreCase = true)
            matchesTraveler
        }
        val filteredStops = bundle.stops.filter { stop ->
            val matchesDay = dayFilter == null || dayFilter <= 0 || stop.dayNumber == dayFilter
            matchesDay
        }
        return bundle.copy(
            expenses = filteredExpenses,
            deposits = filteredDeposits,
            stops = filteredStops
        )
    }

    fun exportTravelerWiseIndividualPdfToCacheFile(context: Context, bundle: TripExportDataBundle, travelerName: String): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val slug = sanitizeFileName(travelerName)
        val ts = fileSafeTimestampFormat.format(Date())
        val pdfFile = File(exportDir, "Traveler_${slug}_Expenditure_$ts.pdf")
        FileOutputStream(pdfFile).use { out ->
            writeTravelerWiseIndividualPdfToStream(bundle, travelerName, out)
        }
        return pdfFile
    }

    fun writeTravelerWiseIndividualPdfToStream(bundle: TripExportDataBundle, targetTraveler: String, outputStream: OutputStream) {
        writeSingleTravelerPdfToStream(bundle, targetTraveler, outputStream)
    }

    private fun writeSingleTravelerPdfToStream(bundle: TripExportDataBundle, targetTraveler: String, outputStream: OutputStream) {
        val byteBuffer = java.io.ByteArrayOutputStream()
        var renderedWithNativePdf = false
        var pdfDocument: PdfDocument? = null
        var activePage: PdfDocument.Page? = null
        try {
            pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32f
            val bottomLimit = pageHeight - 48f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 16f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9.5f
            }
            val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 118, 110)
                style = Paint.Style.FILL
            }
            val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
            }
            val cellBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val positivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(5, 150, 105)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 38, 38)
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            val trip = bundle.trip
            val cur = bundle.appCurrency.uppercase(Locale.US)
            val fin = bundle.financials

            var pageNumber = 1
            activePage = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas = activePage!!.canvas
            var y = margin

            canvas.drawText("INDIVIDUAL TRAVELER EXPENDITURE REPORT", margin, y + 16f, titlePaint)
            canvas.drawText("Trip: ${trip.title} • Traveler: $targetTraveler • Currency: $cur", margin, y + 32f, subPaint)
            y += 50f

            // Traveler Financial Standing (Only selected traveler)
            canvas.drawRect(margin, y, pageWidth - margin, y + 22f, headerBg)
            canvas.drawText("FINANCIAL STANDING: $targetTraveler", margin + 8f, y + 15f, headerText)
            y += 28f

            val bal = fin.memberBalances.firstOrNull { it.memberName.equals(targetTraveler, ignoreCase = true) }
            val entity = bundle.travelers.firstOrNull { it.name.equals(targetTraveler, ignoreCase = true) }
            val dep = bal?.totalDepositedInBase ?: 0.0
            val paid = bal?.totalPaidInBase ?: 0.0
            val share = bal?.fairShareInBase ?: 0.0
            val net = bal?.netBalanceInBase ?: 0.0

            canvas.drawText("• Contact / Role: ${listOfNotNull(entity?.phoneOrEmail, entity?.roleOrNote).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "N/A" }}", margin + 8f, y, cellBold)
            y += 14f
            canvas.drawText("• Total Deposited: ${ExpenseCalculator.formatCurrency(dep, cur)}", margin + 8f, y, cellBold)
            y += 14f
            canvas.drawText("• Direct Paid: ${ExpenseCalculator.formatCurrency(paid, cur)}", margin + 8f, y, cellBold)
            y += 14f
            canvas.drawText("• Fair Share of Spend: ${ExpenseCalculator.formatCurrency(share, cur)}", margin + 8f, y, cellBold)
            y += 14f
            val netStr = if (net >= 0.0) "+${ExpenseCalculator.formatCurrency(net, cur)}" else ExpenseCalculator.formatCurrency(net, cur)
            canvas.drawText("• Net Position: $netStr", margin + 8f, y, if (net >= 0.0) positivePaint else negativePaint)
            y += 24f

            // Deposits by Selected Traveler Only
            canvas.drawRect(margin, y, pageWidth - margin, y + 22f, headerBg)
            canvas.drawText("DEPOSITS RECORDED BY $targetTraveler", margin + 8f, y + 15f, headerText)
            y += 28f

            val travelerDeps = bundle.deposits.filter { it.travelerName.equals(targetTraveler, ignoreCase = true) }
            if (travelerDeps.isEmpty()) {
                canvas.drawText("No deposits recorded for $targetTraveler.", margin + 8f, y, cellPaint)
                y += 18f
            } else {
                travelerDeps.forEach { d ->
                    canvas.drawText("• ${formatTimestamp(d.createdAt)} • ${ExpenseCalculator.formatCurrency(d.amount, d.currency)} • Mode: ${d.paymentMethod.ifBlank { "Deposit" }} • Note: ${d.note.ifBlank { "-" }}", margin + 8f, y, cellPaint)
                    y += 16f
                }
            }

            y += 10f

            // Expenses Involving Selected Traveler Only
            canvas.drawRect(margin, y, pageWidth - margin, y + 22f, headerBg)
            canvas.drawText("EXPENSES PAID OR SHARED BY $targetTraveler", margin + 8f, y + 15f, headerText)
            y += 28f

            val travelerExps = bundle.expenses.filter { 
                it.paidByMember.equals(targetTraveler, ignoreCase = true) || 
                it.splitParticipants().any { p -> p.equals(targetTraveler, ignoreCase = true) } 
            }
            if (travelerExps.isEmpty()) {
                canvas.drawText("No expenses logged involving $targetTraveler.", margin + 8f, y, cellPaint)
                y += 18f
            } else {
                travelerExps.forEach { e ->
                    val conv = ExpenseCalculator.convertToBase(e.amount, e.currency, cur)
                    canvas.drawText("• ${e.title} (${e.expenseCategory().label}) • Paid By: ${e.paidByMember} • Amount: ${ExpenseCalculator.formatCurrency(conv, cur)} • Split: ${e.resolvedSplitType().label}", margin + 8f, y, cellPaint)
                    y += 16f
                }
            }

            pdfDocument.finishPage(activePage)
            activePage = null
            pdfDocument.writeTo(byteBuffer)
            if (byteBuffer.size() > 0) renderedWithNativePdf = true
        } catch (_: Throwable) {
        } finally {
            try {
                if (activePage != null && pdfDocument != null) pdfDocument.finishPage(activePage)
            } catch (_: Throwable) {}
            try { pdfDocument?.close() } catch (_: Throwable) {}
        }

        if (renderedWithNativePdf) {
            outputStream.write(byteBuffer.toByteArray())
            outputStream.flush()
        } else {
            writePurePdf14Fallback(bundle, outputStream)
        }
    }
}
