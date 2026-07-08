package com.example.data

import kotlinx.coroutines.flow.Flow

class JewelRepository(private val database: AppDatabase) {
    private val partyDao = database.partyDao()
    private val transactionDao = database.transactionDao()
    private val estimateDao = database.estimateDao()
    private val invoiceDao = database.invoiceDao()
    private val orderDao = database.orderDao()

    val allParties: Flow<List<Party>> = partyDao.getAllParties()
    val allTransactions: Flow<List<LedgerTransaction>> = transactionDao.getAllTransactions()
    val cashTransactions: Flow<List<LedgerTransaction>> = transactionDao.getCashTransactions()
    val bankTransactions: Flow<List<LedgerTransaction>> = transactionDao.getBankTransactions()
    val allEstimates: Flow<List<Estimate>> = estimateDao.getAllEstimates()
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    suspend fun getPartyById(id: Int): Party? = partyDao.getPartyById(id)
    suspend fun getEstimateById(id: Int): Estimate? = estimateDao.getEstimateById(id)
    suspend fun getInvoiceById(id: Int): Invoice? = invoiceDao.getInvoiceById(id)

    fun getTransactionsForParty(partyId: Int): Flow<List<LedgerTransaction>> =
        transactionDao.getTransactionsForParty(partyId)

    fun getOrdersForParty(partyId: Int): Flow<List<Order>> =
        orderDao.getOrdersForParty(partyId)

    suspend fun insertParty(party: Party): Long = partyDao.insertParty(party)
    suspend fun deleteParty(party: Party) = partyDao.deleteParty(party)

    suspend fun insertTransaction(transaction: LedgerTransaction): Long =
        transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: LedgerTransaction) =
        transactionDao.deleteTransaction(transaction)

    suspend fun insertEstimate(estimate: Estimate): Long = estimateDao.insertEstimate(estimate)
    suspend fun deleteEstimate(estimate: Estimate) = estimateDao.deleteEstimate(estimate)

    suspend fun insertInvoice(invoice: Invoice): Long = invoiceDao.insertInvoice(invoice)
    suspend fun deleteInvoice(invoice: Invoice) = invoiceDao.deleteInvoice(invoice)

    suspend fun insertOrder(order: Order): Long = orderDao.insertOrder(order)
    suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)
}
