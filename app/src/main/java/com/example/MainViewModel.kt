package com.example

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PartyBalance(
    val cashBalance: Double, // Positive = Receivable (debit), Negative = Payable (credit)
    val goldBalance: Double  // Positive = Gold Receivable (grams), Negative = Gold Payable (grams)
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: JewelRepository

    private val prefs = application.getSharedPreferences("jewel_ledger_prefs", Context.MODE_PRIVATE)

    private val _adminPassword = MutableStateFlow(prefs.getString("admin_password", "1234") ?: "1234")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    fun updateAdminPassword(newPassword: String) {
        prefs.edit().putString("admin_password", newPassword).apply()
        _adminPassword.value = newPassword
    }

    private val _isAdminMode = MutableStateFlow(true)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    fun setAdminMode(enabled: Boolean) {
        _isAdminMode.value = enabled
    }

    private val _openingGoldDate = MutableStateFlow(prefs.getLong("opening_gold_date", 0L))
    val openingGoldDate: StateFlow<Long> = _openingGoldDate.asStateFlow()

    private val _openingCash = MutableStateFlow(prefs.getFloat("opening_cash", 0f).toDouble())
    val openingCash: StateFlow<Double> = _openingCash.asStateFlow()

    private val _openingBank = MutableStateFlow(prefs.getFloat("opening_bank", 0f).toDouble())
    val openingBank: StateFlow<Double> = _openingBank.asStateFlow()

    private val _openingGold = MutableStateFlow(prefs.getFloat("opening_gold", 0f).toDouble())
    val openingGold: StateFlow<Double> = _openingGold.asStateFlow()

    private val _openingItemStocks = MutableStateFlow<Map<String, Pair<Double, Int>>>(loadOpeningItemStocks())
    val openingItemStocks: StateFlow<Map<String, Pair<Double, Int>>> = _openingItemStocks.asStateFlow()

    private fun loadOpeningItemStocks(): Map<String, Pair<Double, Int>> {
        val map = mutableMapOf<String, Pair<Double, Int>>()
        val itemNamesSet = prefs.getStringSet("opening_item_names", emptySet()) ?: emptySet()
        itemNamesSet.forEach { name ->
            val wt = prefs.getFloat("opening_item_wt_$name", 0f).toDouble()
            val pcs = prefs.getInt("opening_item_pcs_$name", 0)
            map[name] = Pair(wt, pcs)
        }
        return map
    }

    fun updateOpeningCash(value: Double) {
        prefs.edit().putFloat("opening_cash", value.toFloat()).apply()
        _openingCash.value = value
    }

    fun updateOpeningBank(value: Double) {
        prefs.edit().putFloat("opening_bank", value.toFloat()).apply()
        _openingBank.value = value
    }

    fun updateOpeningGold(value: Double, date: Long = System.currentTimeMillis()) {
        val editor = prefs.edit().putFloat("opening_gold", value.toFloat())
        if (value > 0.0 && prefs.getLong("opening_gold_date", 0L) == 0L) {
            editor.putLong("opening_gold_date", date)
            _openingGoldDate.value = date
        }
        editor.apply()
        _openingGold.value = value
    }

    fun updateOpeningItemStock(name: String, weight: Double, pieces: Int) {
        val normalizedName = name.trim().lowercase().capitalize()
        val currentNames = (prefs.getStringSet("opening_item_names", emptySet()) ?: emptySet()).toMutableSet()
        currentNames.add(normalizedName)
        
        prefs.edit()
            .putStringSet("opening_item_names", currentNames)
            .putFloat("opening_item_wt_$normalizedName", weight.toFloat())
            .putInt("opening_item_pcs_$normalizedName", pieces)
            .apply()
            
        val updatedMap = _openingItemStocks.value.toMutableMap()
        updatedMap[normalizedName] = Pair(weight, pieces)
        _openingItemStocks.value = updatedMap
    }

    fun removeOpeningItemStock(name: String) {
        val normalizedName = name.trim().lowercase().capitalize()
        val currentNames = (prefs.getStringSet("opening_item_names", emptySet()) ?: emptySet()).toMutableSet()
        currentNames.remove(normalizedName)
        
        prefs.edit()
            .putStringSet("opening_item_names", currentNames)
            .remove("opening_item_wt_$normalizedName")
            .remove("opening_item_pcs_$normalizedName")
            .apply()
            
        val updatedMap = _openingItemStocks.value.toMutableMap()
        updatedMap.remove(normalizedName)
        _openingItemStocks.value = updatedMap
    }

    val logFile: File by lazy {
        File(getApplication<Application>().filesDir, "deleted_items_log.txt")
    }

    private val _deletedLogContent = MutableStateFlow("")
    val deletedLogContent: StateFlow<String> = _deletedLogContent.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = JewelRepository(database)
        loadDeletedLog()
    }

    fun loadDeletedLog() {
        viewModelScope.launch {
            if (!logFile.exists()) {
                _deletedLogContent.value = "No deletions logged yet."
            } else {
                try {
                    _deletedLogContent.value = logFile.readText()
                } catch (e: Exception) {
                    _deletedLogContent.value = "Error reading log: ${e.message}"
                }
            }
        }
    }

    private fun logDeletion(entry: String) {
        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timestamp = sdf.format(Date())
                val formattedEntry = """
                    
==================================================
DELETED RECORD AUDIT LOG - $timestamp
==================================================
$entry
--------------------------------------------------
                """.trimIndent()
                
                logFile.appendText(formattedEntry + "\n\n")
                loadDeletedLog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearDeletedLog() {
        viewModelScope.launch {
            try {
                if (logFile.exists()) {
                    logFile.delete()
                }
                loadDeletedLog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Expose DB Flows as state flows
    val parties: StateFlow<List<Party>> = repository.allParties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<LedgerTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashTransactions: StateFlow<List<LedgerTransaction>> = repository.cashTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankTransactions: StateFlow<List<LedgerTransaction>> = repository.bankTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estimates: StateFlow<List<Estimate>> = repository.allEstimates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic aggregated outstanding balances per party (Cash in Rs, Gold in grams)
    val partyBalances: StateFlow<Map<Int, PartyBalance>> = combine(transactions, estimates) { txList, estList ->
        val map = mutableMapOf<Int, PartyBalance>()
        txList.forEach { tx ->
            if (tx.partyId != 0) {
                val current = map[tx.partyId] ?: PartyBalance(0.0, 0.0)
                var cashDiff = 0.0
                var goldDiff = 0.0
                when (tx.type) {
                    "CASH_OUT", "BANK_PAYMENT" -> cashDiff = tx.amount
                    "CASH_IN", "BANK_RECEIPT" -> cashDiff = -tx.amount
                    "GOLD_PAYMENT" -> goldDiff = tx.goldWeight
                    "GOLD_RECEIPT" -> goldDiff = -tx.goldWeight
                    "RATE_CUT_SELL" -> {
                        cashDiff = tx.amount
                        goldDiff = -tx.goldWeight
                    }
                    "RATE_CUT_BUY" -> {
                        cashDiff = -tx.amount
                        goldDiff = tx.goldWeight
                    }
                }
                map[tx.partyId] = PartyBalance(
                    cashBalance = current.cashBalance + cashDiff,
                    goldBalance = current.goldBalance + goldDiff
                )
            }
        }
        estList.forEach { est ->
            val pId = est.partyId
            if (pId != null && pId != 0) {
                val current = map[pId] ?: PartyBalance(0.0, 0.0)
                val estItems = est.itemsJson.deserializeItems()
                val pendingGold = estItems.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
                if (est.isPurchase) {
                    map[pId] = PartyBalance(
                        cashBalance = current.cashBalance - est.totalAmount, // Purchase estimate reduces receivable / increases payable
                        goldBalance = current.goldBalance - pendingGold      // Purchase estimate reduces gold receivable
                    )
                } else {
                    map[pId] = PartyBalance(
                        cashBalance = current.cashBalance + est.totalAmount, // Invoice adds to what is receivable (Dr)
                        goldBalance = current.goldBalance + pendingGold      // Unlocked gold adds to gold receivable
                    )
                }
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered transaction flows for a specific party
    fun getTransactionsForParty(partyId: Int): Flow<List<LedgerTransaction>> {
        val dbTxs = repository.getTransactionsForParty(partyId)
        val partyEstimates = estimates.map { list -> list.filter { it.partyId == partyId } }
        return combine(dbTxs, partyEstimates) { txList, estList ->
            val syntheticTxs = estList.map { est ->
                val estItems = est.itemsJson.deserializeItems()
                val pendingGold = estItems.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
                LedgerTransaction(
                    id = -est.id,
                    partyId = partyId,
                    date = est.date,
                    type = if (est.isPurchase) "PURCHASE_ESTIMATE_INVOICE" else "ESTIMATE_INVOICE",
                    amount = est.totalAmount,
                    goldWeight = pendingGold,
                    purity = if (pendingGold > 0) "Fine" else "",
                    rate = 0.0,
                    remarks = (if (est.isPurchase) "Purchase Estimate #" else "Estimate Invoice #") + est.estimateNumber + if (est.remarks.isNotEmpty()) " - ${est.remarks}" else ""
                )
            }
            (txList + syntheticTxs).sortedByDescending { it.date }
        }
    }

    fun getOrdersForParty(partyId: Int): Flow<List<Order>> {
        return repository.getOrdersForParty(partyId)
    }

    // Cash Book summary
    val cashSummary: StateFlow<Pair<Double, Double>> = cashTransactions.map { list ->
        var totalIn = 0.0
        var totalOut = 0.0
        list.forEach {
            if (it.type == "CASH_IN") totalIn += it.amount
            else if (it.type == "CASH_OUT") totalOut += it.amount
        }
        Pair(totalIn, totalOut)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // Bank Book summary
    val bankSummary: StateFlow<Pair<Double, Double>> = bankTransactions.map { list ->
        var totalReceipts = 0.0
        var totalPayments = 0.0
        list.forEach {
            if (it.type == "BANK_RECEIPT") totalReceipts += it.amount
            else if (it.type == "BANK_PAYMENT") totalPayments += it.amount
        }
        Pair(totalReceipts, totalPayments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // DB Operations
    fun addParty(name: String, phone: String, city: String, partyType: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val party = Party(name = name, phone = phone, city = city, partyType = partyType)
            val id = repository.insertParty(party)
            onComplete(id)
        }
    }

    fun deleteParty(party: Party) {
        viewModelScope.launch {
            val entry = """
                TYPE: PARTY (LEDGER ACCOUNT)
                Party ID: ${party.id}
                Name: ${party.name}
                Phone: ${party.phone}
                City: ${party.city}
                Account Type: ${party.partyType}
                Created On: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(party.createdAt))}
            """.trimIndent()
            logDeletion(entry)
            repository.deleteParty(party)
        }
    }

    fun addTransaction(
        partyId: Int,
        type: String,
        amount: Double,
        goldWeight: Double,
        purity: String,
        rate: Double,
        bankName: String? = null,
        referenceNo: String? = null,
        remarks: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val tx = LedgerTransaction(
                partyId = partyId,
                type = type,
                amount = amount,
                goldWeight = goldWeight,
                purity = purity,
                rate = rate,
                bankName = bankName,
                referenceNo = referenceNo,
                remarks = remarks,
                date = date
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: LedgerTransaction) {
        viewModelScope.launch {
            if (transaction.id < 0) {
                val estimateId = -transaction.id
                val estimate = estimates.value.find { it.id == estimateId }
                if (estimate != null) {
                    deleteEstimate(estimate)
                }
            } else {
                val partyName = parties.value.find { it.id == transaction.partyId }?.name ?: "General Cash/Bank"
                val entry = """
                    TYPE: LEDGER TRANSACTION
                    Transaction ID: ${transaction.id}
                    Party: $partyName (ID: ${transaction.partyId})
                    Date of Transaction: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.date))}
                    Transaction Type: ${transaction.type}
                    Cash Amount: ₹${transaction.amount}
                    Metal Weight: ${transaction.goldWeight} g Fine
                    Purity: ${transaction.purity}
                    Rate: ₹${transaction.rate} / g
                    Bank/Reference: ${transaction.bankName ?: ""}${if (transaction.referenceNo != null) " (Ref: ${transaction.referenceNo})" else ""}
                    Remarks: ${transaction.remarks}
                """.trimIndent()
                logDeletion(entry)
                repository.deleteTransaction(transaction)
            }
        }
    }

    fun addEstimate(
        partyId: Int?,
        partyName: String,
        partyPhone: String,
        estimateNumber: String,
        totalAmount: Double,
        items: List<JewelItem>,
        remarks: String,
        isPurchase: Boolean = false,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val est = Estimate(
                partyId = partyId,
                partyName = partyName,
                partyPhone = partyPhone,
                date = date,
                estimateNumber = estimateNumber,
                totalAmount = totalAmount,
                itemsJson = items.serialize(),
                remarks = remarks,
                isPurchase = isPurchase
            )
            repository.insertEstimate(est)
        }
    }

    fun updateEstimate(
        id: Int,
        partyId: Int?,
        partyName: String,
        partyPhone: String,
        estimateNumber: String,
        totalAmount: Double,
        items: List<JewelItem>,
        remarks: String,
        isPurchase: Boolean,
        date: Long
    ) {
        viewModelScope.launch {
            val est = Estimate(
                id = id,
                partyId = partyId,
                partyName = partyName,
                partyPhone = partyPhone,
                date = date,
                estimateNumber = estimateNumber,
                totalAmount = totalAmount,
                itemsJson = items.serialize(),
                remarks = remarks,
                isPurchase = isPurchase
            )
            repository.insertEstimate(est)
        }
    }

    fun deleteEstimate(estimate: Estimate) {
        viewModelScope.launch {
            val itemsStr = try {
                val items = estimate.itemsJson.deserializeItems()
                items.joinToString("\n") { item ->
                    "  * ${item.name} | ${item.weight}g | Type: ${item.itemType} | Total: ₹${item.total}"
                }
            } catch (e: Exception) {
                "Error parsing items"
            }
            val entry = """
                TYPE: ${if (estimate.isPurchase) "PURCHASE ESTIMATE" else "ESTIMATE INVOICE"}
                Estimate ID: ${estimate.id}
                Estimate/Invoice No: ${estimate.estimateNumber}
                Party Name: ${estimate.partyName}
                Party Phone: ${estimate.partyPhone}
                Date of Estimate: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(estimate.date))}
                Total Amount: ₹${estimate.totalAmount}
                Remarks: ${estimate.remarks}
                Items List:
                $itemsStr
            """.trimIndent()
            logDeletion(entry)
            repository.deleteEstimate(estimate)
        }
    }

    fun addInvoice(
        partyId: Int?,
        partyName: String,
        partyPhone: String,
        invoiceNumber: String,
        totalAmount: Double,
        items: List<JewelItem>,
        remarks: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val inv = Invoice(
                partyId = partyId,
                partyName = partyName,
                partyPhone = partyPhone,
                date = date,
                invoiceNumber = invoiceNumber,
                totalAmount = totalAmount,
                itemsJson = items.serialize(),
                remarks = remarks
            )
            repository.insertInvoice(inv)
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            val itemsStr = try {
                val items = invoice.itemsJson.deserializeItems()
                items.joinToString("\n") { item ->
                    "  * ${item.name} | ${item.weight}g | Type: ${item.itemType} | Total: ₹${item.total}"
                }
            } catch (e: Exception) {
                "Error parsing items"
            }
            val entry = """
                TYPE: TAX INVOICE
                Invoice ID: ${invoice.id}
                Invoice No: ${invoice.invoiceNumber}
                Party Name: ${invoice.partyName}
                Party Phone: ${invoice.partyPhone}
                Date of Invoice: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(invoice.date))}
                Total Amount: ₹${invoice.totalAmount}
                Remarks: ${invoice.remarks}
                Items List:
                $itemsStr
            """.trimIndent()
            logDeletion(entry)
            repository.deleteInvoice(invoice)
        }
    }

    fun addOrder(
        partyId: Int,
        deliveryDate: Long,
        itemsDescription: String,
        advanceAmount: Double,
        agreedRate: Double,
        remarks: String,
        orderDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val order = Order(
                partyId = partyId,
                orderDate = orderDate,
                deliveryDate = deliveryDate,
                status = "Pending",
                itemsDescription = itemsDescription,
                advanceAmount = advanceAmount,
                agreedRate = agreedRate,
                remarks = remarks
            )
            repository.insertOrder(order)
        }
    }

    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            val updated = order.copy(status = newStatus)
            repository.insertOrder(updated)
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            val partyName = parties.value.find { it.id == order.partyId }?.name ?: "Unknown Party"
            val entry = """
                TYPE: CUSTOM ORDER
                Order ID: ${order.id}
                Customer Name: $partyName (ID: ${order.partyId})
                Order Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(order.orderDate))}
                Delivery Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(order.deliveryDate))}
                Status: ${order.status}
                Description: ${order.itemsDescription}
                Advance Amount: ₹${order.advanceAmount}
                Locked Rate: ₹${order.agreedRate}
                Remarks: ${order.remarks}
            """.trimIndent()
            logDeletion(entry)
            repository.deleteOrder(order)
        }
    }

    suspend fun exportBackupString(): String {
        val sb = StringBuilder()
        
        sb.append("--- JEWEL LEDGER BACKUP v1 ---\n")
        
        sb.append("[PREFS]\n")
        sb.append("opening_cash=${prefs.getFloat("opening_cash", 0f)}\n")
        sb.append("opening_bank=${prefs.getFloat("opening_bank", 0f)}\n")
        sb.append("opening_gold=${prefs.getFloat("opening_gold", 0f)}\n")
        sb.append("opening_gold_date=${prefs.getLong("opening_gold_date", 0L)}\n")
        sb.append("admin_password=${prefs.getString("admin_password", "1234")}\n")
        sb.append("[END_PREFS]\n")
        
        val parties = repository.allParties.first()
        val txs = repository.allTransactions.first()
        val ests = repository.allEstimates.first()
        val invs = repository.allInvoices.first()
        val ords = repository.allOrders.first()
        
        sb.append("[PARTIES]\n")
        parties.forEach { p ->
            sb.append("${p.id}:::;:::${p.name}:::;:::${p.phone}:::;:::${p.city}:::;:::${p.partyType}:::;:::${p.createdAt}\n")
        }
        sb.append("[END_PARTIES]\n")
        
        sb.append("[TRANSACTIONS]\n")
        txs.forEach { t ->
            val bank = t.bankName ?: ""
            val ref = t.referenceNo ?: ""
            sb.append("${t.id}:::;:::${t.partyId}:::;:::${t.date}:::;:::${t.type}:::;:::${t.amount}:::;:::${t.goldWeight}:::;:::${t.purity}:::;:::${t.rate}:::;:::${bank}:::;:::${ref}:::;:::${t.remarks}\n")
        }
        sb.append("[END_TRANSACTIONS]\n")
        
        sb.append("[ESTIMATES]\n")
        ests.forEach { e ->
            val pId = e.partyId ?: -1
            sb.append("${e.id}:::;:::${pId}:::;:::${e.partyName}:::;:::${e.partyPhone}:::;:::${e.date}:::;:::${e.estimateNumber}:::;:::${e.totalAmount}:::;:::${e.itemsJson}:::;:::${e.remarks}:::;:::${e.isPurchase}\n")
        }
        sb.append("[END_ESTIMATES]\n")
        
        sb.append("[INVOICES]\n")
        invs.forEach { i ->
            val pId = i.partyId ?: -1
            sb.append("${i.id}:::;:::${pId}:::;:::${i.partyName}:::;:::${i.partyPhone}:::;:::${i.date}:::;:::${i.invoiceNumber}:::;:::${i.totalAmount}:::;:::${i.itemsJson}:::;:::${i.remarks}\n")
        }
        sb.append("[END_INVOICES]\n")
        
        sb.append("[ORDERS]\n")
        ords.forEach { o ->
            sb.append("${o.id}:::;:::${o.partyId}:::;:::${o.orderDate}:::;:::${o.deliveryDate}:::;:::${o.status}:::;:::${o.itemsDescription}:::;:::${o.advanceAmount}:::;:::${o.agreedRate}:::;:::${o.remarks}\n")
        }
        sb.append("[END_ORDERS]\n")
        
        return sb.toString()
    }

    suspend fun restoreBackupString(backupStr: String): Boolean {
        try {
            if (!backupStr.startsWith("--- JEWEL LEDGER BACKUP")) return false
            
            val lines = backupStr.lines()
            var currentSection = ""
            
            val partiesList = mutableListOf<Party>()
            val txsList = mutableListOf<LedgerTransaction>()
            val estsList = mutableListOf<Estimate>()
            val invsList = mutableListOf<Invoice>()
            val ordsList = mutableListOf<Order>()
            
            var openCash = 0.0
            var openBank = 0.0
            var openGold = 0.0
            var openGoldDate = 0L
            var adminPass = "1234"
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("[")) {
                    if (trimmed.startsWith("[END_")) {
                        currentSection = ""
                    } else {
                        currentSection = trimmed
                    }
                    continue
                }
                
                when (currentSection) {
                    "[PREFS]" -> {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            val k = parts[0]
                            val v = parts[1]
                            when (k) {
                                "opening_cash" -> openCash = v.toDoubleOrNull() ?: 0.0
                                "opening_bank" -> openBank = v.toDoubleOrNull() ?: 0.0
                                "opening_gold" -> openGold = v.toDoubleOrNull() ?: 0.0
                                "opening_gold_date" -> openGoldDate = v.toLongOrNull() ?: 0L
                                "admin_password" -> adminPass = v
                            }
                        }
                    }
                    "[PARTIES]" -> {
                        val p = trimmed.split(":::;:::")
                        if (p.size >= 5) {
                            partiesList.add(Party(
                                id = p[0].toIntOrNull() ?: 0,
                                name = p[1],
                                phone = p[2],
                                city = p[3],
                                partyType = p[4],
                                createdAt = p.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis()
                            ))
                        }
                    }
                    "[TRANSACTIONS]" -> {
                        val p = trimmed.split(":::;:::")
                        if (p.size >= 11) {
                            txsList.add(LedgerTransaction(
                                id = p[0].toIntOrNull() ?: 0,
                                partyId = p[1].toIntOrNull() ?: 0,
                                date = p[2].toLongOrNull() ?: System.currentTimeMillis(),
                                type = p[3],
                                amount = p[4].toDoubleOrNull() ?: 0.0,
                                goldWeight = p[5].toDoubleOrNull() ?: 0.0,
                                purity = p[6],
                                rate = p[7].toDoubleOrNull() ?: 0.0,
                                bankName = p[8].ifEmpty { null },
                                referenceNo = p[9].ifEmpty { null },
                                remarks = p[10]
                            ))
                        }
                    }
                    "[ESTIMATES]" -> {
                        val p = trimmed.split(":::;:::")
                        if (p.size >= 10) {
                            estsList.add(Estimate(
                                id = p[0].toIntOrNull() ?: 0,
                                partyId = if (p[1] == "-1") null else p[1].toIntOrNull(),
                                partyName = p[2],
                                partyPhone = p[3],
                                date = p[4].toLongOrNull() ?: System.currentTimeMillis(),
                                estimateNumber = p[5],
                                totalAmount = p[6].toDoubleOrNull() ?: 0.0,
                                itemsJson = p[7],
                                remarks = p[8],
                                isPurchase = p[9].toBoolean()
                            ))
                        }
                    }
                    "[INVOICES]" -> {
                        val p = trimmed.split(":::;:::")
                        if (p.size >= 9) {
                            invsList.add(Invoice(
                                id = p[0].toIntOrNull() ?: 0,
                                partyId = if (p[1] == "-1") null else p[1].toIntOrNull(),
                                partyName = p[2],
                                partyPhone = p[3],
                                date = p[4].toLongOrNull() ?: System.currentTimeMillis(),
                                invoiceNumber = p[5],
                                totalAmount = p[6].toDoubleOrNull() ?: 0.0,
                                itemsJson = p[7],
                                remarks = p[8]
                            ))
                        }
                    }
                    "[ORDERS]" -> {
                        val p = trimmed.split(":::;:::")
                        if (p.size >= 9) {
                            ordsList.add(Order(
                                id = p[0].toIntOrNull() ?: 0,
                                partyId = p[1].toIntOrNull() ?: 0,
                                orderDate = p[2].toLongOrNull() ?: System.currentTimeMillis(),
                                deliveryDate = p[3].toLongOrNull() ?: System.currentTimeMillis(),
                                status = p[4],
                                itemsDescription = p[5],
                                advanceAmount = p[6].toDoubleOrNull() ?: 0.0,
                                agreedRate = p[7].toDoubleOrNull() ?: 0.0,
                                remarks = p[8]
                            ))
                        }
                    }
                }
            }
            
            val database = AppDatabase.getDatabase(getApplication())
            database.clearAllTables()
            
            partiesList.forEach { repository.insertParty(it) }
            txsList.forEach { repository.insertTransaction(it) }
            estsList.forEach { repository.insertEstimate(it) }
            invsList.forEach { repository.insertInvoice(it) }
            ordsList.forEach { repository.insertOrder(it) }
            
            updateOpeningCash(openCash)
            updateOpeningBank(openBank)
            updateOpeningGold(openGold, openGoldDate)
            prefs.edit().putString("admin_password", adminPass).apply()
            _adminPassword.value = adminPass
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
