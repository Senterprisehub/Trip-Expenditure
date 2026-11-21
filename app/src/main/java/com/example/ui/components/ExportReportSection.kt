package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ExpenseCalculator
import com.example.domain.TripExportDataBundle
import com.example.domain.TripReportExporter
import com.example.ui.theme.LocalTripDesignTokens
import kotlin.math.abs

private enum class ReportPreviewFormat {
    PDF_DOCUMENT,
    CSV_TABLE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedExportReportSection(
    bundle: TripExportDataBundle,
    lastExportStatus: String?,
    onSavePdfToDevice: (TripExportDataBundle) -> Unit,
    onSharePdfFile: (TripExportDataBundle) -> Unit,
    onSaveCsvToDevice: (TripExportDataBundle) -> Unit,
    onShareCsvFile: (TripExportDataBundle) -> Unit,
    onNotifyCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTripDesignTokens.current
    val clipboardManager = LocalClipboardManager.current
    var previewFormat by remember { mutableStateOf(ReportPreviewFormat.PDF_DOCUMENT) }

    // Multi-Filter State
    var selectedTraveler by remember { mutableStateOf("All Travelers") }
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var selectedDay by remember { mutableStateOf(0) }
    var isFilterExpanded by remember { mutableStateOf(false) } // Retractable by default

    val allTravelerNames = remember(bundle) {
        (bundle.travelers.map { it.name } + bundle.trip.memberList() + bundle.financials.memberBalances.map { it.memberName })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    val allCategories = remember {
        listOf("All Categories") + com.example.data.local.ExpenseCategory.entries.map { it.label }
    }

    val daysCount = bundle.trip.daysCount.coerceAtLeast(1)

    val filteredBundle = remember(bundle, selectedTraveler, selectedCategory, selectedDay) {
        TripReportExporter.filterBundle(
            bundle = bundle,
            travelerFilter = selectedTraveler,
            categoryFilter = selectedCategory,
            dayFilter = selectedDay.takeIf { it > 0 }
        )
    }

    val csvText = remember(filteredBundle) {
        TripReportExporter.generateDetailedCsv(filteredBundle)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detailed_export_report_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Retractable Multi-Filter Bar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFilterExpanded = !isFilterExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EXPORT MULTI-FILTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFilterExpanded) "Tap to collapse filter options" else "Traveler: $selectedTraveler • Category: $selectedCategory • Day: ${if (selectedDay == 0) "All" else "Day $selectedDay"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isFilterExpanded = !isFilterExpanded }) {
                        Icon(
                            imageVector = if (isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isFilterExpanded) "Collapse Filter" else "Expand Filter"
                        )
                    }
                }

                if (isFilterExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Traveler Filter Chips
                    Text("Traveler:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val travOptions = listOf("All Travelers") + allTravelerNames
                        travOptions.forEach { trav ->
                            FilterChip(
                                selected = selectedTraveler == trav,
                                onClick = { selectedTraveler = trav },
                                label = { Text(trav) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    Text("Expense Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day / Date Filter Chips
                    Text("Day Schedule Filter:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedDay == 0,
                            onClick = { selectedDay = 0 },
                            label = { Text("All Days") }
                        )
                        for (d in 1..daysCount) {
                            FilterChip(
                                selected = selectedDay == d,
                                onClick = { selectedDay = d },
                                label = { Text("Day $d") }
                            )
                        }
                    }
                }
            }
        }

        // 2. Export Control Card (PDF & CSV Actions with Filtered Bundle)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.cardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF & CSV Data Export",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DETAILED DATA EXPORT (PDF & CSV)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export Filtered Trip Ledger",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Traveler: $selectedTraveler • Category: $selectedCategory • Day: ${if (selectedDay == 0) "All" else "Day $selectedDay"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary PDF Action Row
                Text(
                    text = "PDF REPORT (FILTERED A4 MULTI-PAGE DOCUMENT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSavePdfToDevice(filteredBundle) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_save_pdf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = { onSharePdfFile(filteredBundle) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary CSV Action Row
                Text(
                    text = "CSV SPREADSHEET (EXCEL / GOOGLE SHEETS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSaveCsvToDevice(filteredBundle) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_save_csv_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = { onShareCsvFile(filteredBundle) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_share_csv_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(csvText))
                        onNotifyCopied("Copied filtered CSV data (${csvText.lines().size} rows) to clipboard")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_copy_csv_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Filtered CSV Data to Clipboard")
                }

                if (!lastExportStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tokens.positiveBalanceColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, tokens.positiveBalanceColor)
                    ) {
                        Text(
                            text = "✓ $lastExportStatus",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.positiveBalanceColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("export_status_confirmation_text")
                        )
                    }
                }
            }
        }

        // 2. Preview Mode Toggle (PDF Document Preview vs CSV Raw Table Preview)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Report Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = previewFormat == ReportPreviewFormat.PDF_DOCUMENT,
                    onClick = { previewFormat = ReportPreviewFormat.PDF_DOCUMENT },
                    label = { Text("PDF Layout") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("preview_mode_pdf_chip")
                )
                FilterChip(
                    selected = previewFormat == ReportPreviewFormat.CSV_TABLE,
                    onClick = { previewFormat = ReportPreviewFormat.CSV_TABLE },
                    label = { Text("CSV Data") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("preview_mode_csv_chip")
                )
            }
        }

        // 3. Render Selected Preview
        when (previewFormat) {
            ReportPreviewFormat.PDF_DOCUMENT -> {
                PdfDocumentSheetPreview(bundle = bundle)
            }

            ReportPreviewFormat.CSV_TABLE -> {
                CsvRawDataSheetPreview(csvText = csvText)
            }
        }
    }
}

@Composable
private fun PdfDocumentSheetPreview(bundle: TripExportDataBundle) {
    val trip = bundle.trip
    val cur = bundle.appCurrency
    val fin = bundle.financials
    val allNames = (bundle.travelers.map { it.name } + trip.memberList() + fin.memberBalances.map { it.memberName })
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_document_preview_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Dark Slate Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "TRIP EXPENDITURE — DETAILED PDF REPORT",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Trip: ${trip.title} • Destination: ${trip.destination.ifBlank { "N/A" }} • Currency: $cur (${ExpenseCalculator.currencySymbol(cur)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 KPI Summary Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PdfKpiMiniBox(
                    label = "DEPOSITED",
                    value = ExpenseCalculator.formatCurrency(fin.totalDepositedInBase, cur),
                    valueColor = Color(0xFF059669),
                    modifier = Modifier.weight(1f)
                )
                PdfKpiMiniBox(
                    label = "SPENT",
                    value = ExpenseCalculator.formatCurrency(fin.totalSpentInBase, cur),
                    valueColor = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                PdfKpiMiniBox(
                    label = "POOL BAL",
                    value = ExpenseCalculator.formatCurrency(fin.remainingPoolBalance, cur),
                    valueColor = if (fin.remainingPoolBalance >= 0.0) Color(0xFF0F766E) else Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 1: Travelers & Net Standing
            PdfSectionBanner("1. TRAVELERS ROSTER, DEPOSITS & NET STANDING ($cur)")
            Spacer(modifier = Modifier.height(6.dp))
            if (allNames.isEmpty()) {
                Text(
                    text = "No travelers added yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                allNames.forEach { name ->
                    val entity = bundle.travelers.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    val bal = fin.memberBalances.firstOrNull { it.memberName.equals(name, ignoreCase = true) }
                    val dep = bal?.totalDepositedInBase ?: 0.0
                    val paid = bal?.totalPaidInBase ?: 0.0
                    val share = bal?.fairShareInBase ?: 0.0
                    val net = bal?.netBalanceInBase ?: 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                            val contact = listOfNotNull(entity?.phoneOrEmail, entity?.roleOrNote)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            if (contact.isNotBlank()) {
                                Text(
                                    text = contact,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = "Dep: ${ExpenseCalculator.formatCurrency(dep, cur)} | Paid: ${ExpenseCalculator.formatCurrency(paid, cur)} | Share: ${ExpenseCalculator.formatCurrency(share, cur)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }
                        Text(
                            text = if (net >= 0.0) {
                                "+${ExpenseCalculator.formatCurrency(net, cur)}"
                            } else {
                                "-${ExpenseCalculator.formatCurrency(abs(net), cur)}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (net >= 0.0) Color(0xFF059669) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 2: Deposits by Name Ledger
            PdfSectionBanner("2. DEPOSITS LEDGER BY TRAVELER NAME (${bundle.deposits.size})")
            Spacer(modifier = Modifier.height(6.dp))
            if (bundle.deposits.isEmpty()) {
                Text(
                    text = "No deposits recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                bundle.deposits.forEach { dep ->
                    val conv = ExpenseCalculator.convertToBase(dep.amount, dep.currency, cur)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${dep.travelerName} (${dep.paymentMethod.ifBlank { "Deposit" }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "+${ExpenseCalculator.formatCurrency(conv, cur)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 3: Detailed Expenses Ledger
            PdfSectionBanner("3. DETAILED EXPENSES & SPLITS LEDGER (${bundle.expenses.size})")
            Spacer(modifier = Modifier.height(6.dp))
            if (bundle.expenses.isEmpty()) {
                Text(
                    text = "No expenses logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                bundle.expenses.forEach { exp ->
                    val conv = ExpenseCalculator.convertToBase(exp.amount, exp.currency, cur)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${exp.title} (${exp.expenseCategory().label})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Paid by ${exp.paidByMember} • ${exp.resolvedSplitType().label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = ExpenseCalculator.formatCurrency(conv, cur),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 4: Settlements & Itinerary Summary
            PdfSectionBanner("4. SETTLEMENT PLAN (${fin.settlementTransfers.size}) & ITINERARY (${bundle.stops.size})")
            Spacer(modifier = Modifier.height(6.dp))
            if (fin.settlementTransfers.isEmpty()) {
                Text(
                    text = "All traveler balances are settled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                fin.settlementTransfers.forEach { tr ->
                    Text(
                        text = "• ${tr.fromMember} pays ${tr.toMember}: ${ExpenseCalculator.formatCurrency(tr.amountInBase, cur)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfKpiMiniBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF475569)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PdfSectionBanner(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF0F766E),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CsvRawDataSheetPreview(csvText: String) {
    val tokens = LocalTripDesignTokens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("csv_raw_preview_card"),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "RFC-4180 CSV SPREADSHEET DATA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        RoundedCornerShape(8.dp)
                    )
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = csvText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
