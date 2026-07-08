package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.PartyBalance
import com.example.R
import com.example.data.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation

// Styling Color Palette - "High Density"
val DarkGold = Color(0xFFEADDFF)      // Light purple container highlight
val LightGold = Color(0xFFE8DEF8)     // Pale purple secondary container highlight
val LuxuryGold = Color(0xFF6750A4)    // Theme Deep Purple primary accent
val DarkBackground = Color(0xFFF7F2FA) // Light Lavender-grey background
val SurfaceCard = Color(0xFFFFFFFF)    // Pure White cards
val TextWhite = Color(0xFF1D1B20)      // Deep Dark grey/black main text
val TextGray = Color(0xFF625B71)       // Medium grey/purple secondary text
val AccentGreen = Color(0xFF1B5E20)    // High contrast M3 green
val AccentRed = Color(0xFFB00020)      // High contrast M3 red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelLedgerApp(viewModel: MainViewModel) {
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val cashTransactions by viewModel.cashTransactions.collectAsStateWithLifecycle()
    val bankTransactions by viewModel.bankTransactions.collectAsStateWithLifecycle()
    val estimates by viewModel.estimates.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val partyBalances by viewModel.partyBalances.collectAsStateWithLifecycle()
    val openingCash by viewModel.openingCash.collectAsStateWithLifecycle()
    val openingBank by viewModel.openingBank.collectAsStateWithLifecycle()
    val openingGold by viewModel.openingGold.collectAsStateWithLifecycle()
    val openingItemStocks by viewModel.openingItemStocks.collectAsStateWithLifecycle()
    val adminPassword by viewModel.adminPassword.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminMode.collectAsStateWithLifecycle()
    var pendingAdminAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val runWithAdmin = { action: () -> Unit ->
        action()
    }

    var selectedTab by remember { mutableStateOf(0) }

    var estimateToEdit by remember { mutableStateOf<Estimate?>(null) }
    var showPasswordPromptForEdit by remember { mutableStateOf(false) }
    var pendingEstimateToEdit by remember { mutableStateOf<Estimate?>(null) }

    // Dialog state controllers
    var showOpeningBalancesDialog by remember { mutableStateOf(false) }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var preselectedPartyForTx by remember { mutableStateOf<Party?>(null) }
    var preselectedTxType by remember { mutableStateOf("CASH_IN") }

    var showCreateEstimateInvoiceDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("ESTIMATE") } // "ESTIMATE" or "INVOICE"
    var createIsPurchase by remember { mutableStateOf(false) }

    var showCreateOrderDialog by remember { mutableStateOf(false) }
    var preselectedPartyForOrder by remember { mutableStateOf<Party?>(null) }

    // Navigation and deep subviews
    var selectedPartyForLedger by remember { mutableStateOf<Party?>(null) }
    var selectedEstimateForPreview by remember { mutableStateOf<Estimate?>(null) }
    var selectedInvoiceForPreview by remember { mutableStateOf<Invoice?>(null) }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (selectedPartyForLedger == null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "JewelLedger",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = LuxuryGold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isAdmin) LuxuryGold else SurfaceCard)
                                    .clickable {
                                        if (isAdmin) {
                                            viewModel.setAdminMode(false)
                                            Toast.makeText(context, "Logged out from Admin Mode", Toast.LENGTH_SHORT).show()
                                        } else {
                                            runWithAdmin {
                                                Toast.makeText(context, "Admin Mode Unlocked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isAdmin) "Admin" else "Staff",
                                    color = if (isAdmin) DarkBackground else TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { runWithAdmin { showOpeningBalancesDialog = true } }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Opening Balances & Settings",
                                    tint = LuxuryGold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFF3EDF7)
                    )
                )
            }
        },
        bottomBar = {
            if (selectedPartyForLedger == null) {
                NavigationBar(
                    containerColor = Color(0xFFF3EDF7),
                    contentColor = Color(0xFF49454F),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val items = listOf(
                        Triple("Ledger", Icons.Default.People, Icons.Outlined.People),
                        Triple("Cash/Bank", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
                        Triple("Rate Cut & Gold", Icons.Default.Gavel, Icons.Outlined.Gavel),
                        Triple("Est Invoices", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
                        Triple("Orders", Icons.Default.ShoppingCart, Icons.Outlined.ShoppingCart),
                        Triple("Reports", Icons.Default.BarChart, Icons.Outlined.BarChart)
                    )
                    items.forEachIndexed { index, (label, icon, outlineIcon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) icon else outlineIcon,
                                    contentDescription = label,
                                    tint = if (selectedTab == index) Color(0xFF1D192B) else Color(0xFF49454F)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (selectedTab == index) Color(0xFF1D192B) else Color(0xFF49454F),
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFFE8DEF8)
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views based on Tab or active deep Ledger Screen
            if (selectedPartyForLedger != null) {
                // Fully detailed Party ledger view with sub-options
                PartyLedgerDetailScreen(
                    party = selectedPartyForLedger!!,
                    viewModel = viewModel,
                    balances = partyBalances[selectedPartyForLedger!!.id] ?: PartyBalance(0.0, 0.0),
                    onBack = { selectedPartyForLedger = null },
                    onAddTx = { txType ->
                        preselectedPartyForTx = selectedPartyForLedger
                        preselectedTxType = txType
                        showAddTransactionDialog = true
                    },
                    onAddOrder = {
                        preselectedPartyForOrder = selectedPartyForLedger
                        showCreateOrderDialog = true
                    },
                    runWithAdmin = runWithAdmin
                )
            } else {
                when (selectedTab) {
                    0 -> LedgerTab(
                        parties = parties,
                        balances = partyBalances,
                        onPartyClick = { selectedPartyForLedger = it },
                        onAddPartyClick = { showAddPartyDialog = true }
                    )
                    1 -> CashBankTab(
                        cashTransactions = cashTransactions,
                        bankTransactions = bankTransactions,
                        cashSummary = viewModel.cashSummary.collectAsStateWithLifecycle().value,
                        bankSummary = viewModel.bankSummary.collectAsStateWithLifecycle().value,
                        parties = parties,
                        openingCash = openingCash,
                        openingBank = openingBank,
                        onAddTxClick = { type ->
                            preselectedPartyForTx = null
                            preselectedTxType = type
                            showAddTransactionDialog = true
                        }
                    )
                    2 -> RateCutGoldTab(
                        transactions = transactions,
                        parties = parties,
                        openingGold = openingGold,
                        onAddRateCutClick = { type ->
                            preselectedPartyForTx = null
                            preselectedTxType = type // "RATE_CUT_BUY" or "RATE_CUT_SELL"
                            showAddTransactionDialog = true
                        },
                        onAddGoldClick = { type ->
                            preselectedPartyForTx = null
                            preselectedTxType = type // "GOLD_RECEIPT" or "GOLD_PAYMENT"
                            showAddTransactionDialog = true
                        }
                    )
                    3 -> EstimatesInvoicesTab(
                        estimates = estimates,
                        onCreateClick = { isPurchase ->
                            createType = "ESTIMATE"
                            createIsPurchase = isPurchase
                            showCreateEstimateInvoiceDialog = true
                        },
                        onEstimateClick = { selectedEstimateForPreview = it }
                    )
                    4 -> OrdersTab(
                        orders = orders,
                        parties = parties,
                        onAddOrderClick = {
                            preselectedPartyForOrder = null
                            showCreateOrderDialog = true
                        },
                        onStatusChange = { order, nextStatus ->
                            runWithAdmin {
                                viewModel.updateOrderStatus(order, nextStatus)
                            }
                        },
                        onDeleteClick = { ord -> runWithAdmin { viewModel.deleteOrder(ord) } }
                    )
                    5 -> ReportsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Modal Dialogs setup
    if (showAddPartyDialog) {
        AddPartyDialog(
            onDismiss = { showAddPartyDialog = false },
            onConfirm = { name, phone, city, type ->
                viewModel.addParty(name, phone, city, type)
                showAddPartyDialog = false
                Toast.makeText(context, "Party Added successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            parties = parties,
            preselectedParty = preselectedPartyForTx,
            initialType = preselectedTxType,
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { partyId, type, amount, goldWeight, purity, rate, bankName, refNo, remarks, date ->
                viewModel.addTransaction(partyId, type, amount, goldWeight, purity, rate, bankName, refNo, remarks, date = date)
                showAddTransactionDialog = false
                Toast.makeText(context, "Transaction recorded successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateEstimateInvoiceDialog) {
        CreateEstimateInvoiceDialog(
            parties = parties,
            type = createType,
            isPurchase = createIsPurchase,
            editEstimate = estimateToEdit,
            onDismiss = { 
                showCreateEstimateInvoiceDialog = false
                estimateToEdit = null
            },
            onConfirm = { partyId, pName, pPhone, number, total, items, remarks, saveAsRegistered, date ->
                val existingEst = estimateToEdit
                if (existingEst != null) {
                    viewModel.updateEstimate(
                        id = existingEst.id,
                        partyId = partyId,
                        partyName = pName,
                        partyPhone = pPhone,
                        estimateNumber = number,
                        totalAmount = total,
                        items = items,
                        remarks = remarks,
                        isPurchase = existingEst.isPurchase,
                        date = date
                    )
                    Toast.makeText(context, "Estimate Invoice updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    if (saveAsRegistered && partyId == null) {
                        viewModel.addParty(pName, pPhone, "Local", if (createIsPurchase) "SUPPLIER" else "CUSTOMER") { newId ->
                            viewModel.addEstimate(newId.toInt(), pName, pPhone, number, total, items, remarks, isPurchase = createIsPurchase, date = date)
                        }
                    } else {
                        viewModel.addEstimate(partyId, pName, pPhone, number, total, items, remarks, isPurchase = createIsPurchase, date = date)
                    }
                    Toast.makeText(context, if (saveAsRegistered) "Party registered & Estimate saved successfully" else (if (createIsPurchase) "Purchase Estimate added successfully" else "Estimate Invoice generated successfully"), Toast.LENGTH_SHORT).show()
                }
                showCreateEstimateInvoiceDialog = false
                estimateToEdit = null
            }
        )
    }

    if (showCreateOrderDialog) {
        CreateOrderDialog(
            parties = parties,
            preselectedParty = preselectedPartyForOrder,
            onDismiss = { showCreateOrderDialog = false },
            onConfirm = { partyId, delDate, desc, advance, rate, remarks ->
                viewModel.addOrder(partyId, delDate, desc, advance, rate, remarks)
                showCreateOrderDialog = false
                Toast.makeText(context, "Order placed successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showOpeningBalancesDialog) {
        ManageOpeningBalancesDialog(
            viewModel = viewModel,
            onDismiss = { showOpeningBalancesDialog = false }
        )
    }

    pendingAdminAction?.let { action ->
        AdminPasswordPromptDialog(
            onDismiss = { pendingAdminAction = null },
            onSuccess = {
                action()
                pendingAdminAction = null
            },
            viewModel = viewModel
        )
    }

    // Previews of estimates & invoices
    selectedEstimateForPreview?.let { estimate ->
        EstimatePreviewDialog(
            estimate = estimate,
            onDismiss = { selectedEstimateForPreview = null },
            onEdit = { est ->
                runWithAdmin {
                    estimateToEdit = est
                    createType = "ESTIMATE"
                    createIsPurchase = est.isPurchase
                    showCreateEstimateInvoiceDialog = true
                }
                selectedEstimateForPreview = null
            },
            onDelete = { est ->
                runWithAdmin {
                    viewModel.deleteEstimate(est)
                    selectedEstimateForPreview = null
                }
            }
        )
    }

    selectedInvoiceForPreview?.let { invoice ->
        InvoicePreviewDialog(
            invoice = invoice,
            onDismiss = { selectedInvoiceForPreview = null },
            onDelete = { inv ->
                runWithAdmin {
                    viewModel.deleteInvoice(inv)
                    selectedInvoiceForPreview = null
                }
            }
        )
    }
}

// ==================== TABS IMPLEMENTATION ====================

// --- LEDGER TAB ---
@Composable
fun LedgerTab(
    parties: List<Party>,
    balances: Map<Int, PartyBalance>,
    onPartyClick: (Party) -> Unit,
    onAddPartyClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "CUSTOMER", "SUPPLIER"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Identity Header with Premium Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Check if our generated image is available
            val bannerId = com.example.R.drawable.img_jewel_banner_1782752524546
            Image(
                painter = painterResource(id = bannerId),
                contentDescription = "Jewellery Background Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "JewelLedger",
                    color = Color(0xFFEADDFF), // Beautiful theme-compatible light purple highlight
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Wholesale Jewellery Accounting & Ledgers",
                    color = Color.White.copy(alpha = 0.85f), // High contrast white subtitle
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search & Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Party...", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = LuxuryGold) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxuryGold,
                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ledger_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter button trigger
            Row(
                modifier = Modifier
                    .background(SurfaceCard, RoundedCornerShape(12.dp))
                    .border(1.dp, TextGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                listOf("ALL", "CUSTOMERS" to "CUSTOMER", "SUPPLIERS" to "SUPPLIER").forEach { item ->
                    val (label, type) = if (item is Pair<*, *>) item as Pair<String, String> else item.toString() to item.toString()
                    val selected = filterType == type
                    Text(
                        text = label.first().toString() + label.substring(1).lowercase(),
                        modifier = Modifier
                            .clickable { filterType = type }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        color = if (selected) LuxuryGold else TextGray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Parties List
        val filteredParties = parties.filter {
            (filterType == "ALL" || it.partyType == filterType) &&
                    (it.name.contains(searchQuery, ignoreCase = true) || it.city.contains(searchQuery, ignoreCase = true))
        }

        if (filteredParties.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = "No Party",
                        tint = TextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Accounts Found", color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAddPartyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold)
                    ) {
                        Text("Add Your First Party", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredParties) { party ->
                    val balance = balances[party.id] ?: PartyBalance(0.0, 0.0)
                    PartyListItem(
                        party = party,
                        balance = balance,
                        onClick = { onPartyClick(party) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddPartyClick,
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_party_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Party", tint = DarkBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Party (Ledger Account)", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PartyListItem(
    party: Party,
    balance: PartyBalance,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
            .testTag("party_item_${party.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp) // High density compact padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = party.name,
                        color = TextWhite,
                        fontSize = 15.sp, // High density slightly more compact text
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (party.partyType == "CUSTOMER") DarkGold.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = party.partyType,
                            color = if (party.partyType == "CUSTOMER") LuxuryGold else TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "City", tint = TextGray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = party.city, color = TextGray, fontSize = 12.sp)
                }
            }

            // Balances Summary on the Right side
            Column(horizontalAlignment = Alignment.End) {
                // Cash Balance
                val cashVal = balance.cashBalance
                val cashColor = if (cashVal >= 0) AccentGreen else AccentRed
                val cashLabel = if (cashVal >= 0) "Dr (Recv)" else "Cr (Pay)"
                Text(
                    text = "₹${formatAmount(Math.abs(cashVal))}",
                    color = cashColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = cashLabel,
                    color = cashColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Gold Balance
                val goldVal = balance.goldBalance
                val goldColor = if (goldVal >= 0) LuxuryGold else TextWhite
                val goldLabel = if (goldVal >= 0) "Gold Recv" else "Gold Pay"
                Text(
                    text = "${formatWeight(Math.abs(goldVal))} g",
                    color = goldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = goldLabel,
                    color = TextGray,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = TextGray)
        }
    }
}


// --- CASH & BANK TRANSACTIONS TAB ---
@Composable
fun CashBankTab(
    cashTransactions: List<LedgerTransaction>,
    bankTransactions: List<LedgerTransaction>,
    cashSummary: Pair<Double, Double>, // In, Out
    bankSummary: Pair<Double, Double>, // Receipts, Payments
    parties: List<Party>,
    openingCash: Double = 0.0,
    openingBank: Double = 0.0,
    onAddTxClick: (String) -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) } // 0 = Cash Book, 1 = Bank Book

    val partyMap = remember(parties) { parties.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Financial Books",
            color = LuxuryGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Cash vs Bank Tab Toggle
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = SurfaceCard,
            contentColor = LuxuryGold,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("Cash Book", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("Bank Book", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tabIndex == 0) {
            // Cash Book Summary Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opening Cash Balance:", color = TextGray, fontSize = 12.sp)
                Text("₹${formatAmount(openingCash)}", color = LuxuryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Cash Book Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Cash In", color = Color(0xFF21005D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("₹${formatAmount(cashSummary.first)}", color = Color(0xFF21005D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Cash Out", color = Color(0xFF1D192B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("₹${formatAmount(cashSummary.second)}", color = Color(0xFF1D192B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD0E4FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Net Cash", color = Color(0xFF001D36), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val net = openingCash + cashSummary.first - cashSummary.second
                        Text(
                            "₹${formatAmount(net)}",
                            color = Color(0xFF001D36),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cash Transaction list
            if (cashTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No cash transactions recorded yet.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cashTransactions) { tx ->
                        val party = partyMap[tx.partyId]
                        TransactionItemCard(tx = tx, partyName = party?.name ?: "General Cash")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddTxClick("CASH_IN") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, "Cash In")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record Cash In", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddTxClick("CASH_OUT") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, "Cash Out")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record Cash Out", fontWeight = FontWeight.Bold)
                }
            }

        } else {
            // Bank Book Summary Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opening Bank Balance:", color = TextGray, fontSize = 12.sp)
                Text("₹${formatAmount(openingBank)}", color = LuxuryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Bank Book Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Bank Receipts", color = Color(0xFF21005D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("₹${formatAmount(bankSummary.first)}", color = Color(0xFF21005D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Bank Payments", color = Color(0xFF1D192B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("₹${formatAmount(bankSummary.second)}", color = Color(0xFF1D192B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD0E4FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Net Bank", color = Color(0xFF001D36), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val net = openingBank + bankSummary.first - bankSummary.second
                        Text(
                            "₹${formatAmount(net)}",
                            color = Color(0xFF001D36),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bank Transaction list
            if (bankTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No bank transactions recorded yet.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bankTransactions) { tx ->
                        val party = partyMap[tx.partyId]
                        TransactionItemCard(tx = tx, partyName = party?.name ?: "General Bank Account")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddTxClick("BANK_RECEIPT") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, "Bank In")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bank Receipt", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddTxClick("BANK_PAYMENT") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, "Bank Out")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bank Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(tx: LedgerTransaction, partyName: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDebit = tx.type in listOf("CASH_OUT", "BANK_PAYMENT", "GOLD_PAYMENT", "RATE_CUT_SELL", "ESTIMATE_INVOICE")
    val isGold = tx.type in listOf("GOLD_RECEIPT", "GOLD_PAYMENT")
    val isRateCut = tx.type in listOf("RATE_CUT_BUY", "RATE_CUT_SELL")

    val typeLabel = when(tx.type) {
        "CASH_IN" -> "Cash Received"
        "CASH_OUT" -> "Cash Paid"
        "BANK_RECEIPT" -> "Bank Receipt"
        "BANK_PAYMENT" -> "Bank Payment"
        "GOLD_RECEIPT" -> "Gold Received"
        "GOLD_PAYMENT" -> "Gold Issued"
        "RATE_CUT_BUY" -> "Purchase Rate Cut"
        "RATE_CUT_SELL" -> "Sales Rate Cut"
        "ESTIMATE_INVOICE" -> "Estimate Invoice"
        "PURCHASE_ESTIMATE_INVOICE" -> "Purchase Estimate"
        else -> tx.type
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing type
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isDebit) AccentRed.copy(alpha = 0.1f) else AccentGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isGold -> Icons.Default.Brightness5 // custom visual representation for gold/sun
                        isRateCut -> Icons.Default.Gavel
                        tx.type == "ESTIMATE_INVOICE" || tx.type == "PURCHASE_ESTIMATE_INVOICE" -> Icons.Default.Description
                        tx.type.contains("BANK") -> Icons.Default.AccountBalance
                        else -> Icons.Default.AttachMoney
                    },
                    contentDescription = typeLabel,
                    tint = if (isDebit) AccentRed else AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = partyName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = typeLabel, color = TextGray, fontSize = 11.sp)
                    if (tx.purity.isNotEmpty()) {
                        val purityText = tx.purity.toDoubleOrNull()?.let { "${it}%" } ?: tx.purity
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = purityText,
                            color = LuxuryGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(LuxuryGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (tx.remarks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Note: ${tx.remarks}", color = TextGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Values
            Column(horizontalAlignment = Alignment.End) {
                if (isGold || isRateCut || ((tx.type == "ESTIMATE_INVOICE" || tx.type == "PURCHASE_ESTIMATE_INVOICE") && tx.goldWeight > 0)) {
                    Text(
                        text = "${formatWeight(tx.goldWeight)} g",
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                if (tx.amount > 0) {
                    Text(
                        text = "₹${formatAmount(tx.amount)}",
                        color = if (isDebit) AccentRed else AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                if (isRateCut) {
                    Text(
                        text = "@ ₹${formatAmount(tx.rate)}/g",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = formatDate(tx.date), color = TextGray, fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { com.example.utils.PdfExporter.shareTransactionPdf(context, tx, partyName) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share PDF Voucher",
                    tint = LuxuryGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


// --- GOLD & RATE CUTS TAB ---
@Composable
fun RateCutGoldTab(
    transactions: List<LedgerTransaction>,
    parties: List<Party>,
    openingGold: Double = 0.0,
    onAddRateCutClick: (String) -> Unit,
    onAddGoldClick: (String) -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0 = Rate Cuts, 1 = Gold Metal ledger
    val partyMap = remember(parties) { parties.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Inventory & Metal Fixing",
            color = LuxuryGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = subTab,
            containerColor = SurfaceCard,
            contentColor = LuxuryGold,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Rate Cuts (Fixing)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Gold Metal In/Out", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subTab == 0) {
            val rateCuts = transactions.filter { it.type in listOf("RATE_CUT_BUY", "RATE_CUT_SELL") }

            if (rateCuts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No rate cuts recorded. Secure/lock gold rates above.", color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rateCuts) { tx ->
                        val party = partyMap[tx.partyId]
                        TransactionItemCard(tx = tx, partyName = party?.name ?: "Unknown Party")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddRateCutClick("RATE_CUT_BUY") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, LuxuryGold),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, "Buy Fix", tint = LuxuryGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Purchase Rate Cut", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { onAddRateCutClick("RATE_CUT_SELL") },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, "Sell Fix", tint = DarkBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sales Rate Cut", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

        } else {
            val goldTx = transactions.filter { it.type in listOf("GOLD_RECEIPT", "GOLD_PAYMENT") }
            val totalReceived = goldTx.filter { it.type == "GOLD_RECEIPT" }.sumOf { it.goldWeight ?: 0.0 }
            val totalPaid = goldTx.filter { it.type == "GOLD_PAYMENT" }.sumOf { it.goldWeight ?: 0.0 }
            val currentGoldStock = openingGold + totalReceived - totalPaid

            // Gold Metal Book Summary Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opening Raw Gold:", color = TextGray, fontSize = 12.sp)
                Text("${formatWeight(openingGold)} g", color = LuxuryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Gold Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Received", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${formatWeight(totalReceived)} g", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Issued", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${formatWeight(totalPaid)} g", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Current Stock", color = LuxuryGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${formatWeight(currentGoldStock)} g", color = LuxuryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (goldTx.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No Gold receipt/payment recorded. Manage gold flows below.", color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(goldTx) { tx ->
                        val party = partyMap[tx.partyId]
                        TransactionItemCard(tx = tx, partyName = party?.name ?: "Unknown Party")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddGoldClick("GOLD_RECEIPT") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, "Recv Gold")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gold Receipt", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { onAddGoldClick("GOLD_PAYMENT") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, "Issue Gold")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gold Issued", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}


// --- ESTIMATES & NO TAX INVOICES TAB ---
@Composable
fun EstimatesInvoicesTab(
    estimates: List<Estimate>,
    onCreateClick: (Boolean) -> Unit, // passes isPurchase
    onEstimateClick: (Estimate) -> Unit
) {
    var activeSubTab by remember { mutableStateOf("SALES") } // "SALES" or "PURCHASES"

    val filteredEstimates = remember(estimates, activeSubTab) {
        if (activeSubTab == "SALES") {
            estimates.filter { !it.isPurchase }
        } else {
            estimates.filter { it.isPurchase }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Estimate Invoices",
            color = LuxuryGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Toggle selector for Sales vs Purchases
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(8.dp))
                .padding(2.dp)
        ) {
            listOf("SALES" to "Sales Estimates", "PURCHASES" to "Purchase Estimates").forEach { (code, label) ->
                val selected = activeSubTab == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) LuxuryGold else Color.Transparent)
                        .clickable { activeSubTab = code }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) DarkBackground else TextGray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredEstimates.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (activeSubTab == "SALES") "No sales estimates generated yet." else "No supplier purchase estimates added yet.",
                    color = TextGray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEstimates) { est ->
                    val estItems = remember(est.itemsJson) { est.itemsJson.deserializeItems() }
                    val pendingGold = estItems.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
                    Card(
                        onClick = { onEstimateClick(est) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (est.isPurchase) "Purchase Est #: ${est.estimateNumber}" else "Est Invoice #: ${est.estimateNumber}",
                                    color = LuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(est.partyName, color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatDate(est.date), color = TextGray, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${formatAmount(est.totalAmount)}", color = if (est.isPurchase) LuxuryGold else AccentGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (pendingGold > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (est.isPurchase) "- ${String.format("%.3f", pendingGold)}g Gold" else "+ ${String.format("%.3f", pendingGold)}g Gold",
                                        color = LuxuryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Share, "Preview", tint = LuxuryGold, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share / Print", color = LuxuryGold, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onCreateClick(activeSubTab == "PURCHASES") },
            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_estimate_btn")
        ) {
            Icon(Icons.Default.Add, "New Estimate Invoice", tint = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (activeSubTab == "PURCHASES") "Add Supplier Purchase Estimate" else "Generate New Sales Estimate",
                color = DarkBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// --- ORDERS TAB ---
@Composable
fun OrdersTab(
    orders: List<Order>,
    parties: List<Party>,
    onAddOrderClick: () -> Unit,
    onStatusChange: (Order, String) -> Unit,
    onDeleteClick: (Order) -> Unit
) {
    var selectedFilterStatus by remember { mutableStateOf("ALL") } // "ALL", "Pending", "In Progress", "Ready", "Delivered"

    val partyMap = remember(parties) { parties.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Order Pipeline",
            color = LuxuryGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal status scrollbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val states = listOf("ALL", "Pending", "In Progress", "Ready", "Delivered")
            states.forEach { status ->
                val isSelected = selectedFilterStatus == status
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterStatus = status },
                    label = { Text(status) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LuxuryGold,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceCard,
                        labelColor = TextGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selected = isSelected,
                        enabled = true,
                        borderColor = TextGray.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredOrders = orders.filter {
            selectedFilterStatus == "ALL" || it.status == selectedFilterStatus
        }

        if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No orders match selected status.", color = TextGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders) { order ->
                    val party = partyMap[order.partyId]
                    OrderItemCard(
                        order = order,
                        partyName = party?.name ?: "Walk-in Customer",
                        partyPhone = party?.phone ?: "",
                        onStatusChange = { onStatusChange(order, it) },
                        onDelete = { onDeleteClick(order) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onAddOrderClick,
            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_order_btn")
        ) {
            Icon(Icons.Default.ShoppingCart, "New Order", tint = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Customer Order", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrderItemCard(
    order: Order,
    partyName: String,
    partyPhone: String = "",
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expandedStatusMenu by remember { mutableStateOf(false) }

    val statusColor = when(order.status) {
        "Pending" -> Color(0xFFFF9800)
        "In Progress" -> Color(0xFF2196F3)
        "Ready" -> Color(0xFF4CAF50)
        "Delivered" -> TextGray
        else -> TextGray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(partyName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Order Date: ${formatDate(order.orderDate)}", color = TextGray, fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            com.example.utils.PdfExporter.shareOrderPdf(context, order, partyName, partyPhone)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Order PDF",
                            tint = LuxuryGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        Button(
                            onClick = { expandedStatusMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(order.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, "Status Menu", tint = statusColor, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = expandedStatusMenu,
                            onDismissRequest = { expandedStatusMenu = false },
                            modifier = Modifier.background(SurfaceCard)
                        ) {
                            listOf("Pending", "In Progress", "Ready", "Delivered").forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status, color = TextWhite) },
                                    onClick = {
                                        onStatusChange(status)
                                        expandedStatusMenu = false
                                    }
                                )
                            }
                            Divider(color = TextGray.copy(alpha = 0.2f))
                            DropdownMenuItem(
                                text = { Text("Share Order PDF", color = LuxuryGold) },
                                onClick = {
                                    com.example.utils.PdfExporter.shareOrderPdf(context, order, partyName, partyPhone)
                                    expandedStatusMenu = false
                                }
                            )
                            Divider(color = TextGray.copy(alpha = 0.2f))
                            DropdownMenuItem(
                                text = { Text("Delete Order", color = AccentRed) },
                                onClick = {
                                    onDelete()
                                    expandedStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            Text(
                text = "Items: ${order.itemsDescription}",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            if (order.remarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Remarks: ${order.remarks}",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = TextGray.copy(alpha = 0.15f))

            // Footer / Financial details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Locked Rate", color = TextGray, fontSize = 10.sp)
                    Text(
                        text = if (order.agreedRate > 0) "₹${formatAmount(order.agreedRate)}/g" else "Market Rate",
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Advance Paid", color = TextGray, fontSize = 10.sp)
                    Text("₹${formatAmount(order.advanceAmount)}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val daysLeft = ((order.deliveryDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                    val dateStr = formatDate(order.deliveryDate)
                    Text("Delivery Date", color = TextGray, fontSize = 10.sp)
                    Text(
                        text = if (order.status == "Delivered") "Completed" else if (daysLeft < 0) "Overdue ($dateStr)" else "$daysLeft days left ($dateStr)",
                        color = if (order.status == "Delivered") TextGray else if (daysLeft < 0) AccentRed else LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


// ==================== DETAIL DEEP SCREEN: PARTY LEDGER ====================

@Composable
fun PartyLedgerDetailScreen(
    party: Party,
    viewModel: MainViewModel,
    balances: PartyBalance,
    onBack: () -> Unit,
    onAddTx: (String) -> Unit,
    onAddOrder: () -> Unit,
    runWithAdmin: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val txsFlow = remember(party.id) { viewModel.getTransactionsForParty(party.id) }
    val txs by txsFlow.collectAsStateWithLifecycle(emptyList())

    val ordersFlow = remember(party.id) { viewModel.getOrdersForParty(party.id) }
    val orders by ordersFlow.collectAsStateWithLifecycle(emptyList())

    var activeSubSection by remember { mutableStateOf(0) } // 0 = Detailed Ledger, 1 = Orders Pipeline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top back bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = LuxuryGold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = party.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "${party.partyType} | ${party.city} | ${party.phone}", color = TextGray, fontSize = 11.sp)
            }
            IconButton(
                onClick = {
                    com.example.utils.PdfExporter.shareLedgerPdf(
                        context = context,
                        partyName = party.name,
                        partyPhone = party.phone,
                        partyCity = party.city,
                        cashBalance = balances.cashBalance,
                        goldBalance = balances.goldBalance,
                        transactions = txs
                    )
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share PDF Statement", tint = LuxuryGold)
            }
            IconButton(
                onClick = {
                    runWithAdmin {
                        viewModel.deleteParty(party)
                        onBack()
                        Toast.makeText(context, "Party Ledger Deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Ledger", tint = AccentRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Balance Summary Cards Side-by-Side (Cash ledger & Gold Weight ledger!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cash Outstanding Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, if (balances.cashBalance >= 0) AccentGreen.copy(alpha = 0.5f) else AccentRed.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cash Ledger Balance", color = TextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${formatAmount(Math.abs(balances.cashBalance))}",
                        color = if (balances.cashBalance >= 0) AccentGreen else AccentRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (balances.cashBalance >= 0) "RECEIVABLE (Dr)" else "PAYABLE (Cr)",
                        color = if (balances.cashBalance >= 0) AccentGreen else AccentRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gold Weight Outstanding Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Metal Gold Ledger", color = TextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatWeight(Math.abs(balances.goldBalance))} g",
                        color = LuxuryGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (balances.goldBalance >= 0) "GOLD RECEIVABLE" else "GOLD OWED (PAYABLE)",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                com.example.utils.PdfExporter.shareLedgerPdf(
                    context = context,
                    partyName = party.name,
                    partyPhone = party.phone,
                    partyCity = party.city,
                    cashBalance = balances.cashBalance,
                    goldBalance = balances.goldBalance,
                    transactions = txs
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, AccentGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share Statement", tint = AccentGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share PDF Ledger Statement", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selector for detail section
        TabRow(
            selectedTabIndex = activeSubSection,
            containerColor = SurfaceCard,
            contentColor = LuxuryGold,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSubSection == 0,
                onClick = { activeSubSection = 0 },
                text = { Text("Complete Ledger Book", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = activeSubSection == 1,
                onClick = { activeSubSection = 1 },
                text = { Text("Active Orders (${orders.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubSection == 0) {
            // Detailed ledger entries
            if (txs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions found in this party's ledger.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(txs) { tx ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                TransactionItemCard(tx = tx, partyName = party.name)
                            }
                            IconButton(onClick = { runWithAdmin { viewModel.deleteTransaction(tx) } }) {
                                Icon(Icons.Default.DeleteOutline, "Delete Transaction", tint = AccentRed.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction Add Quick Wheel
            Text("Add Ledger Transaction:", color = LuxuryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onAddTx("CASH_IN") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Cash In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddTx("CASH_OUT") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Cash Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddTx("GOLD_RECEIPT") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, LuxuryGold),
                    modifier = Modifier.weight(1.5f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Gold Recv", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddTx("GOLD_PAYMENT") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, TextGray),
                    modifier = Modifier.weight(1.5f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Gold Issue", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onAddTx("RATE_CUT_BUY") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Rate Cut Buy", color = TextWhite, fontSize = 11.sp)
                }
                Button(
                    onClick = { onAddTx("RATE_CUT_SELL") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Rate Cut Sell", color = LuxuryGold, fontSize = 11.sp)
                }
                Button(
                    onClick = { onAddTx("BANK_RECEIPT") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Bank Recv", color = TextGray, fontSize = 11.sp)
                }
                Button(
                    onClick = { onAddTx("BANK_PAYMENT") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Bank Pay", color = TextGray, fontSize = 11.sp)
                }
            }

        } else {
            // Party orders
            if (orders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No active customer orders for this party.", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders) { order ->
                        OrderItemCard(
                            order = order,
                            partyName = party.name,
                            partyPhone = party.phone,
                            onStatusChange = { status ->
                                runWithAdmin {
                                    viewModel.updateOrderStatus(order, status)
                                }
                            },
                            onDelete = { runWithAdmin { viewModel.deleteOrder(order) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAddOrder,
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Add, "New Order")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Place New Order", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==================== DIALOGS & SHEET MODALS ====================

// --- ADD PARTY ---
@Composable
fun AddPartyDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, city: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CUSTOMER") } // "CUSTOMER" or "SUPPLIER"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Ledger Account",
                    color = LuxuryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Party type selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { type = "CUSTOMER" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "CUSTOMER") LuxuryGold else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Customer",
                            color = if (type == "CUSTOMER") DarkBackground else TextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { type = "SUPPLIER" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "SUPPLIER") LuxuryGold else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Supplier",
                            color = if (type == "SUPPLIER") DarkBackground else TextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party Name", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_party_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City / Town", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextGray)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, phone, city, type)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        modifier = Modifier.weight(1.5f),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Account", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- ADD TRANSACTION ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    parties: List<Party>,
    preselectedParty: Party?,
    initialType: String,
    onDismiss: () -> Unit,
    onConfirm: (
        partyId: Int,
        type: String,
        amount: Double,
        goldWeight: Double,
        purity: String,
        rate: Double,
        bankName: String?,
        refNo: String?,
        remarks: String,
        date: Long
    ) -> Unit
) {
    var partyId by remember { mutableStateOf(preselectedParty?.id ?: if (parties.isNotEmpty()) parties.first().id else 0) }
    var type by remember { mutableStateOf(initialType) }

    var amountStr by remember { mutableStateOf("") }
    var goldWeightStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var purityPercentStr by remember { mutableStateOf("91.6") }
    var bankName by remember { mutableStateOf("") }
    var referenceNo by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf<Long?>(null) }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val hasCash = type in listOf("CASH_IN", "CASH_OUT", "BANK_RECEIPT", "BANK_PAYMENT", "RATE_CUT_BUY", "RATE_CUT_SELL")
    val hasGold = type in listOf("GOLD_RECEIPT", "GOLD_PAYMENT", "RATE_CUT_BUY", "RATE_CUT_SELL")
    val isBank = type in listOf("BANK_RECEIPT", "BANK_PAYMENT")
    val isRateCut = type in listOf("RATE_CUT_BUY", "RATE_CUT_SELL")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Record Transaction",
                    color = LuxuryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Party selection dropdown (if not preselected)
                if (preselectedParty == null) {
                    Text("Select Party / Ledger", color = TextGray, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeParty = parties.find { it.id == partyId }
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = BorderStroke(1.dp, TextGray.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(activeParty?.name ?: "No Party Selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, "Select", tint = LuxuryGold)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SurfaceCard).fillMaxWidth(0.85f)
                        ) {
                            parties.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.city})", color = TextWhite) },
                                    onClick = {
                                        partyId = p.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Text(
                        text = "Ledger: ${preselectedParty.name} (${preselectedParty.city})",
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Transaction type label details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when(type) {
                            "CASH_IN" -> "📥 Cash Receipt (Cash In)"
                            "CASH_OUT" -> "📤 Cash Payment (Cash Out)"
                            "BANK_RECEIPT" -> "🏦 Bank Receipt"
                            "BANK_PAYMENT" -> "🏦 Bank Payment"
                            "GOLD_RECEIPT" -> "✨ Gold Receipt (Metal In)"
                            "GOLD_PAYMENT" -> "✨ Gold Issued (Metal Out)"
                            "RATE_CUT_BUY" -> "📈 Gold Purchase Rate Cut"
                            "RATE_CUT_SELL" -> "📉 Gold Sales Rate Cut"
                            else -> type
                        },
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Conditionally visible fields
                if (hasCash) {
                    val label = if (isRateCut) "Total Value calculated (₹)" else "Amount in Rupees (₹)"
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(label, color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (hasGold) {
                    OutlinedTextField(
                        value = goldWeightStr,
                        onValueChange = {
                            goldWeightStr = it
                            // Calculate rate cut total if both rate & weight are entered
                            if (isRateCut) {
                                val rate = rateStr.toDoubleOrNull() ?: 0.0
                                val weight = it.toDoubleOrNull() ?: 0.0
                                if (rate > 0 && weight > 0) {
                                    amountStr = (rate * weight).toString()
                                }
                            }
                        },
                        label = { Text("Gold Weight (Grams)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Purity selection
                    OutlinedTextField(
                        value = purityPercentStr,
                        onValueChange = { purityPercentStr = it },
                        label = { Text("Purity / Touch (%)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("91.6", "99.9", "75.0", "92.0").forEach { p ->
                            val selected = purityPercentStr == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) LuxuryGold else SurfaceCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (selected) LuxuryGold else TextGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { purityPercentStr = p }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$p%",
                                    color = if (selected) DarkBackground else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isRateCut) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = {
                            rateStr = it
                            val rate = it.toDoubleOrNull() ?: 0.0
                            val weight = goldWeightStr.toDoubleOrNull() ?: 0.0
                            if (rate > 0 && weight > 0) {
                                amountStr = (rate * weight).toString()
                            }
                        },
                        label = { Text("Gold Rate (per Gram in ₹)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isBank) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name (e.g., SBI, HDFC)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = referenceNo,
                        onValueChange = { referenceNo = it },
                        label = { Text("Ref / Chq / UPI Transaction No", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val context = LocalContext.current
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = transactionDate ?: System.currentTimeMillis()

                val datePickerDialog = remember(transactionDate) {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val newCal = java.util.Calendar.getInstance()
                            newCal.set(year, month, dayOfMonth)
                            transactionDate = newCal.timeInMillis
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (transactionDate == null) AccentRed else LuxuryGold),
                    border = BorderStroke(1.dp, if (transactionDate == null) AccentRed else TextGray.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = if (transactionDate == null) AccentRed else LuxuryGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (transactionDate != null) {
                            "Transaction Date: " + SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(transactionDate!!))
                        } else {
                            "Select Date * (Compulsory)"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (transactionDate == null) AccentRed else TextWhite
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Description", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextGray)
                    }
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            val wt = goldWeightStr.toDoubleOrNull() ?: 0.0
                            val rt = rateStr.toDoubleOrNull() ?: 0.0
                            val pur = if (hasGold) "$purityPercentStr%" else ""
                            if (partyId > 0 && transactionDate != null) {
                                onConfirm(partyId, type, amt, wt, pur, rt, bankName.ifEmpty { null }, referenceNo.ifEmpty { null }, remarks, transactionDate!!)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        modifier = Modifier.weight(1.5f),
                        enabled = partyId > 0 && (amountStr.isNotBlank() || goldWeightStr.isNotBlank()) && transactionDate != null
                    ) {
                        Text("Confirm Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- CREATE ESTIMATE OR INVOICE FULL SCREEN DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEstimateInvoiceDialog(
    parties: List<Party>,
    type: String, // "ESTIMATE" or "INVOICE"
    isPurchase: Boolean = false,
    editEstimate: Estimate? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        partyId: Int?,
        partyName: String,
        partyPhone: String,
        number: String,
        totalAmount: Double,
        items: List<JewelItem>,
        remarks: String,
        saveAsRegistered: Boolean,
        date: Long
    ) -> Unit
) {
    val filteredParties = remember(parties, isPurchase) {
        if (isPurchase) {
            parties.filter { it.partyType == "SUPPLIER" }
        } else {
            parties.filter { it.partyType == "CUSTOMER" }
        }
    }

    var isLinkedParty by remember { mutableStateOf(editEstimate?.partyId != null && editEstimate.partyId > 0) }
    var selectedPartyId by remember { mutableStateOf(editEstimate?.partyId ?: (if (filteredParties.isNotEmpty()) filteredParties.first().id else 0)) }

    var customName by remember { mutableStateOf(if (editEstimate?.partyId == null) editEstimate?.partyName ?: "" else "") }
    var customPhone by remember { mutableStateOf(editEstimate?.partyPhone ?: "") }
    var saveAsRegisteredParty by remember { mutableStateOf(false) }
    var docNumber by remember { mutableStateOf(editEstimate?.estimateNumber ?: (if (isPurchase) "P-${(100000..999999).random()}" else "E-${(100000..999999).random()}")) }
    var remarks by remember { mutableStateOf(editEstimate?.remarks ?: "") }
    var docDate by remember { mutableStateOf<Long?>(editEstimate?.date) }

    // Item addition fields
    var itemType by remember { mutableStateOf("METAL") } // "METAL" or "PIECE"
    var itemName by remember { mutableStateOf("") }
    var itemGrossWeightStr by remember { mutableStateOf("") }
    var itemStoneWeightStr by remember { mutableStateOf("") }
    var itemWastagePercentStr by remember { mutableStateOf("0") } // default wastage/purity percent is 0%
    var isWastageSaved by remember { mutableStateOf(false) }
    var savedWeight by remember { mutableStateOf(0.0) } // this will represent Net Weight
    var savedGrossWeight by remember { mutableStateOf(0.0) }
    var savedStoneWeight by remember { mutableStateOf(0.0) }
    var savedWastagePercent by remember { mutableStateOf(0.0) }
    var savedFineWeight by remember { mutableStateOf(0.0) }
    var itemPiecesStr by remember { mutableStateOf("") }
    var isRateLocked by remember { mutableStateOf(true) } // Lock/Fix Gold Rate choice
    var itemRateStr by remember { mutableStateOf("") }
    var itemMakingChargesStr by remember { mutableStateOf("") } // per gram charges or fixed
    var itemStoneChargesStr by remember { mutableStateOf("") }
    var itemFreightCostStr by remember { mutableStateOf("") }
    var itemHallmarkingChargesStr by remember { mutableStateOf("") }

    val computedFineWeight: Double = remember(isWastageSaved, savedFineWeight) {
        if (isWastageSaved) savedFineWeight else 0.0
    }

    val computedItemTotal: Double = remember(itemType, isWastageSaved, computedFineWeight, itemRateStr, itemMakingChargesStr, itemPiecesStr, isRateLocked, itemStoneChargesStr, itemFreightCostStr, itemHallmarkingChargesStr) {
        val stoneChg = itemStoneChargesStr.toDoubleOrNull() ?: 0.0
        val freight = itemFreightCostStr.toDoubleOrNull() ?: 0.0
        val hallmark = itemHallmarkingChargesStr.toDoubleOrNull() ?: 0.0
        val baseTotal = if (itemType == "METAL") {
            if (isWastageSaved && isRateLocked) {
                val rate = itemRateStr.toDoubleOrNull() ?: 0.0
                val mc = itemMakingChargesStr.toDoubleOrNull() ?: 0.0
                computedFineWeight * (rate + mc)
            } else {
                0.0
            }
        } else {
            val pcs = itemPiecesStr.toIntOrNull() ?: 0
            val rate = itemRateStr.toDoubleOrNull() ?: 0.0
            pcs.toDouble() * rate
        }
        baseTotal + stoneChg + freight + hallmark
    }

    val initialItems = remember(editEstimate) {
        editEstimate?.itemsJson?.deserializeItems() ?: emptyList()
    }
    val itemsList = remember { 
        val list = mutableStateListOf<JewelItem>()
        list.addAll(initialItems)
        list
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val grandTotal = itemsList.sumOf { it.total }
    val totalFineGoldPending = itemsList.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
    val totalFineGoldLocked = itemsList.filter { it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = LuxuryGold)
                    }
                    Text(
                        text = if (isPurchase) "Create Purchase Estimate" else "Create Estimate Invoice",
                        color = LuxuryGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    TextButton(
                        onClick = {
                            val targetParty = filteredParties.find { it.id == selectedPartyId }
                            if (targetParty != null && itemsList.isNotEmpty() && docDate != null) {
                                onConfirm(targetParty.id, targetParty.name, targetParty.phone, docNumber, grandTotal, itemsList, remarks, false, docDate!!)
                            }
                        },
                        enabled = filteredParties.isNotEmpty() && selectedPartyId > 0 && itemsList.isNotEmpty() && docDate != null
                    ) {
                        Text("Save", color = if (docDate == null || filteredParties.isEmpty() || selectedPartyId <= 0) TextGray else LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Registered Party Row
                Text(
                    text = if (isPurchase) "Select Registered Supplier * (Compulsory)" else "Select Registered Customer * (Compulsory)",
                    color = if (filteredParties.isEmpty() || selectedPartyId <= 0) AccentRed else LuxuryGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (filteredParties.isNotEmpty()) {
                    val activeParty = filteredParties.find { it.id == selectedPartyId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = BorderStroke(1.dp, if (selectedPartyId <= 0) AccentRed else LuxuryGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeParty?.name ?: (if (isPurchase) "Select registered supplier" else "Select registered customer"),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedPartyId <= 0) AccentRed else TextWhite
                                )
                                Icon(Icons.Default.ArrowDropDown, "Select", tint = if (selectedPartyId <= 0) AccentRed else LuxuryGold)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SurfaceCard).fillMaxWidth(0.9f)
                        ) {
                            filteredParties.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.city})", color = TextWhite) },
                                    onClick = {
                                        selectedPartyId = p.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isPurchase) "⚠️ No registered suppliers found. Please add a registered supplier first in the Parties screen." else "⚠️ No registered customers found. Please add a registered customer first in the Parties screen.",
                        color = AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = docNumber,
                    onValueChange = { docNumber = it },
                    label = { Text("${type.lowercase().replaceFirstChar { it.uppercase() }} Serial Number", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                val context = LocalContext.current
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = docDate ?: System.currentTimeMillis()

                val datePickerDialog = remember(docDate) {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val newCal = java.util.Calendar.getInstance()
                            newCal.set(year, month, dayOfMonth)
                            docDate = newCal.timeInMillis
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (docDate == null) AccentRed else LuxuryGold),
                    border = BorderStroke(1.dp, if (docDate == null) AccentRed else TextGray.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = if (docDate == null) AccentRed else LuxuryGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (docDate != null) {
                            "Document Date: " + SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(docDate!!))
                        } else {
                            "Select Document Date * (Compulsory)"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (docDate == null) AccentRed else TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = LuxuryGold.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                // Item Addition Section
                Text("Add Jewellery Item:", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Segmented Tab Row for Item Type Choice
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (itemType == "METAL") LuxuryGold else Color.Transparent)
                            .clickable { itemType = "METAL" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Metal Weight Based",
                            color = if (itemType == "METAL") DarkBackground else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (itemType == "PIECE") LuxuryGold else Color.Transparent)
                            .clickable { itemType = "PIECE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stone / Piece Based",
                            color = if (itemType == "PIECE") DarkBackground else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(if (itemType == "METAL") "Item Details (e.g. Gold Bangle 22K)" else "Item Details (e.g. Single stone)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (itemType == "METAL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = itemGrossWeightStr,
                            onValueChange = { itemGrossWeightStr = it },
                            label = { Text("Gross Weight (g)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                disabledBorderColor = TextGray.copy(alpha = 0.5f),
                                disabledTextColor = TextWhite.copy(alpha = 0.6f)
                            ),
                            singleLine = true,
                            enabled = !isWastageSaved,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = itemStoneWeightStr,
                            onValueChange = { itemStoneWeightStr = it },
                            label = { Text("Stone Weight (g)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                disabledBorderColor = TextGray.copy(alpha = 0.5f),
                                disabledTextColor = TextWhite.copy(alpha = 0.6f)
                            ),
                            singleLine = true,
                            enabled = !isWastageSaved,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val gross = itemGrossWeightStr.toDoubleOrNull() ?: 0.0
                        val stone = itemStoneWeightStr.toDoubleOrNull() ?: 0.0
                        val netWeight = (gross - stone).coerceAtLeast(0.0)
                        val netWeightStr = String.format("%.3f", netWeight)

                        OutlinedTextField(
                            value = netWeightStr,
                            onValueChange = {},
                            label = { Text("Net Weight (g)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = TextGray.copy(alpha = 0.5f),
                                disabledTextColor = TextWhite
                            ),
                            singleLine = true,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = itemWastagePercentStr,
                            onValueChange = { itemWastagePercentStr = it },
                            label = { Text("Wastage / Purity (%)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                disabledBorderColor = TextGray.copy(alpha = 0.5f),
                                disabledTextColor = TextWhite.copy(alpha = 0.6f)
                            ),
                            singleLine = true,
                            enabled = !isWastageSaved,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isWastageSaved) {
                        Button(
                            onClick = {
                                val grossVal = itemGrossWeightStr.toDoubleOrNull() ?: 0.0
                                val stoneVal = itemStoneWeightStr.toDoubleOrNull() ?: 0.0
                                val netWeight = (grossVal - stoneVal).coerceAtLeast(0.0)
                                val wst = itemWastagePercentStr.toDoubleOrNull() ?: 0.0
                                savedGrossWeight = grossVal
                                savedStoneWeight = stoneVal
                                savedWeight = netWeight
                                savedWastagePercent = wst
                                savedFineWeight = netWeight * (wst / 100.0)
                                isWastageSaved = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            enabled = itemGrossWeightStr.isNotBlank() && itemWastagePercentStr.isNotBlank()
                        ) {
                            Icon(Icons.Default.Save, "Save", tint = DarkBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Weight & Wastage", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Saved", tint = AccentGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weight & Wastage Saved!", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { isWastageSaved = false },
                                border = BorderStroke(1.dp, LuxuryGold),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, "Edit", tint = LuxuryGold, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", color = LuxuryGold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Show live calculated pure fine weight (always visible if saved or not, but distinct)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isWastageSaved) SurfaceCard else SurfaceCard.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isWastageSaved) "Saved Pure Fine Weight:" else "Calculated Fine Weight (Preview):", color = TextGray, fontSize = 12.sp)
                            val displayFine = if (isWastageSaved) savedFineWeight else {
                                val gross = itemGrossWeightStr.toDoubleOrNull() ?: 0.0
                                val stone = itemStoneWeightStr.toDoubleOrNull() ?: 0.0
                                val netWeight = (gross - stone).coerceAtLeast(0.0)
                                val wst = itemWastagePercentStr.toDoubleOrNull() ?: 0.0
                                netWeight * (wst / 100.0)
                            }
                            Text("${String.format("%.3f", displayFine)} gm", color = if (isWastageSaved) AccentGreen else LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isWastageSaved) {
                        // Custom lock rate checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRateLocked = !isRateLocked }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.5.dp, LuxuryGold, RoundedCornerShape(4.dp))
                                    .background(if (isRateLocked) LuxuryGold else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRateLocked) {
                                    Icon(Icons.Default.Check, "Checked", tint = DarkBackground, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert Pure Fine into Rate (Lock Rate)", color = TextWhite, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isRateLocked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = itemRateStr,
                                    onValueChange = { itemRateStr = it },
                                    label = { Text("Gold Rate / g fine (₹)", color = TextGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxuryGold,
                                        unfocusedBorderColor = TextGray,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = itemMakingChargesStr,
                                    onValueChange = { itemMakingChargesStr = it },
                                    label = { Text("Making Charge / g (₹)", color = TextGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxuryGold,
                                        unfocusedBorderColor = TextGray,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Item Total Amount: ₹${formatAmount(computedItemTotal)}",
                                color = AccentGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Gold Rate is unfixed. This ${String.format("%.3f", computedFineWeight)}g fine gold will be recorded as pending fine weight balance.",
                                    color = LuxuryGold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    } else {
                        // Help text when not saved
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Click 'Save Weight & Wastage' to lock this calculation and configure gold rates.",
                                color = LuxuryGold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                } else {
                    // PIECE Based
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = itemPiecesStr,
                            onValueChange = { itemPiecesStr = it },
                            label = { Text("Pieces (Qty)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = itemRateStr,
                            onValueChange = { itemRateStr = it },
                            label = { Text("Rate per piece (₹)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Item Total Amount: ₹${formatAmount(computedItemTotal)}",
                        color = AccentGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Optional Item Charges (₹):", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itemStoneChargesStr,
                        onValueChange = { itemStoneChargesStr = it },
                        label = { Text("Stone Chg", color = TextGray, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = itemFreightCostStr,
                        onValueChange = { itemFreightCostStr = it },
                        label = { Text("Freight", color = TextGray, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = itemHallmarkingChargesStr,
                        onValueChange = { itemHallmarkingChargesStr = it },
                        label = { Text("Hallmark", color = TextGray, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxuryGold,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val wt = if (itemType == "METAL") savedWeight else 0.0
                        val wst = if (itemType == "METAL") savedWastagePercent else 100.0
                        val fineWt = if (itemType == "METAL") savedFineWeight else 0.0
                        val rLocked = isRateLocked
                        val rate = itemRateStr.toDoubleOrNull() ?: 0.0
                        val mc = itemMakingChargesStr.toDoubleOrNull() ?: 0.0
                        val pcs = itemPiecesStr.toIntOrNull() ?: 0
                        val stoneChg = itemStoneChargesStr.toDoubleOrNull() ?: 0.0
                        val freight = itemFreightCostStr.toDoubleOrNull() ?: 0.0
                        val hallmark = itemHallmarkingChargesStr.toDoubleOrNull() ?: 0.0

                        val baseVal = if (itemType == "METAL") {
                            if (rLocked) fineWt * (rate + mc) else 0.0
                        } else {
                            pcs.toDouble() * rate
                        }
                        val totalVal = baseVal + stoneChg + freight + hallmark

                        itemsList.add(
                            JewelItem(
                                name = itemName,
                                weight = wt,
                                rate = rate,
                                makingCharges = mc,
                                total = totalVal,
                                itemType = itemType,
                                wastagePercent = wst,
                                pureFineWeight = fineWt,
                                pieces = pcs,
                                isRateLocked = rLocked,
                                grossWeight = if (itemType == "METAL") savedGrossWeight else 0.0,
                                stoneWeight = if (itemType == "METAL") savedStoneWeight else 0.0,
                                stoneCharges = stoneChg,
                                freightCost = freight,
                                hallmarkingCharges = hallmark
                            )
                        )
                        // Clear inputs
                        itemName = ""
                        itemGrossWeightStr = ""
                        itemStoneWeightStr = ""
                        itemWastagePercentStr = "0"
                        isWastageSaved = false
                        savedWeight = 0.0
                        savedGrossWeight = 0.0
                        savedStoneWeight = 0.0
                        savedWastagePercent = 0.0
                        savedFineWeight = 0.0
                        itemPiecesStr = ""
                        itemRateStr = ""
                        itemMakingChargesStr = ""
                        itemStoneChargesStr = ""
                        itemFreightCostStr = ""
                        itemHallmarkingChargesStr = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = if (itemType == "METAL") {
                        itemName.isNotBlank() && isWastageSaved && (!isRateLocked || itemRateStr.isNotBlank())
                    } else {
                        itemName.isNotBlank() && itemPiecesStr.isNotBlank() && itemRateStr.isNotBlank()
                    }
                ) {
                    Icon(Icons.Default.Add, "Add", tint = DarkBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Item to Invoice", color = DarkBackground, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Added Items Table Header
                if (itemsList.isNotEmpty()) {
                    Text("Selected Items List:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    itemsList.forEachIndexed { idx, it ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(it.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (it.itemType == "METAL") {
                                        val weightDetails = "Gross: ${it.grossWeight}g | Stone: ${it.stoneWeight}g | Net: ${it.weight}g"
                                        val chargesDetails = mutableListOf<String>()
                                        if (it.stoneCharges > 0) chargesDetails.add("Stone: ₹${formatAmount(it.stoneCharges)}")
                                        if (it.freightCost > 0) chargesDetails.add("Freight: ₹${formatAmount(it.freightCost)}")
                                        if (it.hallmarkingCharges > 0) chargesDetails.add("Hallmark: ₹${formatAmount(it.hallmarkingCharges)}")
                                        val chargesStr = if (chargesDetails.isNotEmpty()) "\n" + chargesDetails.joinToString(" | ") else ""

                                        if (it.isRateLocked) {
                                            Text(
                                                text = "$weightDetails\nWastage: ${it.wastagePercent}% | Fine: ${String.format("%.3f", it.pureFineWeight)}g\nRate: ₹${formatAmount(it.rate)}/g + ₹${formatAmount(it.makingCharges)} MC$chargesStr",
                                                color = TextGray,
                                                fontSize = 10.sp
                                            )
                                        } else {
                                            Text(
                                                text = "$weightDetails\nWastage: ${it.wastagePercent}% | Fine: ${String.format("%.3f", it.pureFineWeight)}g (Rate Unfixed)$chargesStr",
                                                color = LuxuryGold.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    } else {
                                        val chargesDetails = mutableListOf<String>()
                                        if (it.stoneCharges > 0) chargesDetails.add("Stone: ₹${formatAmount(it.stoneCharges)}")
                                        if (it.freightCost > 0) chargesDetails.add("Freight: ₹${formatAmount(it.freightCost)}")
                                        if (it.hallmarkingCharges > 0) chargesDetails.add("Hallmark: ₹${formatAmount(it.hallmarkingCharges)}")
                                        val chargesStr = if (chargesDetails.isNotEmpty()) "\n" + chargesDetails.joinToString(" | ") else ""
                                        Text("${it.pieces} pcs @ ₹${formatAmount(it.rate)} each$chargesStr", color = TextGray, fontSize = 11.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (it.itemType == "METAL" && !it.isRateLocked) {
                                        Text("${String.format("%.3f", it.pureFineWeight)}g Fine", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Unfixed", color = TextGray, fontSize = 9.sp)
                                    } else {
                                        Text("₹${formatAmount(it.total)}", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { itemsList.removeAt(idx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Remove Item", tint = AccentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Terms / Document Remarks", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Large Total Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Amount Due (Fixed):", color = TextGray, fontSize = 13.sp)
                            Text("₹${formatAmount(grandTotal)}", color = AccentGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        if (totalFineGoldPending > 0) {
                            Divider(color = TextGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pending Fine Gold (Unfixed):", color = LuxuryGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${String.format("%.3f", totalFineGoldPending)} g", color = LuxuryGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


// --- CREATE CUSTOMER ORDER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderDialog(
    parties: List<Party>,
    preselectedParty: Party?,
    onDismiss: () -> Unit,
    onConfirm: (
        partyId: Int,
        deliveryDate: Long,
        itemsDescription: String,
        advanceAmount: Double,
        agreedRate: Double,
        remarks: String
    ) -> Unit
) {
    var partyId by remember { mutableStateOf(preselectedParty?.id ?: if (parties.isNotEmpty()) parties.first().id else 0) }
    var itemsDesc by remember { mutableStateOf("") }
    var advanceAmountStr by remember { mutableStateOf("") }
    var agreedRateStr by remember { mutableStateOf("") }
    var deliveryDaysStr by remember { mutableStateOf("7") } // Days from today
    var remarks by remember { mutableStateOf("") }

    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Customer Order",
                    color = LuxuryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Party selection dropdown
                if (preselectedParty == null) {
                    Text("Select Customer", color = TextGray, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeParty = parties.find { it.id == partyId }
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = BorderStroke(1.dp, TextGray.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(activeParty?.name ?: "No Customer Selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, "Select", tint = LuxuryGold)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(SurfaceCard).fillMaxWidth(0.85f)
                        ) {
                            parties.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.city})", color = TextWhite) },
                                    onClick = {
                                        partyId = p.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Customer: ${preselectedParty.name} (${preselectedParty.city})",
                        color = LuxuryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = itemsDesc,
                    onValueChange = { itemsDesc = it },
                    label = { Text("Order items & specifications", color = TextGray) },
                    placeholder = { Text("e.g. 2 Gold chains 22K (40g total)", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deliveryDaysStr,
                    onValueChange = { deliveryDaysStr = it },
                    label = { Text("Deliver in how many days from now?", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = agreedRateStr,
                    onValueChange = { agreedRateStr = it },
                    label = { Text("Agreed/Locked Gold Rate per Gram (Optional)", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = advanceAmountStr,
                    onValueChange = { advanceAmountStr = it },
                    label = { Text("Advance Amount Paid (₹)", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Order Remarks / Size Details", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextGray)
                    }
                    Button(
                        onClick = {
                            val days = deliveryDaysStr.toLongOrNull() ?: 7
                            val delTimestamp = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                            val advance = advanceAmountStr.toDoubleOrNull() ?: 0.0
                            val rate = agreedRateStr.toDoubleOrNull() ?: 0.0
                            if (partyId > 0 && itemsDesc.isNotBlank()) {
                                onConfirm(partyId, delTimestamp, itemsDesc, advance, rate, remarks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        modifier = Modifier.weight(1.5f),
                        enabled = partyId > 0 && itemsDesc.isNotBlank()
                    ) {
                        Text("Place Order", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- ESTIMATE RECEIPT PREVIEW DIALOG ---
@Composable
fun EstimatePreviewDialog(
    estimate: Estimate,
    onDismiss: () -> Unit,
    onEdit: (Estimate) -> Unit,
    onDelete: (Estimate) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val items = remember(estimate.itemsJson) { estimate.itemsJson.deserializeItems() }

    val receiptText = buildString {
        appendLine(if (estimate.isPurchase) "=== PURCHASE ESTIMATE ===" else "=== ESTIMATE INVOICE ===")
        appendLine("JEWELLEDGER WHOLESALERS")
        appendLine("Date: ${formatDate(estimate.date)}")
        appendLine(if (estimate.isPurchase) "Purchase Est No: ${estimate.estimateNumber}" else "Est Invoice No: ${estimate.estimateNumber}")
        appendLine(if (estimate.isPurchase) "Supplier: ${estimate.partyName}" else "Customer: ${estimate.partyName}")
        appendLine("Phone: ${estimate.partyPhone}")
        appendLine("------------------------")
        items.forEach {
            appendLine("${it.name}")
            if (it.itemType == "METAL") {
                appendLine("  Gross Wt: ${it.grossWeight}g  Stone Wt: ${it.stoneWeight}g")
                appendLine("  Net Wt: ${it.weight}g  Wastage: ${it.wastagePercent}%")
                appendLine("  Pure Fine: ${String.format("%.3f", it.pureFineWeight)}g")
                if (it.isRateLocked) {
                    appendLine("  Gold Rate: ₹${formatAmount(it.rate)}/g")
                    if (it.makingCharges > 0) {
                        appendLine("  MC: ₹${formatAmount(it.makingCharges)}/g")
                    }
                } else {
                    appendLine("  Gold Rate: [Pending Gold Fixing]")
                }
            } else {
                appendLine("  Qty: ${it.pieces} pcs @ ₹${formatAmount(it.rate)}")
            }
            val chgStrings = mutableListOf<String>()
            if (it.stoneCharges > 0) chgStrings.add("Stone Chg: ₹${formatAmount(it.stoneCharges)}")
            if (it.freightCost > 0) chgStrings.add("Freight: ₹${formatAmount(it.freightCost)}")
            if (it.hallmarkingCharges > 0) chgStrings.add("Hallmark: ₹${formatAmount(it.hallmarkingCharges)}")
            if (chgStrings.isNotEmpty()) {
                appendLine("  " + chgStrings.joinToString(" | "))
            }
            appendLine("  Total Amount: ₹${formatAmount(it.total)}")
            appendLine("------------------------")
        }
        val pendingFineGold = items.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
        appendLine("GRAND TOTAL CASH: ₹${formatAmount(estimate.totalAmount)}")
        if (pendingFineGold > 0) {
            appendLine("PENDING FINE GOLD: ${String.format("%.3f", pendingFineGold)}g")
        }
        if (estimate.remarks.isNotEmpty()) {
            appendLine("Remarks: ${estimate.remarks}")
        }
        appendLine("========================")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White), // White classic receipt print color!
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branding Header
                Text(
                    text = "JEWELLEDGER WHOLESALERS",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = if (estimate.isPurchase) "PURCHASE ESTIMATE" else "ESTIMATE INVOICE",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (estimate.isPurchase) "Purchase Est #: ${estimate.estimateNumber}" else "Est Invoice #: ${estimate.estimateNumber}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Date: ${formatDate(estimate.date)}", color = Color.Black, fontSize = 11.sp)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(if (estimate.isPurchase) "Supplier: ${estimate.partyName}" else "Customer: ${estimate.partyName}", color = Color.Black, fontSize = 11.sp)
                }
                if (estimate.partyPhone.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Phone: ${estimate.partyPhone}", color = Color.Black, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Black)

                // Items list
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (item.itemType == "METAL" && !item.isRateLocked) {
                                Text("${String.format("%.3f", item.pureFineWeight)}g Fine (Unfixed)", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("₹${formatAmount(item.total)}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (item.itemType == "METAL") {
                            val wtStr = "Gross: ${item.grossWeight}g | Stone: ${item.stoneWeight}g | Net: ${item.weight}g"
                            Text(
                                text = if (item.isRateLocked) {
                                    "$wtStr\nWastage: ${item.wastagePercent}% | Fine: ${String.format("%.3f", item.pureFineWeight)}g @ ₹${formatAmount(item.rate)}/g (MC: ₹${formatAmount(item.makingCharges)}/g)"
                                } else {
                                    "$wtStr\nWastage: ${item.wastagePercent}% | Fine: ${String.format("%.3f", item.pureFineWeight)}g Fine (Rate Unfixed)"
                                },
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "${item.pieces} pcs @ ₹${formatAmount(item.rate)} each",
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        }
                        // Show extra charges
                        val chgStrings = mutableListOf<String>()
                        if (item.stoneCharges > 0) chgStrings.add("Stone: ₹${formatAmount(item.stoneCharges)}")
                        if (item.freightCost > 0) chgStrings.add("Freight: ₹${formatAmount(item.freightCost)}")
                        if (item.hallmarkingCharges > 0) chgStrings.add("Hallmark: ₹${formatAmount(item.hallmarkingCharges)}")
                        if (chgStrings.isNotEmpty()) {
                            Text(
                                text = chgStrings.joinToString(" | "),
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL CASH AMOUNT:", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("₹${formatAmount(estimate.totalAmount)}", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                val pendingFineGold = items.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
                if (pendingFineGold > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PENDING FINE GOLD:", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.3f", pendingFineGold)} g", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (estimate.remarks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Remarks: ${estimate.remarks}", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.utils.PdfExporter.shareEstimatePdf(context, estimate, items)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, "PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(receiptText))
                                Toast.makeText(context, "Quotation text copied to share!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, LuxuryGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy", tint = LuxuryGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Text", color = LuxuryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onEdit(estimate) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Edit, "Edit", tint = DarkBackground, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onDelete(estimate) },
                            border = BorderStroke(1.dp, AccentRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete Invoice", color = AccentRed, fontSize = 11.sp)
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Close", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


// --- INVOICE RECEIPT PREVIEW DIALOG ---
@Composable
fun InvoicePreviewDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onDelete: (Invoice) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val items = remember(invoice.itemsJson) { invoice.itemsJson.deserializeItems() }

    val receiptText = buildString {
        appendLine("=== NO-TAX INVOICE ===")
        appendLine("JEWELLEDGER WHOLESALERS")
        appendLine("Date: ${formatDate(invoice.date)}")
        appendLine("Invoice No: ${invoice.invoiceNumber}")
        appendLine("Customer: ${invoice.partyName}")
        appendLine("Phone: ${invoice.partyPhone}")
        appendLine("------------------------")
        items.forEach {
            appendLine("${it.name}")
            if (it.itemType == "METAL") {
                appendLine("  Gross Wt: ${it.grossWeight}g  Stone Wt: ${it.stoneWeight}g")
                appendLine("  Net Wt: ${it.weight}g  Wastage: ${it.wastagePercent}%")
                appendLine("  Pure Fine: ${String.format("%.3f", it.pureFineWeight)}g")
                appendLine("  Gold Rate: ₹${formatAmount(it.rate)}/g + ₹${formatAmount(it.makingCharges)} MC")
            } else {
                appendLine("  Qty: ${it.pieces} pcs @ ₹${formatAmount(it.rate)}")
            }
            val chgStrings = mutableListOf<String>()
            if (it.stoneCharges > 0) chgStrings.add("Stone Chg: ₹${formatAmount(it.stoneCharges)}")
            if (it.freightCost > 0) chgStrings.add("Freight: ₹${formatAmount(it.freightCost)}")
            if (it.hallmarkingCharges > 0) chgStrings.add("Hallmark: ₹${formatAmount(it.hallmarkingCharges)}")
            if (chgStrings.isNotEmpty()) {
                appendLine("  " + chgStrings.joinToString(" | "))
            }
            appendLine("  Total: ₹${formatAmount(it.total)}")
            appendLine("------------------------")
        }
        appendLine("INVOICE GRAND TOTAL: ₹${formatAmount(invoice.totalAmount)}")
        if (invoice.remarks.isNotEmpty()) {
            appendLine("Remarks: ${invoice.remarks}")
        }
        appendLine("Note: Non-GST trade summary.")
        appendLine("========================")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branding Header
                Text(
                    text = "JEWELLEDGER WHOLESALERS",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "NON-GST TRADE INVOICE",
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Inv #: ${invoice.invoiceNumber}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Date: ${formatDate(invoice.date)}", color = Color.Black, fontSize = 11.sp)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Customer: ${invoice.partyName}", color = Color.Black, fontSize = 11.sp)
                }
                if (invoice.partyPhone.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Phone: ${invoice.partyPhone}", color = Color.Black, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Black)

                // Items list
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("₹${formatAmount(item.total)}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (item.itemType == "METAL") {
                            val wtStr = "Gross: ${item.grossWeight}g | Stone: ${item.stoneWeight}g | Net: ${item.weight}g"
                            Text(
                                text = "$wtStr\nWastage: ${item.wastagePercent}% | Fine: ${String.format("%.3f", item.pureFineWeight)}g @ ₹${formatAmount(item.rate)}/g (MC: ₹${formatAmount(item.makingCharges)}/g)",
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "${item.pieces} pcs @ ₹${formatAmount(item.rate)} each",
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        }
                        // Show extra charges
                        val chgStrings = mutableListOf<String>()
                        if (item.stoneCharges > 0) chgStrings.add("Stone: ₹${formatAmount(item.stoneCharges)}")
                        if (item.freightCost > 0) chgStrings.add("Freight: ₹${formatAmount(item.freightCost)}")
                        if (item.hallmarkingCharges > 0) chgStrings.add("Hallmark: ₹${formatAmount(item.hallmarkingCharges)}")
                        if (chgStrings.isNotEmpty()) {
                            Text(
                                text = chgStrings.joinToString(" | "),
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BILL AMOUNT DUE:", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("₹${formatAmount(invoice.totalAmount)}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                if (invoice.remarks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Remarks: ${invoice.remarks}", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "*This is a final commercial billing record of sale not subject to GST regulations.",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(receiptText))
                            Toast.makeText(context, "Invoice text copied successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onDelete(invoice) },
                        border = BorderStroke(1.dp, AccentRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete", color = AccentRed, fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Close", color = Color.Black)
                    }
                }
            }
        }
    }
}


// ==================== STRING & NUMBER FORMATTERS ====================

fun formatAmount(value: Double): String {
    return String.format(Locale.getDefault(), "%,.2f", value)
}

fun formatWeight(value: Double): String {
    return String.format(Locale.getDefault(), "%,.3f", value)
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOpeningBalancesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val openingCash by viewModel.openingCash.collectAsStateWithLifecycle()
    val openingBank by viewModel.openingBank.collectAsStateWithLifecycle()
    val openingGold by viewModel.openingGold.collectAsStateWithLifecycle()
    val openingGoldDate by viewModel.openingGoldDate.collectAsStateWithLifecycle()
    val openingItemStocks by viewModel.openingItemStocks.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(0) } // 0 = Financials, 1 = Item Stock, 2 = Admin & Security

    var adminAuthenticatedLocal by remember { mutableStateOf(true) }
    var adminAuthPasswordInput by remember { mutableStateOf("") }
    var adminAuthError by remember { mutableStateOf(false) }

    // Financial inputs
    var cashInput by remember(openingCash) { mutableStateOf(if (openingCash == 0.0) "" else openingCash.toString()) }
    var bankInput by remember(openingBank) { mutableStateOf(if (openingBank == 0.0) "" else openingBank.toString()) }
    var goldInput by remember(openingGold) { mutableStateOf(if (openingGold == 0.0) "" else openingGold.toString()) }

    // Item inputs
    var newItemName by remember { mutableStateOf("") }
    var newItemWeight by remember { mutableStateOf("") }
    var newItemPieces by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val cashVal = cashInput.toDoubleOrNull() ?: 0.0
                    val bankVal = bankInput.toDoubleOrNull() ?: 0.0
                    val goldVal = goldInput.toDoubleOrNull() ?: 0.0
                    viewModel.updateOpeningCash(cashVal)
                    viewModel.updateOpeningBank(bankVal)
                    viewModel.updateOpeningGold(goldVal)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        title = {
            Text(
                text = "Opening Balances & Settings",
                color = LuxuryGold,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        containerColor = DarkBackground,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = SurfaceCard,
                    contentColor = LuxuryGold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Financials", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Item / Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Admin & Security", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    val context = LocalContext.current
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text("Set opening balances for your ledger books.", color = TextGray, fontSize = 11.sp)

                        // Cash Input
                        OutlinedTextField(
                            value = cashInput,
                            onValueChange = { cashInput = it },
                            label = { Text("Opening Cash Balance (₹)", color = TextGray) },
                            placeholder = { Text("0.00", color = TextGray.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = SurfaceCard,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bank Input
                        OutlinedTextField(
                            value = bankInput,
                            onValueChange = { bankInput = it },
                            label = { Text("Opening Bank Balance (₹)", color = TextGray) },
                            placeholder = { Text("0.00", color = TextGray.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = SurfaceCard,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Raw Gold Input
                        val goldIsSet = openingGold > 0.0
                        OutlinedTextField(
                            value = goldInput,
                            onValueChange = { goldInput = it },
                            label = { Text("Opening Raw Gold Stock (grams)", color = TextGray) },
                            placeholder = { Text("0.000", color = TextGray.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !goldIsSet,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = SurfaceCard,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                disabledBorderColor = SurfaceCard.copy(alpha = 0.5f),
                                disabledTextColor = TextWhite.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (goldIsSet) {
                            val formattedDate = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(openingGoldDate))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LuxuryGold.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "🔒 Opening Gold Set: $openingGold grams",
                                        color = LuxuryGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Configured on $formattedDate. Opening gold can only be configured once at the financial year start. Otherwise, we can only adjust active balance below.",
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = LuxuryGold.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Balance Adjustments (Expenses / Corrections)", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Adjust active cash, bank, or gold balances. Adjustments outwards are recorded as operating expenses.", color = TextGray, fontSize = 10.sp)

                        var adjustAsset by remember { mutableStateOf("CASH") } // "CASH", "BANK", "GOLD"
                        var adjustType by remember { mutableStateOf("OUT") } // "OUT" (Reduce), "IN" (Increase)
                        var adjustAmountStr by remember { mutableStateOf("") }
                        var adjustRemarks by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
                        ) {
                            listOf("CASH", "BANK", "GOLD").forEach { asset ->
                                val isSelected = adjustAsset == asset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) LuxuryGold else SurfaceCard)
                                        .clickable { adjustAsset = asset }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(asset, color = if (isSelected) DarkBackground else TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
                        ) {
                            listOf("OUT" to "Reduce Balance (-)", "IN" to "Increase Balance (+)").forEach { (type, label) ->
                                val isSelected = adjustType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) (if (type == "OUT") AccentRed else AccentGreen) else SurfaceCard)
                                        .clickable { adjustType = type }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) DarkBackground else TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = adjustAmountStr,
                            onValueChange = { adjustAmountStr = it },
                            label = { Text(if (adjustAsset == "GOLD") "Adjustment Weight (grams)" else "Adjustment Amount (₹)", color = TextGray, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = SurfaceCard,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = adjustRemarks,
                            onValueChange = { adjustRemarks = it },
                            label = { Text("Adjustment Reason (Remarks)", color = TextGray, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxuryGold,
                                unfocusedBorderColor = SurfaceCard,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val value = adjustAmountStr.toDoubleOrNull() ?: 0.0
                                if (value > 0.0 && adjustRemarks.isNotBlank()) {
                                    val typeCode = when (adjustAsset) {
                                        "CASH" -> if (adjustType == "OUT") "CASH_OUT" else "CASH_IN"
                                        "BANK" -> if (adjustType == "OUT") "BANK_PAYMENT" else "BANK_RECEIPT"
                                        else -> if (adjustType == "OUT") "GOLD_PAYMENT" else "GOLD_RECEIPT"
                                    }
                                    viewModel.addTransaction(
                                        partyId = 0,
                                        type = typeCode,
                                        amount = if (adjustAsset != "GOLD") value else 0.0,
                                        goldWeight = if (adjustAsset == "GOLD") value else 0.0,
                                        purity = if (adjustAsset == "GOLD") "24K (99.9)" else "",
                                        rate = 0.0,
                                        remarks = "Balance Adjustment: $adjustRemarks",
                                        bankName = if (adjustAsset == "BANK") "General" else null
                                    )
                                    adjustAmountStr = ""
                                    adjustRemarks = ""
                                    Toast.makeText(context, "Balance adjusted and recorded as transaction", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount and reason!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply Balance Adjustment", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else if (activeTab == 1) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Add Item Form
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Add Opening Item Stock", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                
                                OutlinedTextField(
                                    value = newItemName,
                                    onValueChange = { newItemName = it },
                                    label = { Text("Item Name", color = TextGray, fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Gold Ring", color = TextGray.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxuryGold,
                                        unfocusedBorderColor = DarkBackground,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newItemWeight,
                                        onValueChange = { newItemWeight = it },
                                        label = { Text("Weight (g)", color = TextGray, fontSize = 11.sp) },
                                        placeholder = { Text("0.00", color = TextGray.copy(alpha = 0.5f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LuxuryGold,
                                            unfocusedBorderColor = DarkBackground,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = newItemPieces,
                                        onValueChange = { newItemPieces = it },
                                        label = { Text("Pieces", color = TextGray, fontSize = 11.sp) },
                                        placeholder = { Text("0", color = TextGray.copy(alpha = 0.5f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LuxuryGold,
                                            unfocusedBorderColor = DarkBackground,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (newItemName.isNotBlank()) {
                                            val wt = newItemWeight.toDoubleOrNull() ?: 0.0
                                            val pcs = newItemPieces.toIntOrNull() ?: 0
                                            viewModel.updateOpeningItemStock(newItemName, wt, pcs)
                                            newItemName = ""
                                            newItemWeight = ""
                                            newItemPieces = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add to Opening Stock", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Existing Opening Stock:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        if (openingItemStocks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No opening item stock configured yet.", color = TextGray, fontSize = 11.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(openingItemStocks.keys.toList().sorted()) { name ->
                                    val pair = openingItemStocks[name] ?: Pair(0.0, 0)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceCard, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Wt: ${pair.first}g | Pcs: ${pair.second}", color = TextGray, fontSize = 10.sp)
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeOpeningItemStock(name) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete", tint = AccentRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val context = LocalContext.current
                    if (!adminAuthenticatedLocal) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Authentication Required", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Please enter the admin password to access security settings and audit logs.", color = TextGray, fontSize = 11.sp)

                            OutlinedTextField(
                                value = adminAuthPasswordInput,
                                onValueChange = { 
                                    adminAuthPasswordInput = it
                                    adminAuthError = false
                                },
                                label = { Text("Admin Password", color = TextGray, fontSize = 11.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = SurfaceCard,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (adminAuthError) {
                                Text("Incorrect password!", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (adminAuthPasswordInput == viewModel.adminPassword.value) {
                                        adminAuthenticatedLocal = true
                                        adminAuthError = false
                                    } else {
                                        adminAuthError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Authenticate", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Update Admin Password", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            var newPasswordInput by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = { newPasswordInput = it },
                                label = { Text("New Password", color = TextGray, fontSize = 11.sp) },
                                placeholder = { Text("Enter new admin password", color = TextGray.copy(alpha = 0.5f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = SurfaceCard,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newPasswordInput.isNotBlank()) {
                                        viewModel.updateAdminPassword(newPasswordInput)
                                        Toast.makeText(context, "Admin password updated successfully!", Toast.LENGTH_SHORT).show()
                                        newPasswordInput = ""
                                    } else {
                                        Toast.makeText(context, "Password cannot be empty!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Divider(color = SurfaceCard)

                            Text("Database Backup & Restore", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Secure your data by exporting to Google Drive or other cloud services, or restore an existing backup.", color = TextGray, fontSize = 10.sp)

                            Button(
                                onClick = {
                                    scope.launch {
                                        val backupStr = viewModel.exportBackupString()
                                        shareBackup(context, backupStr)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = DarkBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup to Google Drive / Cloud Share", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            var importText by remember { mutableStateOf("") }

                            OutlinedTextField(
                                value = importText,
                                onValueChange = { importText = it },
                                label = { Text("Paste Backup Code to Restore", color = TextGray, fontSize = 11.sp) },
                                placeholder = { Text("Paste exported backup code here...", color = TextGray.copy(alpha = 0.4f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = SurfaceCard,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (importText.isNotBlank()) {
                                        scope.launch {
                                            val success = viewModel.restoreBackupString(importText)
                                            if (success) {
                                                Toast.makeText(context, "Ledger restored successfully!", Toast.LENGTH_LONG).show()
                                                importText = ""
                                            } else {
                                                Toast.makeText(context, "Failed to restore backup. Invalid code format.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please paste a valid backup code first.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Restore Backup Code", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Divider(color = SurfaceCard)

                            Text("Deleted Records Audit Logs", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Tracked historical deletions of parties, transactions, and estimates.", color = TextGray, fontSize = 10.sp)

                            val logsContent by viewModel.deletedLogContent.collectAsStateWithLifecycle()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = logsContent,
                                    color = TextWhite,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.clearDeletedLog()
                                    Toast.makeText(context, "Log history cleared successfully", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Log History", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AdminPasswordPromptDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: MainViewModel
) {
    var passwordInput by remember { mutableStateOf("") }
    var errorState by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Authentication Required", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This action is restricted to Admins. Please enter the Admin Password to continue.", color = TextGray, fontSize = 12.sp)
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorState = false
                    },
                    label = { Text("Admin Password", color = TextGray) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorState) {
                    Text("Incorrect Password!", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passwordInput == viewModel.adminPassword.value) {
                        onSuccess()
                    } else {
                        errorState = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = DarkBackground)
            ) {
                Text("Unlock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = DarkBackground
    )
}

fun shareBackup(context: android.content.Context, backupContent: String) {
    try {
        val file = java.io.File(context.cacheDir, "jewel_ledger_backup.jewelbackup")
        file.writeText(backupContent)
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Jewel Ledger Database Backup")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(intent, "Backup via Google Drive / Cloud Share"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing backup: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

