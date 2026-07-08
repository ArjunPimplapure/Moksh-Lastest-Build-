package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties ORDER BY name ASC")
    fun getAllParties(): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getPartyById(id: Int): Party?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: Party): Long

    @Delete
    suspend fun deleteParty(party: Party)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM ledger_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM ledger_transactions WHERE partyId = :partyId ORDER BY date DESC")
    fun getTransactionsForParty(partyId: Int): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM ledger_transactions WHERE type IN ('CASH_IN', 'CASH_OUT') ORDER BY date DESC")
    fun getCashTransactions(): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM ledger_transactions WHERE type IN ('BANK_RECEIPT', 'BANK_PAYMENT') ORDER BY date DESC")
    fun getBankTransactions(): Flow<List<LedgerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LedgerTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: LedgerTransaction)
}

@Dao
interface EstimateDao {
    @Query("SELECT * FROM estimates ORDER BY date DESC")
    fun getAllEstimates(): Flow<List<Estimate>>

    @Query("SELECT * FROM estimates WHERE id = :id")
    suspend fun getEstimateById(id: Int): Estimate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: Estimate): Long

    @Delete
    suspend fun deleteEstimate(estimate: Estimate)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Int): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY deliveryDate ASC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE partyId = :partyId ORDER BY deliveryDate ASC")
    fun getOrdersForParty(partyId: Int): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Delete
    suspend fun deleteOrder(order: Order)
}
