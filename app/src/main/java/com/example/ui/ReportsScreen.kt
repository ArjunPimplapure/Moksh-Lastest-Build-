package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import android.widget.Toast
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.PartyBalance
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

// Helper extension for string matching
fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { this.contains(it, ignoreCase = true) }
}

// Parse purity string like "22K (91.6)" or "91.6" to a double percentage
fun parsePurityToPercent(purityStr: String): Double {
    if (purityStr.isBlank()) return 100.0
    // Try to find the number inside parentheses, e.g. "91.6" from "22K (91.6)"
    val regex = "\\(([^)]+)\\)".toRegex()
    val match = regex.find(purityStr)
    if (match != null) {
        val groupValue = match.groupValues[1]
        val cleanValue = groupValue.replace("%", "").trim()
        cleanValue.toDoubleOrNull()?.let { return it }
    }
    // If not in parentheses, try to extract any decimal/int number
    val numberRegex = "([0-9.]+)".toRegex()
    val numMatch = numberRegex.find(purityStr)
    if (numMatch != null) {
        numMatch.groupValues[1].toDoubleOrNull()?.let { return it }
    }
    return 100.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val estimates by viewModel.estimates.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val partyBalances by viewModel.partyBalances.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Daily Report, 1 = Party Balances, 2 = Monthly Turnover, 3 = Profit & Loss

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Module Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(LuxuryGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Reports",
                    tint = LuxuryGold,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Reports & Accounts",
                    color = LuxuryGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Professional bookkeeping & P&L statements",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs for Reports - 5 tabs
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = SurfaceCard,
            contentColor = LuxuryGold,
            edgePadding = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            val tabs = listOf(
                "Daily Report",
                "Party Balances",
                "Monthly Turnover",
                "Profit & Loss",
                "Sales & Purchases"
            )
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Reports Screen Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> DailyLedgerTab(
                    viewModel = viewModel,
                    transactions = transactions,
                    estimates = estimates,
                    invoices = invoices,
                    parties = parties
                )
                1 -> PartyBalancesTab(
                    parties = parties,
                    partyBalances = partyBalances
                )
                2 -> MonthlyTurnoverTab(
                    transactions = transactions,
                    estimates = estimates,
                    invoices = invoices
                )
                3 -> ProfitLossStatementTab(
                    transactions = transactions,
                    estimates = estimates,
                    invoices = invoices
                )
                4 -> SalesPurchaseReportTab(
                    viewModel = viewModel,
                    estimates = estimates
                )
            }
        }
    }
}

// ==================== DAILY LEDGER (VOUCHER) VIEW ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLedgerTab(
    viewModel: MainViewModel,
    transactions: List<LedgerTransaction>,
    estimates: List<Estimate>,
    invoices: List<Invoice>,
    parties: List<Party>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val partiesMap = remember(parties) { parties.associateBy { it.id } }

    val dateDisplay = remember(selectedDate) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(selectedDate.time)
    }
    val shortDateStr = remember(selectedDate) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
    }

    // Filter day start and end timestamps
    val dayStartAndEnd = remember(selectedDate) {
        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        
        val calEnd = selectedDate.clone() as Calendar
        calEnd.set(Calendar.HOUR_OF_DAY, 23)
        calEnd.set(Calendar.MINUTE, 59)
        calEnd.set(Calendar.SECOND, 59)
        calEnd.set(Calendar.MILLISECOND, 999)
        val end = calEnd.timeInMillis
        start to end
    }

    // Build chronological unified list of daily double-entry vouchers
    val dayVouchers = remember(transactions, estimates, invoices, dayStartAndEnd, partiesMap) {
        val list = mutableListOf<DailyVoucherItem>()
        val start = dayStartAndEnd.first
        val end = dayStartAndEnd.second

        // 1. Process Ledger Transactions
        transactions.filter { it.date in start..end }.forEach { tx ->
            val pName = if (tx.partyId == 0) "General Cash/Bank Account" else partiesMap[tx.partyId]?.name ?: "Unknown Party"
            val pureGold = tx.goldWeight * (parsePurityToPercent(tx.purity) / 100.0)
            
            val (debit, credit, label) = when (tx.type) {
                "CASH_IN" -> Triple("Cash Account", pName, "Cash Receipt")
                "CASH_OUT" -> Triple(pName, "Cash Account", "Cash Payment")
                "BANK_RECEIPT" -> Triple(tx.bankName?.ifEmpty { "Bank Account" } ?: "Bank Account", pName, "Bank Receipt")
                "BANK_PAYMENT" -> Triple(pName, tx.bankName?.ifEmpty { "Bank Account" } ?: "Bank Account", "Bank Payment")
                "GOLD_RECEIPT" -> Triple("Gold Stock (Inward)", pName, "Gold Receipt")
                "GOLD_PAYMENT" -> Triple(pName, "Gold Stock (Outward)", "Gold Payment")
                "RATE_CUT_BUY" -> Triple("Gold Purchases Account", pName, "Rate Cut Buy")
                "RATE_CUT_SELL" -> Triple(pName, "Gold Sales Account", "Rate Cut Sell")
                else -> Triple("Suspense Account", "Suspense Account", tx.type)
            }

            list.add(
                DailyVoucherItem(
                    id = "TX-${tx.id}",
                    type = tx.type,
                    label = label,
                    partyName = pName,
                    date = tx.date,
                    debitLedger = debit,
                    creditLedger = credit,
                    amount = tx.amount,
                    goldWeight = tx.goldWeight,
                    pureGoldWeight = pureGold,
                    rate = tx.rate,
                    reference = tx.referenceNo?.ifEmpty { "TX-${tx.id}" } ?: "TX-${tx.id}",
                    remarks = tx.remarks
                )
            )
        }

        // 2. Process Estimates (Sales/Purchases)
        estimates.filter { it.date in start..end }.forEach { est ->
            val pName = est.partyName.ifEmpty { partiesMap[est.partyId]?.name ?: "Walk-in Customer" }
            val estItems = est.itemsJson.deserializeItems()
            val totalWt = estItems.sumOf { it.weight }
            val totalPure = estItems.sumOf { it.pureFineWeight }
            
            val (debit, credit, label) = if (est.isPurchase) {
                Triple("Gold Purchases Account", pName, "Purchase Estimate")
            } else {
                Triple(pName, "Gold Sales Account", "Estimate Invoice")
            }

            list.add(
                DailyVoucherItem(
                    id = "EST-${est.id}",
                    type = if (est.isPurchase) "PURCHASE_ESTIMATE" else "SALES_ESTIMATE",
                    label = label,
                    partyName = pName,
                    date = est.date,
                    debitLedger = debit,
                    creditLedger = credit,
                    amount = est.totalAmount,
                    goldWeight = totalWt,
                    pureGoldWeight = totalPure,
                    rate = 0.0,
                    reference = "Doc #${est.estimateNumber}",
                    remarks = est.remarks,
                    itemsJson = est.itemsJson
                )
            )
        }

        // 3. Process Invoices
        invoices.filter { it.date in start..end }.forEach { inv ->
            val pName = inv.partyName.ifEmpty { partiesMap[inv.partyId]?.name ?: "Walk-in Customer" }
            val invItems = inv.itemsJson.deserializeItems()
            val totalWt = invItems.sumOf { it.weight }
            val totalPure = invItems.sumOf { it.pureFineWeight }

            list.add(
                DailyVoucherItem(
                    id = "INV-${inv.id}",
                    type = "TAX_INVOICE",
                    label = "Tax Invoice",
                    partyName = pName,
                    date = inv.date,
                    debitLedger = pName,
                    creditLedger = "Gold Sales Account",
                    amount = inv.totalAmount,
                    goldWeight = totalWt,
                    pureGoldWeight = totalPure,
                    rate = 0.0,
                    reference = "Inv #${inv.invoiceNumber}",
                    remarks = inv.remarks,
                    itemsJson = inv.itemsJson
                )
            )
        }

        list.sortBy { it.date }
        list
    }

    // Compute Summaries
    val summaryCashIn = dayVouchers.filter { it.type in listOf("CASH_IN", "BANK_RECEIPT") }.sumOf { it.amount }
    val summaryCashOut = dayVouchers.filter { it.type in listOf("CASH_OUT", "BANK_PAYMENT") }.sumOf { it.amount }
    val netCash = summaryCashIn - summaryCashOut

    val summaryGoldIn = dayVouchers.filter { it.type in listOf("GOLD_RECEIPT", "RATE_CUT_BUY") }.sumOf { it.goldWeight }
    val summaryGoldOut = dayVouchers.filter { it.type in listOf("GOLD_PAYMENT", "RATE_CUT_SELL") }.sumOf { it.goldWeight }
    val netGold = summaryGoldIn - summaryGoldOut

    val summarySales = dayVouchers.filter { it.type in listOf("SALES_ESTIMATE", "TAX_INVOICE") }.sumOf { it.amount }
    val summaryPurchases = dayVouchers.filter { it.type == "PURCHASE_ESTIMATE" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Navigation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedDate = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                            }
                        ) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = "Prev Day", tint = LuxuryGold, modifier = Modifier.size(18.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = LuxuryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateDisplay,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                selectedDate = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                            }
                        ) {
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", tint = LuxuryGold, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { selectedDate = Calendar.getInstance() },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text("Go to Today", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share PDF Report Button
                        Button(
                            onClick = {
                                if (dayVouchers.isEmpty()) {
                                    Toast.makeText(context, "No entries to share on this date", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val start = dayStartAndEnd.first
                                val end = dayStartAndEnd.second
                                com.example.utils.PdfExporter.shareDailyReportPdf(
                                    context = context,
                                    dateStr = shortDateStr,
                                    vouchers = dayVouchers,
                                    estimatesForDay = estimates.filter { it.date in start..end },
                                    invoicesForDay = invoices.filter { it.date in start..end },
                                    cashIn = summaryCashIn,
                                    cashOut = summaryCashOut,
                                    goldIn = summaryGoldIn,
                                    goldOut = summaryGoldOut,
                                    totalSales = summarySales,
                                    totalPurchases = summaryPurchases
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF Report", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Daily Summary Stats
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ACCOUNTANT DAILY STATS",
                        color = LuxuryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Cash In (Dr)", color = TextGray, fontSize = 10.sp)
                            Text("₹${formatAmount(summaryCashIn)}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Total Cash Out (Cr)", color = TextGray, fontSize = 10.sp)
                            Text("₹${formatAmount(summaryCashOut)}", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Net Cash Balance", color = TextGray, fontSize = 10.sp)
                            Text("₹${formatAmount(netCash)}", color = if (netCash >= 0) AccentGreen else AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = DarkBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Gold In (Weight)", color = TextGray, fontSize = 10.sp)
                            Text("${formatWeight(summaryGoldIn)} g", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Gold Out (Weight)", color = TextGray, fontSize = 10.sp)
                            Text("${formatWeight(summaryGoldOut)} g", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Net Metal Flow", color = TextGray, fontSize = 10.sp)
                            Text("${formatWeight(netGold)} g", color = if (netGold >= 0) AccentGreen else AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = DarkBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Sales Billing (Est/Tax)", color = TextGray, fontSize = 10.sp)
                            Text("₹${formatAmount(summarySales)}", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column {
                            Text("Purchase Billing (Est)", color = TextGray, fontSize = 10.sp)
                            Text("₹${formatAmount(summaryPurchases)}", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Voucher List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Double Entry Ledgers (${dayVouchers.size})",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Voucher format for software entries",
                    color = TextGray,
                    fontSize = 10.sp
                )
            }
        }

        if (dayVouchers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Layers, contentDescription = "No Vouchers", tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No transactions or bills recorded on this date.", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        } else {
            items(dayVouchers) { v ->
                val isInflow = v.type in listOf("CASH_IN", "BANK_RECEIPT", "GOLD_RECEIPT", "RATE_CUT_SELL", "SALES_ESTIMATE", "TAX_INVOICE")
                val isMetalTx = v.type in listOf("GOLD_RECEIPT", "GOLD_PAYMENT", "RATE_CUT_BUY", "RATE_CUT_SELL")
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isInflow) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isInflow) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = v.label,
                                    color = if (isInflow) AccentGreen else AccentRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = v.reference,
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Party / Account row
                        Text(
                            text = v.partyName,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = DarkBackground.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Double-entry representation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Dr", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(v.debitLedger, color = TextWhite, fontSize = 12.sp, maxLines = 1)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Cr", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(v.creditLedger, color = TextGray, fontSize = 12.sp, maxLines = 1)
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                if (v.amount > 0) {
                                    Text(
                                        text = "₹${formatAmount(v.amount)}",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                if (v.goldWeight > 0) {
                                    Text(
                                        text = "${formatWeight(v.goldWeight)} g",
                                        color = LuxuryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (v.pureGoldWeight > 0 && Math.abs(v.pureGoldWeight - v.goldWeight) > 0.001) {
                                        Text(
                                            text = "Pure: ${formatWeight(v.pureGoldWeight)} g",
                                            color = TextGray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                if (v.rate > 0) {
                                    Text(
                                        text = "Rate: ₹${formatAmount(v.rate)}",
                                        color = TextGray,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        if (v.remarks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Remarks: ${v.remarks}",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            )
                        }

                        if (v.itemsJson.isNotEmpty()) {
                            val itemsList = remember(v.itemsJson) { v.itemsJson.deserializeItems() }
                            if (itemsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = DarkBackground.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ITEMIZED BREAKDOWN (FOR ACCOUNTANT):",
                                    color = LuxuryGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                itemsList.forEachIndexed { index, item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkBackground.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${item.name}",
                                                color = TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "₹${formatAmount(item.total)}",
                                                color = LuxuryGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (item.itemType == "METAL") {
                                            Text(
                                                text = "Gross: ${item.grossWeight}g | Stone: ${item.stoneWeight}g | Net: ${item.weight}g",
                                                color = TextGray,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Wastage: ${item.wastagePercent}% | Fine: ${String.format("%.3f", item.pureFineWeight)}g",
                                                color = TextGray,
                                                fontSize = 10.sp
                                            )
                                            if (item.isRateLocked) {
                                                Text(
                                                    text = "Rate: ₹${formatAmount(item.rate)}/g | MC: ₹${formatAmount(item.makingCharges)}/g",
                                                    color = TextGray,
                                                    fontSize = 10.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "Rate: Unfixed | MC: ₹${formatAmount(item.makingCharges)}/g",
                                                    color = TextGray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Qty: ${item.pieces} pcs | Rate: ₹${formatAmount(item.rate)} each",
                                                color = TextGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                        val extraParts = mutableListOf<String>()
                                        if (item.stoneCharges > 0) extraParts.add("Stone: ₹${formatAmount(item.stoneCharges)}")
                                        if (item.freightCost > 0) extraParts.add("Freight: ₹${formatAmount(item.freightCost)}")
                                        if (item.hallmarkingCharges > 0) extraParts.add("Hallmark: ₹${formatAmount(item.hallmarkingCharges)}")
                                        if (extraParts.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Extra Chg: ${extraParts.joinToString(" | ")}",
                                                color = LuxuryGold.copy(alpha = 0.8f),
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }
                                    if (index < itemsList.size - 1) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== REPORT OF ALL BALANCES ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyBalancesTab(
    parties: List<Party>,
    partyBalances: Map<Int, PartyBalance>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(0) } // 0 = All, 1 = Cash Dr (Receivable), 2 = Cash Cr (Payable), 3 = Gold Dr (Receivable), 4 = Gold Cr (Payable)

    // Compute aggregations across all balances
    val totalCashDr = partyBalances.values.filter { it.cashBalance > 0 }.sumOf { it.cashBalance }
    val totalCashCr = partyBalances.values.filter { it.cashBalance < 0 }.sumOf { Math.abs(it.cashBalance) }
    val netCash = totalCashDr - totalCashCr

    val totalGoldDr = partyBalances.values.filter { it.goldBalance > 0 }.sumOf { it.goldBalance }
    val totalGoldCr = partyBalances.values.filter { it.goldBalance < 0 }.sumOf { Math.abs(it.goldBalance) }
    val netGold = totalGoldDr - totalGoldCr

    val filteredList = remember(parties, partyBalances, searchQuery, filterType) {
        parties.map { party ->
            val balance = partyBalances[party.id] ?: PartyBalance(0.0, 0.0)
            party to balance
        }.filter { (party, balance) ->
            val matchesSearch = party.name.contains(searchQuery, ignoreCase = true) || 
                                party.city.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (filterType) {
                0 -> true
                1 -> balance.cashBalance > 0
                2 -> balance.cashBalance < 0
                3 -> balance.goldBalance > 0
                4 -> balance.goldBalance < 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Aggregations Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TOTAL PARTY BALANCE AGGREGATIONS (TRIAL BALANCE)",
                        color = LuxuryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cash Receivables (Dr)", color = TextGray, fontSize = 9.sp)
                            Text("₹${formatAmount(totalCashDr)}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cash Payables (Cr)", color = TextGray, fontSize = 9.sp)
                            Text("₹${formatAmount(totalCashCr)}", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Net Cash Balance", color = TextGray, fontSize = 9.sp)
                            Text("₹${formatAmount(netCash)} ${if (netCash >= 0) "Dr" else "Cr"}", color = if (netCash >= 0) AccentGreen else AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = DarkBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gold Receivables (Dr)", color = TextGray, fontSize = 9.sp)
                            Text("${formatWeight(totalGoldDr)} g", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gold Payables (Cr)", color = TextGray, fontSize = 9.sp)
                            Text("${formatWeight(totalGoldCr)} g", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Net Gold Balance", color = TextGray, fontSize = 9.sp)
                            Text("${formatWeight(Math.abs(netGold))} g ${if (netGold >= 0) "Dr" else "Cr"}", color = if (netGold >= 0) AccentGreen else AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Export Button
                    Button(
                        onClick = {
                            val trialBalanceText = buildString {
                                appendLine("*JEWELLEDGER TRIAL BALANCE REPORT*")
                                appendLine("Date: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())}")
                                appendLine("---------------------------------------")
                                appendLine("Party Name (City) | Cash Bal (Dr/Cr) | Gold Bal (Dr/Cr)")
                                appendLine("---------------------------------------")
                                parties.forEach { p ->
                                    val b = partyBalances[p.id] ?: PartyBalance(0.0, 0.0)
                                    val cashStr = if (b.cashBalance >= 0) "₹${formatAmount(b.cashBalance)} Dr" else "₹${formatAmount(Math.abs(b.cashBalance))} Cr"
                                    val goldStr = if (b.goldBalance >= 0) "${formatWeight(b.goldBalance)}g Dr" else "${formatWeight(Math.abs(b.goldBalance))}g Cr"
                                    appendLine("${p.name} (${p.city}) | $cashStr | $goldStr")
                                }
                                appendLine("---------------------------------------")
                                appendLine("Net Cash: ₹${formatAmount(netCash)} ${if (netCash >= 0) "Dr" else "Cr"}")
                                appendLine("Net Gold: ${formatWeight(Math.abs(netGold))}g ${if (netGold >= 0) "Dr" else "Cr"}")
                            }
                            clipboardManager.setText(AnnotatedString(trialBalanceText))
                            Toast.makeText(context, "Trial Balance report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Full Balances Report (CSV/Format)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar and Quick Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or city...", fontSize = 12.sp, color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = LuxuryGold, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.3f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterChips = listOf("All", "Cash Dr", "Cash Cr", "Gold Dr", "Gold Cr")
                    filterChips.forEachIndexed { idx, label ->
                        val selected = filterType == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LuxuryGold else SurfaceCard)
                                .border(1.dp, if (selected) LuxuryGold else TextGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { filterType = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else TextGray,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Results Count Header
        item {
            Text(
                text = "Accounts Match: ${filteredList.size} parties",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // List of Party Cards
        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No matching party balances found.", color = TextGray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredList) { (party, balance) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = party.name,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${party.city} • ${party.phone}",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (party.partyType == "SUPPLIER") LuxuryGold.copy(alpha = 0.1f) else TextGray.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = party.partyType,
                                    color = if (party.partyType == "SUPPLIER") LuxuryGold else TextGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = DarkBackground.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Cash Ledger Balance", color = TextGray, fontSize = 9.sp)
                                val cBal = balance.cashBalance
                                if (cBal > 0) {
                                    Text("₹${formatAmount(cBal)} Dr", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else if (cBal < 0) {
                                    Text("₹${formatAmount(Math.abs(cBal))} Cr", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Text("₹0.00", color = TextGray, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gold Weight Balance", color = TextGray, fontSize = 9.sp)
                                val gBal = balance.goldBalance
                                if (gBal > 0) {
                                    Text("${formatWeight(gBal)} g Dr", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else if (gBal < 0) {
                                    Text("${formatWeight(Math.abs(gBal))} g Cr", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Text("0.000 g", color = TextGray, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== MONTHLY SALES & PURCHASES ====================
data class MonthSummary(
    val monthCode: String, // YYYY-MM
    val monthLabel: String, // June 2026
    var totalSales: Double = 0.0,
    var totalPurchases: Double = 0.0,
    var salesGoldWeight: Double = 0.0,
    var purchasesGoldWeight: Double = 0.0,
    var salesCount: Int = 0,
    var purchaseCount: Int = 0,
    var rateCutSellsAmt: Double = 0.0,
    var rateCutBuysAmt: Double = 0.0,
    var realTaxInvoiceSales: Double = 0.0,
    var estimateSales: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyTurnoverTab(
    transactions: List<LedgerTransaction>,
    estimates: List<Estimate>,
    invoices: List<Invoice>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var expandedMonth by remember { mutableStateOf<String?>(null) }

    // Group and aggregate monthly metrics
    val monthlyData = remember(transactions, estimates, invoices) {
        val summaries = mutableMapOf<String, MonthSummary>()
        val sdfCode = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Helper to get or insert summary
        fun getSummary(dateLong: Long): MonthSummary {
            val code = sdfCode.format(Date(dateLong))
            return summaries.getOrPut(code) {
                MonthSummary(
                    monthCode = code,
                    monthLabel = sdfLabel.format(Date(dateLong))
                )
            }
        }

        // 1. Process Estimates
        estimates.forEach { est ->
            val summary = getSummary(est.date)
            val estItems = est.itemsJson.deserializeItems()
            val totalWt = estItems.sumOf { it.weight }
            
            if (est.isPurchase) {
                summary.totalPurchases += est.totalAmount
                summary.purchasesGoldWeight += totalWt
                summary.purchaseCount++
            } else {
                summary.totalSales += est.totalAmount
                summary.salesGoldWeight += totalWt
                summary.estimateSales += est.totalAmount
                summary.salesCount++
            }
        }

        // 2. Process Invoices
        invoices.forEach { inv ->
            val summary = getSummary(inv.date)
            val invItems = inv.itemsJson.deserializeItems()
            val totalWt = invItems.sumOf { it.weight }
            
            summary.totalSales += inv.totalAmount
            summary.salesGoldWeight += totalWt
            summary.realTaxInvoiceSales += inv.totalAmount
            summary.salesCount++
        }

        // 3. Process Rate Cut Ledger Transactions as supplementary turnover
        transactions.forEach { tx ->
            val summary = getSummary(tx.date)
            if (tx.type == "RATE_CUT_BUY") {
                summary.totalPurchases += tx.amount
                summary.purchasesGoldWeight += tx.goldWeight
                summary.rateCutBuysAmt += tx.amount
            } else if (tx.type == "RATE_CUT_SELL") {
                summary.totalSales += tx.amount
                summary.salesGoldWeight += tx.goldWeight
                summary.rateCutSellsAmt += tx.amount
            }
        }

        summaries.values.sortedByDescending { it.monthCode }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "MONTH-OVER-MONTH BUSINESS TURNOVER",
                        color = LuxuryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tracks absolute billing value and metal volume flows on a calendar-month basis.",
                        color = TextGray,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Button(
                        onClick = {
                            if (monthlyData.isEmpty()) {
                                Toast.makeText(context, "No monthly data to export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val summaryStr = buildString {
                                appendLine("*JEWELLEDGER MONTHLY TURNOVER REPORT*")
                                appendLine("----------------------------------------")
                                monthlyData.forEach { d ->
                                    appendLine("*${d.monthLabel}*")
                                    appendLine("- Sales: ₹${formatAmount(d.totalSales)} (Metal: ${formatWeight(d.salesGoldWeight)}g)")
                                    appendLine("- Purchases: ₹${formatAmount(d.totalPurchases)} (Metal: ${formatWeight(d.purchasesGoldWeight)}g)")
                                    appendLine("- Net Position: ₹${formatAmount(d.totalSales - d.totalPurchases)}")
                                    appendLine("----------------------------------------")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(summaryStr))
                            Toast.makeText(context, "Monthly report copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Monthly Summary via Clipboard", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (monthlyData.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No monthly transactions recorded yet.", color = TextGray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(monthlyData) { d ->
                val isExpanded = expandedMonth == d.monthCode
                val monthlyNet = d.totalSales - d.totalPurchases
                val monthlyNetGold = d.salesGoldWeight - d.purchasesGoldWeight

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedMonth = if (isExpanded) null else d.monthCode }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = d.monthLabel,
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = LuxuryGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Brief Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Monthly Sales", color = TextGray, fontSize = 9.sp)
                                Text("₹${formatAmount(d.totalSales)}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${formatWeight(d.salesGoldWeight)} g Sold", color = TextGray, fontSize = 9.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Monthly Purchases", color = TextGray, fontSize = 9.sp)
                                Text("₹${formatAmount(d.totalPurchases)}", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${formatWeight(d.purchasesGoldWeight)} g Bought", color = TextGray, fontSize = 9.sp)
                            }
                        }

                        // Expanded breakdown details
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = DarkBackground.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("SALES TURN OVER DETAILS", color = LuxuryGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimate Invoices Sales:", color = TextWhite, fontSize = 11.sp)
                                Text("₹${formatAmount(d.estimateSales)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax Invoices Sales:", color = TextWhite, fontSize = 11.sp)
                                Text("₹${formatAmount(d.realTaxInvoiceSales)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sales Rate Cuts Realized:", color = TextWhite, fontSize = 11.sp)
                                Text("₹${formatAmount(d.rateCutSellsAmt)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("PURCHASE TURN OVER DETAILS", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Purchase Estimates:", color = TextWhite, fontSize = 11.sp)
                                Text("₹${formatAmount(d.totalPurchases - d.rateCutBuysAmt)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Purchase Rate Cuts Realized:", color = TextWhite, fontSize = 11.sp)
                                Text("₹${formatAmount(d.rateCutBuysAmt)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = DarkBackground.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Net Cash Turnover Diff", color = TextGray, fontSize = 9.sp)
                                    Text(
                                        text = "${if (monthlyNet >= 0) "+" else ""}₹${formatAmount(monthlyNet)}",
                                        color = if (monthlyNet >= 0) AccentGreen else AccentRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Gold Flow Diff", color = TextGray, fontSize = 9.sp)
                                    Text(
                                        text = "${if (monthlyNetGold >= 0) "+" else ""}${formatWeight(monthlyNetGold)} g",
                                        color = if (monthlyNetGold >= 0) AccentGreen else AccentRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== PROFIT & LOSS STATEMENTS ====================
data class PLStructure(
    val cashSalesFixed: Double,
    val cashSalesRateCuts: Double,
    val totalCashRevenue: Double,
    val cashPurchFixed: Double,
    val cashPurchRateCuts: Double,
    val totalCashCost: Double,
    val operatingExpenses: Double,
    val netCashProfit: Double,
    
    val goldBilledSales: Double,
    val goldReceiptsLedger: Double,
    val totalGoldInflows: Double,
    val goldBilledPurch: Double,
    val goldPaymentsLedger: Double,
    val totalGoldOutflows: Double,
    val netMetalProfit: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossStatementTab(
    transactions: List<LedgerTransaction>,
    estimates: List<Estimate>,
    invoices: List<Invoice>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var periodFilter by remember { mutableStateOf("ALL") } // ALL, MONTH, YEAR

    val plData = remember(transactions, estimates, invoices, periodFilter) {
        val filteredTxs = transactions.filter { isDateInPeriod(it.date, periodFilter) }
        val filteredEsts = estimates.filter { isDateInPeriod(it.date, periodFilter) }
        val filteredInvs = invoices.filter { isDateInPeriod(it.date, periodFilter) }

        // --- 1. CASH PROFIT/LOSS CALCULATIONS ---
        
        // Sales from Rate-Locked Items (Where gold rate/price is fixed)
        var lockedSalesAmt = 0.0
        filteredEsts.filter { !it.isPurchase }.forEach { est ->
            val items = est.itemsJson.deserializeItems()
            lockedSalesAmt += items.filter { it.isRateLocked }.sumOf { it.total }
        }
        filteredInvs.forEach { inv ->
            val items = inv.itemsJson.deserializeItems()
            lockedSalesAmt += items.filter { it.isRateLocked }.sumOf { it.total }
        }

        // Sales from realised rate cuts (RATE_CUT_SELL)
        val rateCutSellsCash = filteredTxs.filter { it.type == "RATE_CUT_SELL" }.sumOf { it.amount }
        val totalRevenueCash = lockedSalesAmt + rateCutSellsCash

        // Purchases from Rate-Locked Items
        var lockedPurchsAmt = 0.0
        filteredEsts.filter { it.isPurchase }.forEach { est ->
            val items = est.itemsJson.deserializeItems()
            lockedPurchsAmt += items.filter { it.isRateLocked }.sumOf { it.total }
        }

        // Purchases from realised rate cuts (RATE_CUT_BUY)
        val rateCutBuysCash = filteredTxs.filter { it.type == "RATE_CUT_BUY" }.sumOf { it.amount }
        val totalCostCash = lockedPurchsAmt + rateCutBuysCash

        // Operating Expenses (Cash/bank paid outwards to general/salary/rent/etc)
        val expensesCash = filteredTxs.filter { tx ->
            val isExpenseType = tx.type in listOf("CASH_OUT", "BANK_PAYMENT")
            val isGeneral = tx.partyId == 0
            val hasExpenseKeywords = tx.remarks.containsAny("expense", "salary", "rent", "travel", "tea", "food", "office", "bill", "freight", "electric", "telephone", "tax")
            isExpenseType && (isGeneral || hasExpenseKeywords)
        }.sumOf { it.amount }

        val netCashProfitVal = totalRevenueCash - totalCostCash - expensesCash


        // --- 2. METAL (GOLD) PROFIT/LOSS CALCULATIONS (In 24k Fine Grams) ---
        
        // Pure Gold Billed in Sales (We earn this gold obligation from customers)
        var goldInflowSales = 0.0
        filteredEsts.filter { !it.isPurchase }.forEach { est ->
            val items = est.itemsJson.deserializeItems()
            goldInflowSales += items.sumOf { it.pureFineWeight }
        }
        filteredInvs.forEach { inv ->
            val items = inv.itemsJson.deserializeItems()
            goldInflowSales += items.sumOf { it.pureFineWeight }
        }

        // Gold Received on Ledger (GOLD_RECEIPT)
        val goldReceiptsPhysical = filteredTxs.filter { it.type == "GOLD_RECEIPT" }.sumOf { it.goldWeight }
        val totalGoldInflowsVal = goldInflowSales + goldReceiptsPhysical

        // Pure Gold Billed in Purchases (We owe this gold obligation to suppliers)
        var goldOutflowPurch = 0.0
        filteredEsts.filter { it.isPurchase }.forEach { est ->
            val items = est.itemsJson.deserializeItems()
            goldOutflowPurch += items.sumOf { it.pureFineWeight }
        }

        // Gold Issued on Ledger (GOLD_PAYMENT)
        val goldPaymentsPhysical = filteredTxs.filter { it.type == "GOLD_PAYMENT" }.sumOf { it.goldWeight }
        val totalGoldOutflowsVal = goldOutflowPurch + goldPaymentsPhysical

        val netGoldGainVal = totalGoldInflowsVal - totalGoldOutflowsVal

        PLStructure(
            cashSalesFixed = lockedSalesAmt,
            cashSalesRateCuts = rateCutSellsCash,
            totalCashRevenue = totalRevenueCash,
            cashPurchFixed = lockedPurchsAmt,
            cashPurchRateCuts = rateCutBuysCash,
            totalCashCost = totalCostCash,
            operatingExpenses = expensesCash,
            netCashProfit = netCashProfitVal,
            
            goldBilledSales = goldInflowSales,
            goldReceiptsLedger = goldReceiptsPhysical,
            totalGoldInflows = totalGoldInflowsVal,
            goldBilledPurch = goldOutflowPurch,
            goldPaymentsLedger = goldPaymentsPhysical,
            totalGoldOutflows = totalGoldOutflowsVal,
            netMetalProfit = netGoldGainVal
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Range Filter row
        item {
            Row(
                modifier = Modifier
                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                    .padding(2.dp)
                    .fillMaxWidth()
            ) {
                listOf("ALL" to "All Time", "MONTH" to "Last 30 Days", "YEAR" to "This Year").forEach { (code, label) ->
                    val selected = periodFilter == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) LuxuryGold else Color.Transparent)
                            .clickable { periodFilter = code }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else TextGray,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Explanatory Note Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LuxuryGold.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = LuxuryGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Wholesale accounting separates Metal and Currency: Metal profit tracks 24k weight gained from wastage/melt differentials, while Cash profit represents locked fixed-rate sales minus expenditures.",
                        color = TextGray,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // SECTION 1: CASH PROFIT & LOSS (INR ₹)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. CASH PROFIT & LOSS (₹)",
                            color = LuxuryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (plData.netCashProfit >= 0) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (plData.netCashProfit >= 0) "Cash Profit" else "Cash Loss",
                                color = if (plData.netCashProfit >= 0) AccentGreen else AccentRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("NET REALIZED CASH PROFIT", color = TextGray, fontSize = 9.sp)
                    Text(
                        text = "₹${formatAmount(plData.netCashProfit)}",
                        color = if (plData.netCashProfit >= 0) AccentGreen else AccentRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = DarkBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Revenues
                    Text("REVENUES (INFLOWS)", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rate-Locked Sales Bills:", color = TextWhite, fontSize = 11.sp)
                        Text("+₹${formatAmount(plData.cashSalesFixed)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Realized Sales Rate Cuts:", color = TextWhite, fontSize = 11.sp)
                        Text("+₹${formatAmount(plData.cashSalesRateCuts)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Cash Revenue:", color = TextGray, fontSize = 11.sp)
                        Text("₹${formatAmount(plData.totalCashRevenue)}", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Purchases & Costs
                    Text("DIRECT COSTS & PURCHASES", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rate-Locked Purchase Bills:", color = TextWhite, fontSize = 11.sp)
                        Text("-₹${formatAmount(plData.cashPurchFixed)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Realized Purchase Rate Cuts:", color = TextWhite, fontSize = 11.sp)
                        Text("-₹${formatAmount(plData.cashPurchRateCuts)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Operating Expenses (Salary/Rent/etc):", color = TextWhite, fontSize = 11.sp)
                        Text("-₹${formatAmount(plData.operatingExpenses)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Expenses & Costs:", color = TextGray, fontSize = 11.sp)
                        Text("₹${formatAmount(plData.totalCashCost + plData.operatingExpenses)}", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SECTION 2: METAL PROFIT & POSITION (GOLD GRAMS)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. METAL PROFIT & POSITION (FINE GOLD)",
                            color = LuxuryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (plData.netMetalProfit >= 0) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (plData.netMetalProfit >= 0) "Gold Gain" else "Gold Deficit",
                                color = if (plData.netMetalProfit >= 0) AccentGreen else AccentRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("NET OPERATIONS GOLD GAIN/LOSS (24K FINE)", color = TextGray, fontSize = 9.sp)
                    Text(
                        text = "${if (plData.netMetalProfit >= 0) "+" else ""}${formatWeight(plData.netMetalProfit)} g",
                        color = if (plData.netMetalProfit >= 0) AccentGreen else AccentRed,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = DarkBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Metal Inflows
                    Text("METAL EARNED (INFLOWS / OBLIGATIONS)", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gold weight billed in Sales:", color = TextWhite, fontSize = 11.sp)
                        Text("+${formatWeight(plData.goldBilledSales)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gold physically received on ledger:", color = TextWhite, fontSize = 11.sp)
                        Text("+${formatWeight(plData.goldReceiptsLedger)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Gold Inward/Due:", color = TextGray, fontSize = 11.sp)
                        Text("${formatWeight(plData.totalGoldInflows)} g", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metal Outflows
                    Text("METAL OBLIGATED (OUTFLOWS / EXCHANGES)", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gold weight billed in Purchases:", color = TextWhite, fontSize = 11.sp)
                        Text("-${formatWeight(plData.goldBilledPurch)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gold physically issued on ledger:", color = TextWhite, fontSize = 11.sp)
                        Text("-${formatWeight(plData.goldPaymentsLedger)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Gold Outward/Paid:", color = TextGray, fontSize = 11.sp)
                        Text("${formatWeight(plData.totalGoldOutflows)} g", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Export Actions
        item {
            Button(
                onClick = {
                    val summaryText = """
                        *JEWELLEDGER PROFIT & LOSS REPORT*
                        *Period Filter:* ${periodFilter.capitalize()}
                        -----------------------------------------
                        *1. CASH STATEMENT (INR):*
                        - Total Cash Revenue: ₹${formatAmount(plData.totalCashRevenue)}
                        - Total Cash Costs: ₹${formatAmount(plData.totalCashCost)}
                        - Operating Expenses: ₹${formatAmount(plData.operatingExpenses)}
                        - *NET REALIZED CASH PROFIT:* ₹${formatAmount(plData.netCashProfit)}
                        
                        *2. METAL POSITION (GOLD):*
                        - Fine Gold Inward: ${formatWeight(plData.totalGoldInflows)} g
                        - Fine Gold Outward: ${formatWeight(plData.totalGoldOutflows)} g
                        - *NET METAL GAIN/LOSS:* ${formatWeight(plData.netMetalProfit)} g (24k Fine)
                        -----------------------------------------
                        Generated via Wholesale accounting ledger.
                    """.trimIndent()
                    clipboardManager.setText(AnnotatedString(summaryText))
                    Toast.makeText(context, "P&L Statement copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Full P&L Statement", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Utility function to check if a timestamp falls into selected period
private fun isDateInPeriod(timestamp: Long, period: String): Boolean {
    if (period == "ALL") return true
    val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()

    return when (period) {
        "MONTH" -> {
            val diffMs = nowCal.timeInMillis - itemCal.timeInMillis
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            diffDays <= 30
        }
        "YEAR" -> {
            itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        }
        else -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPurchaseReportTab(
    viewModel: MainViewModel,
    estimates: List<Estimate>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "SALES", "PURCHASES"
    var dateFilterMode by remember { mutableStateOf("ALL") } // "ALL", "CUSTOM"
    
    val calendarStart = remember { Calendar.getInstance().apply { add(Calendar.DATE, -7) } }
    val calendarEnd = remember { Calendar.getInstance() }
    
    var startTimestamp by remember { mutableStateOf(calendarStart.timeInMillis) }
    var endTimestamp by remember { mutableStateOf(calendarEnd.timeInMillis) }

    val formatter = remember { SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()) }

    // Dialog trigger variables
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = startTimestamp }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth, 0, 0, 0)
                startTimestamp = newCal.timeInMillis
                showStartDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showEndDatePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = endTimestamp }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth, 23, 59, 59)
                endTimestamp = newCal.timeInMillis
                showEndDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Filter estimates based on dates and sale/purchase filter
    val filteredEsts = remember(estimates, selectedFilter, dateFilterMode, startTimestamp, endTimestamp) {
        estimates.filter { est ->
            val matchesType = when (selectedFilter) {
                "SALES" -> !est.isPurchase
                "PURCHASES" -> est.isPurchase
                else -> true
            }
            val matchesDate = if (dateFilterMode == "CUSTOM") {
                est.date in startTimestamp..endTimestamp
            } else {
                true
            }
            matchesType && matchesDate
        }.sortedByDescending { est -> est.date }
    }

    // Consolidated values
    val (totGross, totStone, totNet, totFine, totStoneChg, totFreight, totHallmark, totCash) = remember(filteredEsts) {
        var gross = 0.0
        var stone = 0.0
        var net = 0.0
        var fine = 0.0
        var stoneChg = 0.0
        var freight = 0.0
        var hallmark = 0.0
        var cash = 0.0

        filteredEsts.forEach { est ->
            val items = est.itemsJson.deserializeItems()
            items.forEach { item ->
                if (item.itemType == "METAL") {
                    gross += item.grossWeight
                    stone += item.stoneWeight
                    net += item.weight
                    fine += item.pureFineWeight
                }
                stoneChg += item.stoneCharges
                freight += item.freightCost
                hallmark += item.hallmarkingCharges
            }
            cash += est.totalAmount
        }
        Octuple(gross, stone, net, fine, stoneChg, freight, hallmark, cash)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Filter Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Document Type Filter:", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "All", "SALES" to "Sales Only", "PURCHASES" to "Purchases Only").forEach { (id, label) ->
                            val sel = selectedFilter == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) LuxuryGold else DarkBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (sel) LuxuryGold else TextGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { selectedFilter = id }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (sel) DarkBackground else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Date Range Filter:", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "All Dates", "CUSTOM" to "Custom Range").forEach { (id, label) ->
                            val sel = dateFilterMode == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) LuxuryGold else DarkBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (sel) LuxuryGold else TextGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { dateFilterMode = id }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (sel) DarkBackground else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (dateFilterMode == "CUSTOM") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showStartDatePicker = true },
                                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxuryGold),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("From: ${formatter.format(Date(startTimestamp))}", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { showEndDatePicker = true },
                                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxuryGold),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("To: ${formatter.format(Date(endTimestamp))}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Consolidated Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CONSOLIDATED SUMMARY REPORT",
                        color = LuxuryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Gross Weight:", color = TextGray, fontSize = 11.sp)
                        Text("${String.format("%.3f", totGross)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Stone Weight:", color = TextGray, fontSize = 11.sp)
                        Text("${String.format("%.3f", totStone)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Net Weight:", color = TextGray, fontSize = 11.sp)
                        Text("${String.format("%.3f", totNet)} g", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Pure Fine Weight:", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("${String.format("%.3f", totFine)} g Fine", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = DarkBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Stone Charges:", color = TextGray, fontSize = 11.sp)
                        Text("₹${formatAmount(totStoneChg)}", color = TextWhite, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Freight Cost:", color = TextGray, fontSize = 11.sp)
                        Text("₹${formatAmount(totFreight)}", color = TextWhite, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Hallmarking Charges:", color = TextGray, fontSize = 11.sp)
                        Text("₹${formatAmount(totHallmark)}", color = TextWhite, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Bill Amount (Cash):", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("₹${formatAmount(totCash)}", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of transactions/documents
        if (filteredEsts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching sale or purchase records found.", color = TextGray, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredEsts) { est ->
                val itemsList = est.itemsJson.deserializeItems()
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (est.isPurchase) "PURCHASE BILL #${est.estimateNumber}" else "SALES BILL #${est.estimateNumber}",
                                    color = if (est.isPurchase) LuxuryGold else AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = est.partyName,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(est.date)),
                                    color = TextGray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "₹${formatAmount(est.totalAmount)}",
                                    color = if (est.isPurchase) LuxuryGold else AccentGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = DarkBackground.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))

                        itemsList.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = item.name,
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.itemType == "METAL") {
                                    Text(
                                        text = "Gross: ${item.grossWeight}g | Stone: ${item.stoneWeight}g | Net: ${item.weight}g\nWastage: ${item.wastagePercent}% | Fine: ${String.format("%.3f", item.pureFineWeight)}g",
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Text(
                                        text = "${item.pieces} pcs @ ₹${formatAmount(item.rate)} each",
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                                val extras = mutableListOf<String>()
                                if (item.stoneCharges > 0) extras.add("Stone: ₹${formatAmount(item.stoneCharges)}")
                                if (item.freightCost > 0) extras.add("Freight: ₹${formatAmount(item.freightCost)}")
                                if (item.hallmarkingCharges > 0) extras.add("Hallmark: ₹${formatAmount(item.hallmarkingCharges)}")
                                if (extras.isNotEmpty()) {
                                    Text(
                                        text = extras.joinToString(" | "),
                                        color = LuxuryGold.copy(alpha = 0.8f),
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Helper class to hold 8 elements
data class Octuple<A, B, C, D, E, F, G, H>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
    val eighth: H
)
