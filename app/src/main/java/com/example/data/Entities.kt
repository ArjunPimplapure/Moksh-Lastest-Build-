package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parties")
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val city: String,
    val partyType: String, // "CUSTOMER" or "SUPPLIER"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger_transactions")
data class LedgerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int, // 0 if general cash/bank not tied to a party
    val date: Long,
    val type: String, // "CASH_IN", "CASH_OUT", "BANK_RECEIPT", "BANK_PAYMENT", "GOLD_RECEIPT", "GOLD_PAYMENT", "RATE_CUT_BUY", "RATE_CUT_SELL"
    val amount: Double, // cash/bank amount, or rate-cut final monetary value
    val goldWeight: Double, // gold received/issued in grams, or rate-cut gold weight
    val purity: String, // e.g. "22K (91.6)", "24K (99.9)", "18K (75.0)"
    val rate: Double, // locked gold rate per gram (only for rate cuts)
    val bankName: String? = null, // for bank transactions
    val referenceNo: String? = null, // for bank/UPI ref no or voucher ID
    val remarks: String
)

@Entity(tableName = "estimates")
data class Estimate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int?, // optional linked party
    val partyName: String, // can be custom
    val partyPhone: String,
    val date: Long,
    val estimateNumber: String,
    val totalAmount: Double,
    val itemsJson: String, // serialized items
    val remarks: String,
    val isPurchase: Boolean = false
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int?, // optional linked party
    val partyName: String,
    val partyPhone: String,
    val date: Long,
    val invoiceNumber: String,
    val totalAmount: Double,
    val itemsJson: String, // serialized items
    val remarks: String
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int, // customer linked to the order
    val orderDate: Long,
    val deliveryDate: Long,
    val status: String, // "Pending", "In Progress", "Ready", "Delivered"
    val itemsDescription: String, // e.g. "Chains - 50g, Rings - 20g"
    val advanceAmount: Double,
    val agreedRate: Double, // gold rate locked if any
    val remarks: String
)

// Helper structure for Estimate/Invoice Items
data class JewelItem(
    val name: String,
    val weight: Double, // in grams (Net Weight)
    val rate: Double, // per gram or per piece
    val makingCharges: Double, // per gram or fixed making charges
    val total: Double,
    val itemType: String = "METAL", // "METAL" or "PIECE"
    val wastagePercent: Double = 100.0, // e.g. 83.0%
    val pureFineWeight: Double = 0.0, // e.g. 14.940g
    val pieces: Int = 0, // e.g. 50 pcs
    val isRateLocked: Boolean = true, // whether rate is fixed or fine gold weight is pending
    val grossWeight: Double = 0.0,
    val stoneWeight: Double = 0.0,
    val stoneCharges: Double = 0.0,
    val freightCost: Double = 0.0,
    val hallmarkingCharges: Double = 0.0
) {
    fun serialize(): String {
        return "$name|$weight|$rate|$makingCharges|$total|$itemType|$wastagePercent|$pureFineWeight|$pieces|$isRateLocked|$grossWeight|$stoneWeight|$stoneCharges|$freightCost|$hallmarkingCharges"
    }

    companion object {
        fun deserialize(str: String): JewelItem {
            val parts = str.split("|")
            val baseWeight = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            return JewelItem(
                name = parts.getOrNull(0) ?: "",
                weight = baseWeight,
                rate = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                makingCharges = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                total = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                itemType = parts.getOrNull(5) ?: "METAL",
                wastagePercent = parts.getOrNull(6)?.toDoubleOrNull() ?: 100.0,
                pureFineWeight = parts.getOrNull(7)?.toDoubleOrNull() ?: baseWeight,
                pieces = parts.getOrNull(8)?.toIntOrNull() ?: 0,
                isRateLocked = parts.getOrNull(9)?.let { it.trim().lowercase() == "true" } ?: true,
                grossWeight = parts.getOrNull(10)?.toDoubleOrNull() ?: baseWeight,
                stoneWeight = parts.getOrNull(11)?.toDoubleOrNull() ?: 0.0,
                stoneCharges = parts.getOrNull(12)?.toDoubleOrNull() ?: 0.0,
                freightCost = parts.getOrNull(13)?.toDoubleOrNull() ?: 0.0,
                hallmarkingCharges = parts.getOrNull(14)?.toDoubleOrNull() ?: 0.0
            )
        }
    }
}

fun List<JewelItem>.serialize(): String {
    return this.joinToString(";;;") { it.serialize() }
}

fun String.deserializeItems(): List<JewelItem> {
    if (this.isBlank()) return emptyList()
    return this.split(";;;").map { JewelItem.deserialize(it) }
}

data class DailyVoucherItem(
    val id: String,
    val type: String, // CASH_IN, CASH_OUT, BANK_RECEIPT, BANK_PAYMENT, GOLD_RECEIPT, GOLD_PAYMENT, RATE_CUT_BUY, RATE_CUT_SELL, SALES_ESTIMATE, PURCHASE_ESTIMATE, TAX_INVOICE
    val label: String,
    val partyName: String,
    val date: Long,
    val debitLedger: String,
    val creditLedger: String,
    val amount: Double,
    val goldWeight: Double,
    val pureGoldWeight: Double,
    val rate: Double,
    val reference: String,
    val remarks: String,
    val itemsJson: String = ""
)

