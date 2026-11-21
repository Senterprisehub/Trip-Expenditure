package com.example.domain

import com.example.data.local.DepositEntity
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.ItineraryStopEntity
import com.example.data.local.SplitType
import com.example.data.local.TripEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val displayName: String,
    val ratePerUsd: Double
)

data class MemberBalance(
    val memberName: String,
    val totalDepositedInBase: Double,
    val totalPaidInBase: Double,
    val fairShareInBase: Double,
    val netBalanceInBase: Double // Positive = owed/surplus; Negative = owes/deficit
)

data class SettlementTransfer(
    val fromMember: String,
    val toMember: String,
    val amountInBase: Double,
    val currency: String
)

data class CategorySpendSummary(
    val category: ExpenseCategory,
    val amountInBase: Double,
    val fractionOfTotal: Float
)

data class TripFinancialSummary(
    val baseCurrency: String,
    val totalDepositedInBase: Double,
    val totalSpentInBase: Double,
    val remainingPoolBalance: Double,
    val totalBudget: Double,
    val remainingBudget: Double,
    val budgetProgress: Float,
    val memberBalances: List<MemberBalance>,
    val settlementTransfers: List<SettlementTransfer>,
    val categoryBreakdown: List<CategorySpendSummary>
)

object ExpenseCalculator {

    const val DEPOSIT_POOL_PAYER = "Trip Deposit Pool"

    val currencyCatalog: List<CurrencyInfo> = listOf(
        CurrencyInfo("INR", "₹", "Indian Rupee (₹)", 83.5),
        CurrencyInfo("USD", "$", "US Dollar ($)", 1.0),
        CurrencyInfo("EUR", "€", "Euro (€)", 0.92),
        CurrencyInfo("GBP", "£", "British Pound (£)", 0.79),
        CurrencyInfo("JPY", "¥", "Japanese Yen (¥)", 150.0),
        CurrencyInfo("AUD", "A$", "Australian Dollar (A$)", 1.52),
        CurrencyInfo("CAD", "C$", "Canadian Dollar (C$)", 1.38),
        CurrencyInfo("CHF", "CHF ", "Swiss Franc (CHF)", 0.88),
        CurrencyInfo("AED", "AED ", "UAE Dirham (AED)", 3.67),
        CurrencyInfo("SGD", "S$", "Singapore Dollar (S$)", 1.34),
        CurrencyInfo("THB", "฿", "Thai Baht (฿)", 35.5),
        CurrencyInfo("NOK", "kr ", "Norwegian Krone (kr)", 10.65)
    )

    private val usdExchangeRates: Map<String, Double> =
        currencyCatalog.associate { it.code to it.ratePerUsd }

    val supportedCurrencies: List<String> = currencyCatalog.map { it.code }

    fun currencySymbol(code: String): String {
        val upper = code.uppercase(Locale.US)
        return currencyCatalog.firstOrNull { it.code == upper }?.symbol ?: "$upper "
    }

    fun formatCurrency(amount: Double, currencyCode: String): String {
        val code = currencyCode.uppercase(Locale.US)
        val sym = currencySymbol(code)
        val absVal = abs(amount)
        val formatted = if (code == "JPY") {
            String.format(Locale.US, "%,d", absVal.roundToInt())
        } else {
            String.format(Locale.US, "%,.2f", absVal)
        }
        return if (amount < -0.005) "-$sym$formatted" else "$sym$formatted"
    }

    /**
     * Converts [amount] from [fromCurrency] to [toBaseCurrency] using offline rates.
     */
    fun convertToBase(amount: Double, fromCurrency: String, toBaseCurrency: String): Double {
        if (amount <= 0.0) return 0.0
        val fromCode = fromCurrency.uppercase(Locale.US)
        val toCode = toBaseCurrency.uppercase(Locale.US)
        if (fromCode == toCode) return roundCents(amount)

        val fromRate = usdExchangeRates[fromCode] ?: 1.0
        val toRate = usdExchangeRates[toCode] ?: 1.0
        val inUsd = amount / fromRate
        return roundCents(inUsd * toRate)
    }

    /**
     * Computes complete financial metrics including Traveler Deposits by Name,
     * per-member net balances, and minimum-transfer debt settlement instructions.
     */
    fun calculateTripFinancials(
        trip: TripEntity,
        expenses: List<ExpenseEntity>,
        deposits: List<DepositEntity> = emptyList(),
        overrideCurrency: String? = null
    ): TripFinancialSummary {
        val displayCurrency = (overrideCurrency ?: trip.baseCurrency).uppercase(Locale.US)
        val members = trip.memberList()
        val depositMap = members.associateWith { 0.0 }.toMutableMap()
        val paidMap = members.associateWith { 0.0 }.toMutableMap()
        val shareMap = members.associateWith { 0.0 }.toMutableMap()
        val categoryTotals = mutableMapOf<ExpenseCategory, Double>()

        var totalDeposited = 0.0
        for (deposit in deposits) {
            val depInDisplay = convertToBase(deposit.amount, deposit.currency, displayCurrency)
            totalDeposited += depInDisplay
            val name = deposit.travelerName.trim()
            if (name.isNotEmpty()) {
                depositMap[name] = (depositMap[name] ?: 0.0) + depInDisplay
            }
        }

        var totalRealSpend = 0.0

        for (expense in expenses) {
            val baseAmt = convertToBase(expense.amount, expense.currency, displayCurrency)

            val payer = expense.paidByMember.trim()
            if (payer.isNotEmpty() && !payer.equals(DEPOSIT_POOL_PAYER, ignoreCase = true)) {
                paidMap[payer] = (paidMap[payer] ?: 0.0) + baseAmt
            }

            val participants = expense.splitParticipants().ifEmpty { members }
            val customShares = expense.customShareMap()

            if (participants.isNotEmpty()) {
                when (expense.resolvedSplitType()) {
                    SplitType.EQUAL -> {
                        val perPerson = baseAmt / participants.size
                        for (participant in participants) {
                            shareMap[participant] = (shareMap[participant] ?: 0.0) + perPerson
                        }
                    }
                    SplitType.EXACT -> {
                        val sumCustom = customShares.values.sum()
                        if (sumCustom > 0.0) {
                            for ((person, exactVal) in customShares) {
                                val normalized = (exactVal / sumCustom) * baseAmt
                                shareMap[person] = (shareMap[person] ?: 0.0) + normalized
                            }
                        } else {
                            val perPerson = baseAmt / participants.size
                            for (participant in participants) {
                                shareMap[participant] = (shareMap[participant] ?: 0.0) + perPerson
                            }
                        }
                    }
                    SplitType.PERCENTAGE -> {
                        val sumPct = customShares.values.sum()
                        if (sumPct > 0.0) {
                            for ((person, pct) in customShares) {
                                val share = (pct / sumPct) * baseAmt
                                shareMap[person] = (shareMap[person] ?: 0.0) + share
                            }
                        } else {
                            val perPerson = baseAmt / participants.size
                            for (participant in participants) {
                                shareMap[participant] = (shareMap[participant] ?: 0.0) + perPerson
                            }
                        }
                    }
                }
            }

            if (!expense.isSettlement && expense.expenseCategory() != ExpenseCategory.SETTLEMENT) {
                totalRealSpend += baseAmt
                val cat = expense.expenseCategory()
                categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + baseAmt
            }
        }

        val allPeople = (members + depositMap.keys + paidMap.keys + shareMap.keys)
            .filter { it.isNotBlank() && !it.equals(DEPOSIT_POOL_PAYER, ignoreCase = true) }
            .distinct()

        val memberBalances = allPeople.map { person ->
            val deposited = roundCents(depositMap[person] ?: 0.0)
            val paid = roundCents(paidMap[person] ?: 0.0)
            val share = roundCents(shareMap[person] ?: 0.0)
            MemberBalance(
                memberName = person,
                totalDepositedInBase = deposited,
                totalPaidInBase = paid,
                fairShareInBase = share,
                netBalanceInBase = roundCents((deposited + paid) - share)
            )
        }

        val settlements = simplifyDebts(memberBalances, displayCurrency)

        val breakdown = categoryTotals.entries
            .sortedByDescending { it.value }
            .map { (cat, amt) ->
                val frac = if (totalRealSpend > 0.0) (amt / totalRealSpend).toFloat().coerceIn(0f, 1f) else 0f
                CategorySpendSummary(
                    category = cat,
                    amountInBase = roundCents(amt),
                    fractionOfTotal = frac
                )
            }

        val convertedBudget = convertToBase(trip.totalBudget, trip.baseCurrency, displayCurrency)
        val effectiveCap = if (totalDeposited > 0.0 && convertedBudget <= 0.0) {
            totalDeposited
        } else {
            convertedBudget
        }

        val progress = if (effectiveCap > 0.0) {
            (totalRealSpend / effectiveCap).toFloat().coerceIn(0f, 1f)
        } else 0f

        return TripFinancialSummary(
            baseCurrency = displayCurrency,
            totalDepositedInBase = roundCents(totalDeposited),
            totalSpentInBase = roundCents(totalRealSpend),
            remainingPoolBalance = roundCents(totalDeposited - totalRealSpend),
            totalBudget = roundCents(effectiveCap),
            remainingBudget = roundCents(effectiveCap - totalRealSpend),
            budgetProgress = progress,
            memberBalances = memberBalances,
            settlementTransfers = settlements,
            categoryBreakdown = breakdown
        )
    }

    /**
     * Greedy minimum-transfer debt resolution algorithm.
     */
    fun simplifyDebts(
        balances: List<MemberBalance>,
        currency: String
    ): List<SettlementTransfer> {
        val debtors = balances
            .filter { it.netBalanceInBase < -0.01 }
            .map { it.memberName to -it.netBalanceInBase }
            .sortedByDescending { it.second }
            .toMutableList()

        val creditors = balances
            .filter { it.netBalanceInBase > 0.01 }
            .map { it.memberName to it.netBalanceInBase }
            .sortedByDescending { it.second }
            .toMutableList()

        val transfers = mutableListOf<SettlementTransfer>()
        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val (debtorName, debtAmt) = debtors[i]
            val (creditorName, creditAmt) = creditors[j]

            val settled = roundCents(min(debtAmt, creditAmt))
            if (settled > 0.01) {
                transfers.add(
                    SettlementTransfer(
                        fromMember = debtorName,
                        toMember = creditorName,
                        amountInBase = settled,
                        currency = currency
                    )
                )
            }

            val remainingDebt = debtAmt - settled
            val remainingCredit = creditAmt - settled

            if (remainingDebt <= 0.01) {
                i++
            } else {
                debtors[i] = debtorName to remainingDebt
            }

            if (remainingCredit <= 0.01) {
                j++
            } else {
                creditors[j] = creditorName to remainingCredit
            }
        }

        return transfers
    }

    /**
     * Calculates great-circle distance in kilometers between two GPS coordinates.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return roundCents(r * c)
    }

    /**
     * Generates a clean, shareable CSV & Markdown expenditure report for export.
     */
    fun buildExportableLedger(
        trip: TripEntity,
        financials: TripFinancialSummary,
        deposits: List<DepositEntity>,
        stops: List<ItineraryStopEntity>,
        expenses: List<ExpenseEntity>
    ): String {
        val cur = financials.baseCurrency
        val sb = StringBuilder()
        sb.appendLine("=== TRIP EXPENDITURE LEDGER ===")
        sb.appendLine("Trip: ${trip.title} (${trip.destination})")
        sb.appendLine("Dates: ${trip.startDate} - ${trip.endDate}")
        sb.appendLine("Travelers: ${trip.memberList().joinToString(", ")}")
        sb.appendLine("Total Deposited: ${formatCurrency(financials.totalDepositedInBase, cur)}")
        sb.appendLine("Total Spent: ${formatCurrency(financials.totalSpentInBase, cur)}")
        sb.appendLine("Pool Remaining: ${formatCurrency(financials.remainingPoolBalance, cur)}")
        sb.appendLine()
        sb.appendLine("--- TRAVELER DEPOSITS & NET BALANCES ---")
        for (mb in financials.memberBalances) {
            val status = when {
                mb.netBalanceInBase > 0.01 -> "Net +${formatCurrency(mb.netBalanceInBase, cur)}"
                mb.netBalanceInBase < -0.01 -> "Net -${formatCurrency(abs(mb.netBalanceInBase), cur)}"
                else -> "Settled (0.00)"
            }
            sb.appendLine(
                "• ${mb.memberName}: Deposited ${formatCurrency(mb.totalDepositedInBase, cur)} | " +
                    "Direct Paid ${formatCurrency(mb.totalPaidInBase, cur)} | " +
                    "Share ${formatCurrency(mb.fairShareInBase, cur)} -> $status"
            )
        }
        sb.appendLine()
        if (deposits.isNotEmpty()) {
            sb.appendLine("--- DEPOSITS LOG ---")
            for (dep in deposits) {
                sb.appendLine("• ${dep.travelerName}: ${formatCurrency(dep.amount, dep.currency)} (${dep.note})")
            }
            sb.appendLine()
        }
        sb.appendLine("--- EXPENSE CSV LOG ---")
        sb.appendLine("Title,Category,Amount,Currency,PaidBy,SplitAmong")
        for (exp in expenses) {
            sb.appendLine(
                "\"${exp.title}\",${exp.category},${exp.amount},${exp.currency},${exp.paidByMember},\"${exp.splitAmongCsv}\""
            )
        }
        if (stops.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("--- ITINERARY (${stops.size} STOPS) ---")
            for (stop in stops) {
                sb.appendLine("Day ${stop.dayNumber} • ${stop.startTime}: ${stop.title} (${stop.locationName})")
            }
        }
        return sb.toString()
    }

    private fun roundCents(value: Double): Double =
        (value * 100.0).roundToInt() / 100.0
}
