package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Estimate
import com.example.data.Invoice
import com.example.data.DailyVoucherItem
import com.example.data.deserializeItems
import com.example.data.JewelItem
import com.example.data.LedgerTransaction
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private fun formatAmount(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        return format.format(amount)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun shareEstimatePdf(context: Context, estimate: Estimate, items: List<JewelItem>) {
        val pdfDocument = PdfDocument()
        
        // A4 size: 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            isAntiAlias = true
        }

        // Draw header background
        paint.color = Color.rgb(11, 12, 16) // Deep Dark Gray / Black
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Draw header title
        textPaint.color = Color.rgb(212, 175, 55) // Luxury Gold
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 20f
        canvas.drawText("JEWELLEDGER WHOLESALERS", 30f, 40f, textPaint)

        // Subtitle
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        canvas.drawText("Wholesale Jewellery Accounting & Ledgers", 30f, 60f, textPaint)

        // Invoice Metadata Header
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(30f, 105f, 565f, 185f, paint)

        // Draw Metadata details
        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        val docTitle = if (estimate.isPurchase) "PURCHASE ESTIMATE" else "ESTIMATE INVOICE"
        canvas.drawText(docTitle, 40f, 125f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        canvas.drawText("Invoice No: ${estimate.estimateNumber}", 40f, 145f, textPaint)
        canvas.drawText("Date: ${formatDate(estimate.date)}", 40f, 165f, textPaint)

        val partyLabel = if (estimate.isPurchase) "Supplier: ${estimate.partyName}" else "Customer: ${estimate.partyName}"
        canvas.drawText(partyLabel, 320f, 125f, textPaint)
        if (estimate.partyPhone.isNotEmpty()) {
            canvas.drawText("Phone: ${estimate.partyPhone}", 320f, 145f, textPaint)
        }

        // Table Header
        paint.color = Color.rgb(212, 175, 55) // Gold
        canvas.drawRect(30f, 200f, 565f, 225f, paint)

        textPaint.color = Color.rgb(11, 12, 16)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 11f
        canvas.drawText("S.No", 40f, 217f, textPaint)
        canvas.drawText("Item Name / Type", 80f, 217f, textPaint)
        canvas.drawText("Weight / Qty / Rate Details", 220f, 217f, textPaint)
        canvas.drawText("Total Amount", 460f, 217f, textPaint)

        // Table Rows
        var currentY = 245f
        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 9f

        items.forEachIndexed { index, item ->
            // Zebra row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(250, 250, 250)
                canvas.drawRect(30f, currentY - 14f, 565f, currentY + 14f, paint)
            }

            textPaint.color = Color.rgb(33, 33, 33)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${index + 1}", 40f, currentY, textPaint)
            
            // Name
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.name, 80f, currentY, textPaint)

            // Details
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8.5f
            val detailsText = if (item.itemType == "METAL") {
                if (item.isRateLocked) {
                    "Net: ${item.weight}g | Wastage: ${item.wastagePercent}% = ${String.format("%.3f", item.pureFineWeight)}g Fine"
                } else {
                    "Net: ${item.weight}g | Wastage: ${item.wastagePercent}% = ${String.format("%.3f", item.pureFineWeight)}g (Unfixed)"
                }
            } else {
                "${item.pieces} pcs @ ₹${formatAmount(item.rate)}"
            }
            canvas.drawText(detailsText, 220f, currentY - 4f, textPaint)

            // Second line for weight and extra charges
            textPaint.textSize = 7.5f
            textPaint.color = Color.rgb(100, 100, 100)
            val line2Parts = mutableListOf<String>()
            if (item.itemType == "METAL") {
                line2Parts.add("Gross: ${item.grossWeight}g")
                line2Parts.add("Stone: ${item.stoneWeight}g")
                if (item.isRateLocked) {
                    line2Parts.add("Rate: ₹${formatAmount(item.rate)}")
                    if (item.makingCharges > 0) {
                        line2Parts.add("MC: ₹${formatAmount(item.makingCharges)}")
                    }
                }
            }
            if (item.stoneCharges > 0) line2Parts.add("Stone Chg: ₹${formatAmount(item.stoneCharges)}")
            if (item.freightCost > 0) line2Parts.add("Freight: ₹${formatAmount(item.freightCost)}")
            if (item.hallmarkingCharges > 0) line2Parts.add("Hallmark: ₹${formatAmount(item.hallmarkingCharges)}")
            if (line2Parts.isNotEmpty()) {
                canvas.drawText(line2Parts.joinToString(" | "), 220f, currentY + 7f, textPaint)
            }

            // Total Amount
            val amountText = if (item.itemType == "METAL" && !item.isRateLocked) {
                "Pending Gold"
            } else {
                "₹${formatAmount(item.total)}"
            }
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.rgb(33, 33, 33)
            textPaint.textSize = 9f
            canvas.drawText(amountText, 460f, currentY, textPaint)

            currentY += 32f
        }

        // Draw total section divider
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawRect(30f, currentY - 5f, 565f, currentY - 4f, paint)

        currentY += 15f

        // Total amounts
        textPaint.color = Color.rgb(11, 12, 16)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText("GRAND TOTAL CASH:", 240f, currentY, textPaint)
        canvas.drawText("₹${formatAmount(estimate.totalAmount)}", 460f, currentY, textPaint)

        val pendingFineGold = items.filter { !it.isRateLocked && it.itemType == "METAL" }.sumOf { it.pureFineWeight }
        if (pendingFineGold > 0) {
            currentY += 20f
            textPaint.color = Color.rgb(200, 50, 50)
            canvas.drawText("PENDING FINE GOLD:", 240f, currentY, textPaint)
            canvas.drawText("${String.format("%.3f", pendingFineGold)} g", 460f, currentY, textPaint)
        }

        if (estimate.remarks.isNotEmpty()) {
            currentY += 35f
            textPaint.color = Color.rgb(117, 117, 117)
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Remarks: ${estimate.remarks}", 40f, currentY, textPaint)
        }

        // Drawer Signature Line or decorative footer
        currentY = 780f
        paint.color = Color.rgb(220, 220, 220)
        canvas.drawRect(30f, currentY, 565f, currentY + 1f, paint)

        textPaint.color = Color.rgb(117, 117, 117)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Thank you for your business! Generated via JewelLedger Wholesale App.", 40f, currentY + 18f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache
        val fileName = "Estimate_Invoice_${estimate.estimateNumber}.pdf"
        val cacheFile = File(context.cacheDir, fileName)
        try {
            FileOutputStream(cacheFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            shareFile(context, cacheFile, "Estimate Invoice #${estimate.estimateNumber}")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun shareTransactionPdf(context: Context, tx: LedgerTransaction, partyName: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            isAntiAlias = true
        }

        // Draw header background
        paint.color = Color.rgb(11, 12, 16)
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Draw header title
        textPaint.color = Color.rgb(212, 175, 55) // Luxury Gold
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 20f
        canvas.drawText("JEWELLEDGER WHOLESALERS", 30f, 40f, textPaint)

        // Subtitle
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        canvas.drawText("Wholesale Jewellery Accounting & Ledgers", 30f, 60f, textPaint)

        // Title based on type
        val title = when (tx.type) {
            "CASH_IN" -> "CASH RECEIPT VOUCHER"
            "CASH_OUT" -> "CASH PAYMENT VOUCHER"
            "BANK_RECEIPT" -> "BANK RECEIPT VOUCHER"
            "BANK_PAYMENT" -> "BANK PAYMENT VOUCHER"
            "GOLD_RECEIPT" -> "GOLD RECEIPT VOUCHER"
            "GOLD_PAYMENT" -> "GOLD PAYMENT VOUCHER"
            "RATE_CUT_BUY" -> "PURCHASE RATE CUT VOUCHER"
            "RATE_CUT_SELL" -> "SALES RATE CUT VOUCHER"
            "ESTIMATE_INVOICE" -> "ESTIMATE INVOICE"
            "PURCHASE_ESTIMATE_INVOICE" -> "PURCHASE ESTIMATE"
            else -> "TRANSACTION VOUCHER"
        }

        // Voucher Info Block
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(30f, 105f, 565f, 185f, paint)

        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText(title, 40f, 125f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        val displayId = if (tx.id < 0) "EST-${-tx.id}" else "TX-${tx.id}"
        canvas.drawText("Voucher No: $displayId", 40f, 145f, textPaint)
        canvas.drawText("Date: ${formatDate(tx.date)}", 40f, 165f, textPaint)

        canvas.drawText("Party / Account: $partyName", 320f, 125f, textPaint)

        // Voucher Details Block
        paint.color = Color.rgb(212, 175, 55) // Gold accent line
        canvas.drawRect(30f, 200f, 565f, 203f, paint)

        var currentY = 230f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText("VOUCHER DETAILS", 30f, currentY, textPaint)
        currentY += 25f

        // Details list
        fun drawDetailLine(label: String, value: String) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11f
            textPaint.color = Color.rgb(100, 100, 100)
            canvas.drawText(label, 40f, currentY, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 11f
            textPaint.color = Color.rgb(33, 33, 33)
            canvas.drawText(value, 200f, currentY, textPaint)

            // Bottom subtle divider line
            paint.color = Color.rgb(230, 230, 230)
            canvas.drawRect(30f, currentY + 8f, 565f, currentY + 9f, paint)
            currentY += 30f
        }

        val typeLabel = when (tx.type) {
            "CASH_IN" -> "Cash Receipt (Dr)"
            "CASH_OUT" -> "Cash Issued (Cr)"
            "BANK_RECEIPT" -> "Bank Deposit Receipt"
            "BANK_PAYMENT" -> "Bank Withdrawal Payment"
            "GOLD_RECEIPT" -> "Metal Gold Receipt"
            "GOLD_PAYMENT" -> "Metal Gold Issued"
            "RATE_CUT_BUY" -> "Gold Rate Lock/Fix (Buy)"
            "RATE_CUT_SELL" -> "Gold Rate Lock/Fix (Sell)"
            "ESTIMATE_INVOICE" -> "Estimate Invoice Sales"
            "PURCHASE_ESTIMATE_INVOICE" -> "Purchase Estimate Invoice"
            else -> tx.type
        }

        drawDetailLine("Transaction Type:", typeLabel)

        if (tx.amount > 0) {
            drawDetailLine("Cash Amount:", "₹${formatAmount(tx.amount)}")
        }

        if (tx.goldWeight > 0) {
            drawDetailLine("Metal Weight:", "${String.format("%.3f", tx.goldWeight)} g")
        }

        if (tx.purity.isNotEmpty()) {
            drawDetailLine("Purity / Touch:", tx.purity)
        }

        if (tx.rate > 0) {
            drawDetailLine("Locked Rate / g:", "₹${formatAmount(tx.rate)}")
        }

        if (tx.remarks.isNotEmpty()) {
            drawDetailLine("Remarks / Note:", tx.remarks)
        }

        // Draw Signatures
        currentY = 650f
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawLine(50f, currentY, 200f, currentY, paint)
        canvas.drawLine(395f, currentY, 545f, currentY, paint)

        textPaint.textSize = 10f
        textPaint.color = Color.rgb(100, 100, 100)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Customer's Signature", 70f, currentY + 15f, textPaint)
        canvas.drawText("Authorized Signature", 415f, currentY + 15f, textPaint)

        // Footer
        currentY = 780f
        paint.color = Color.rgb(220, 220, 220)
        canvas.drawRect(30f, currentY, 565f, currentY + 1f, paint)

        textPaint.color = Color.rgb(117, 117, 117)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Thank you for your business! Generated via JewelLedger Wholesale App.", 40f, currentY + 18f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache
        val fileName = "Voucher_${displayId}.pdf"
        val cacheFile = File(context.cacheDir, fileName)
        try {
            FileOutputStream(cacheFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            shareFile(context, cacheFile, "Voucher $displayId - $partyName")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun shareOrderPdf(
        context: Context,
        order: com.example.data.Order,
        partyName: String,
        partyPhone: String
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            isAntiAlias = true
        }

        // Draw header background
        paint.color = Color.rgb(11, 12, 16)
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Draw header title
        textPaint.color = Color.rgb(212, 175, 55) // Luxury Gold
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 20f
        canvas.drawText("JEWELLEDGER WHOLESALERS", 30f, 40f, textPaint)

        // Subtitle
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        canvas.drawText("Wholesale Jewellery Accounting & Ledgers", 30f, 60f, textPaint)

        // Title
        val title = "CUSTOMER ORDER BOOKING"

        // Voucher Info Block
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(30f, 105f, 565f, 185f, paint)

        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText(title, 40f, 125f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        val displayId = "ORD-${order.id}"
        canvas.drawText("Order No: $displayId", 40f, 145f, textPaint)
        canvas.drawText("Booking Date: ${formatDate(order.orderDate)}", 40f, 165f, textPaint)

        canvas.drawText("Customer Name: $partyName", 320f, 125f, textPaint)
        if (partyPhone.isNotEmpty()) {
            canvas.drawText("Phone: $partyPhone", 320f, 145f, textPaint)
        }
        canvas.drawText("Delivery Date: ${formatDate(order.deliveryDate)}", 320f, 165f, textPaint)

        // Voucher Details Block
        paint.color = Color.rgb(212, 175, 55) // Gold accent line
        canvas.drawRect(30f, 200f, 565f, 203f, paint)

        var currentY = 230f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText("ORDER INFORMATION", 30f, currentY, textPaint)
        currentY += 25f

        // Details list helper
        fun drawDetailLine(label: String, value: String) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11f
            textPaint.color = Color.rgb(100, 100, 100)
            canvas.drawText(label, 40f, currentY, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 11f
            textPaint.color = Color.rgb(33, 33, 33)
            canvas.drawText(value, 200f, currentY, textPaint)

            // Bottom subtle divider line
            paint.color = Color.rgb(230, 230, 230)
            canvas.drawRect(30f, currentY + 8f, 565f, currentY + 9f, paint)
            currentY += 30f
        }

        drawDetailLine("Order Status:", order.status)
        drawDetailLine("Items Description:", order.itemsDescription)

        val rateLabel = if (order.agreedRate > 0) "₹${formatAmount(order.agreedRate)}/g (Rate Locked)" else "Market Rate (Rate Unfixed)"
        drawDetailLine("Agreed Gold Rate:", rateLabel)

        drawDetailLine("Advance Amount:", "₹${formatAmount(order.advanceAmount)}")

        if (order.remarks.isNotEmpty()) {
            drawDetailLine("Remarks / Notes:", order.remarks)
        }

        // Draw Signatures
        currentY = 650f
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawLine(50f, currentY, 200f, currentY, paint)
        canvas.drawLine(395f, currentY, 545f, currentY, paint)

        textPaint.textSize = 10f
        textPaint.color = Color.rgb(100, 100, 100)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Customer's Signature", 70f, currentY + 15f, textPaint)
        canvas.drawText("Authorized Signature", 415f, currentY + 15f, textPaint)

        // Footer
        currentY = 780f
        paint.color = Color.rgb(220, 220, 220)
        canvas.drawRect(30f, currentY, 565f, currentY + 1f, paint)

        textPaint.color = Color.rgb(117, 117, 117)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Thank you for your business! Generated via JewelLedger Wholesale App.", 40f, currentY + 18f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache
        val fileName = "Order_Booking_${order.id}.pdf"
        val cacheFile = File(context.cacheDir, fileName)
        try {
            FileOutputStream(cacheFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            shareFile(context, cacheFile, "Order Booking #$displayId - $partyName")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun shareLedgerPdf(
        context: Context,
        partyName: String,
        partyPhone: String,
        partyCity: String,
        cashBalance: Double,
        goldBalance: Double,
        transactions: List<LedgerTransaction>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            isAntiAlias = true
        }

        // Draw header background
        paint.color = Color.rgb(11, 12, 16)
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Draw header title
        textPaint.color = Color.rgb(212, 175, 55)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 20f
        canvas.drawText("JEWELLEDGER WHOLESALERS", 30f, 40f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        canvas.drawText("Party Ledger Account Statement", 30f, 60f, textPaint)

        // Party Info Metadata Block
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(30f, 105f, 565f, 185f, paint)

        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        canvas.drawText("LEDGER ACCOUNT STATEMENT", 40f, 125f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        canvas.drawText("Party Name: $partyName", 40f, 145f, textPaint)
        canvas.drawText("City: $partyCity", 40f, 165f, textPaint)
        if (partyPhone.isNotEmpty()) {
            canvas.drawText("Phone: $partyPhone", 220f, 145f, textPaint)
        }

        // Summarized Balances
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val cashBalLabel = if (cashBalance >= 0) "Dr: ₹${formatAmount(cashBalance)}" else "Cr: ₹${formatAmount(-cashBalance)}"
        canvas.drawText("Cash Balance: $cashBalLabel", 340f, 125f, textPaint)
        canvas.drawText("Gold Balance: ${String.format("%.3f", goldBalance)} g Fine", 340f, 145f, textPaint)

        // Table Header
        paint.color = Color.rgb(212, 175, 55)
        canvas.drawRect(30f, 200f, 565f, 225f, paint)

        textPaint.color = Color.rgb(11, 12, 16)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 10f
        canvas.drawText("Date", 40f, 217f, textPaint)
        canvas.drawText("Type", 110f, 217f, textPaint)
        canvas.drawText("Details / Remarks", 200f, 217f, textPaint)
        canvas.drawText("Metal (g)", 420f, 217f, textPaint)
        canvas.drawText("Cash Amount", 490f, 217f, textPaint)

        // Table Rows
        var currentY = 245f
        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 9f

        val printList = transactions.take(18)

        printList.forEachIndexed { index, tx ->
            if (index % 2 == 1) {
                paint.color = Color.rgb(250, 250, 250)
                canvas.drawRect(30f, currentY - 14f, 565f, currentY + 14f, paint)
            }

            textPaint.color = Color.rgb(33, 33, 33)
            canvas.drawText(formatDate(tx.date), 40f, currentY, textPaint)

            val typeStr = when (tx.type) {
                "CASH_IN" -> "Cash Received"
                "CASH_OUT" -> "Cash Paid"
                "BANK_RECEIPT" -> "Bank Receipt"
                "BANK_PAYMENT" -> "Bank Payment"
                "GOLD_RECEIPT" -> "Gold Received"
                "GOLD_PAYMENT" -> "Gold Issued"
                "RATE_CUT_BUY" -> "Purchase Cut"
                "RATE_CUT_SELL" -> "Sales Cut"
                "ESTIMATE_INVOICE" -> "Est Invoice"
                "PURCHASE_ESTIMATE_INVOICE" -> "Pur Estimate"
                else -> tx.type
            }
            canvas.drawText(typeStr, 110f, currentY, textPaint)

            val remarksText = if (tx.remarks.length > 35) tx.remarks.take(32) + "..." else tx.remarks
            canvas.drawText(remarksText, 200f, currentY, textPaint)

            val isGoldType = tx.type in listOf("GOLD_RECEIPT", "GOLD_PAYMENT") || ((tx.type == "ESTIMATE_INVOICE" || tx.type == "PURCHASE_ESTIMATE_INVOICE") && tx.goldWeight > 0)
            val goldText = if (isGoldType || tx.type.contains("RATE_CUT")) {
                val prefix = if (tx.type == "GOLD_PAYMENT" || tx.type == "PURCHASE_ESTIMATE_INVOICE") "-" else if (tx.type == "GOLD_RECEIPT" || tx.type == "ESTIMATE_INVOICE") "+" else ""
                "$prefix${String.format("%.3f", tx.goldWeight)}g"
            } else {
                "-"
            }
            canvas.drawText(goldText, 420f, currentY, textPaint)

            val isDebit = tx.type in listOf("CASH_OUT", "BANK_PAYMENT", "GOLD_PAYMENT", "RATE_CUT_SELL", "ESTIMATE_INVOICE")
            val amtText = if (tx.amount > 0) {
                val prefix = if (isDebit) "+" else "-"
                "$prefix₹${formatAmount(tx.amount)}"
            } else {
                "-"
            }
            canvas.drawText(amtText, 490f, currentY, textPaint)

            currentY += 26f
        }

        // Draw footer and signature
        currentY = 780f
        paint.color = Color.rgb(220, 220, 220)
        canvas.drawRect(30f, currentY, 565f, currentY + 1f, paint)

        textPaint.color = Color.rgb(117, 117, 117)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Generated via JewelLedger Wholesale App on ${formatDate(System.currentTimeMillis())}.", 40f, currentY + 18f, textPaint)

        pdfDocument.finishPage(page)

        val fileName = "Ledger_Statement_${partyName.replace(" ", "_")}.pdf"
        val cacheFile = File(context.cacheDir, fileName)
        try {
            FileOutputStream(cacheFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            shareFile(context, cacheFile, "Ledger Account Statement - $partyName")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun shareFile(context: Context, file: File, subject: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareTextFile(context: Context, file: File, subject: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Log File")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDailyReportPdf(
        context: Context,
        dateStr: String,
        vouchers: List<com.example.data.DailyVoucherItem>,
        estimatesForDay: List<Estimate>,
        invoicesForDay: List<Invoice>,
        cashIn: Double,
        cashOut: Double,
        goldIn: Double,
        goldOut: Double,
        totalSales: Double = 0.0,
        totalPurchases: Double = 0.0
    ) {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 10f
            isAntiAlias = true
        }

        fun drawHeader(canvas: Canvas, title: String) {
            // Draw header background (Premium Dark Theme)
            paint.color = Color.rgb(11, 12, 16)
            canvas.drawRect(0f, 0f, 595f, 75f, paint)

            // Title
            textPaint.color = Color.rgb(212, 175, 55) // Luxury Gold
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 16f
            canvas.drawText("JEWELLEDGER WHOLESALERS", 30f, 32f, textPaint)

            // Subtitle
            textPaint.color = Color.WHITE
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 10f
            canvas.drawText("$title: $dateStr (Page $pageNumber)", 30f, 50f, textPaint)
        }

        // Page 1 header and KPI boxes
        drawHeader(canvas, "Daily Transaction Summary Report")

        // KPI boxes background
        paint.color = Color.rgb(245, 245, 245)
        canvas.drawRect(30f, 85f, 565f, 155f, paint)

        textPaint.color = Color.rgb(33, 33, 33)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 8.5f

        // Column 1
        canvas.drawText("CASH INFLOW:", 40f, 103f, textPaint)
        textPaint.color = Color.rgb(27, 94, 32) // AccentGreen
        canvas.drawText("₹${formatAmount(cashIn)}", 120f, 103f, textPaint)

        textPaint.color = Color.rgb(33, 33, 33)
        canvas.drawText("CASH OUTFLOW:", 40f, 121f, textPaint)
        textPaint.color = Color.rgb(176, 0, 32) // AccentRed
        canvas.drawText("₹${formatAmount(cashOut)}", 120f, 121f, textPaint)

        // Column 2
        textPaint.color = Color.rgb(33, 33, 33)
        canvas.drawText("NET CASH FLOW:", 205f, 103f, textPaint)
        val netCash = cashIn - cashOut
        textPaint.color = if (netCash >= 0) Color.rgb(27, 94, 32) else Color.rgb(176, 0, 32)
        canvas.drawText("₹${formatAmount(netCash)}", 295f, 103f, textPaint)

        textPaint.color = Color.rgb(33, 33, 33)
        canvas.drawText("BILL SALES:", 205f, 121f, textPaint)
        textPaint.color = Color.rgb(212, 175, 55)
        canvas.drawText("₹${formatAmount(totalSales)}", 295f, 121f, textPaint)

        textPaint.color = Color.rgb(33, 33, 33)
        canvas.drawText("BILL PURCHASES:", 205f, 139f, textPaint)
        textPaint.color = Color.rgb(33, 33, 33)
        canvas.drawText("₹${formatAmount(totalPurchases)}", 295f, 139f, textPaint)

        // Column 3
        textPaint.color = Color.rgb(33, 33, 33)
        canvas.drawText("GOLD RECEIVED:", 410f, 103f, textPaint)
        canvas.drawText("${String.format(Locale.US, "%.3f", goldIn)} g", 505f, 103f, textPaint)

        canvas.drawText("GOLD ISSUED:", 410f, 121f, textPaint)
        canvas.drawText("${String.format(Locale.US, "%.3f", goldOut)} g", 505f, 121f, textPaint)

        canvas.drawText("TOTAL RECORDS:", 410f, 139f, textPaint)
        canvas.drawText("${vouchers.size}", 505f, 139f, textPaint)

        // Draw Table Header
        paint.color = Color.rgb(212, 175, 55) // Gold bar
        canvas.drawRect(30f, 165f, 565f, 185f, paint)

        textPaint.color = Color.rgb(11, 12, 16)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9f
        canvas.drawText("Sl", 35f, 178f, textPaint)
        canvas.drawText("Party Account", 60f, 178f, textPaint)
        canvas.drawText("Type", 210f, 178f, textPaint)
        canvas.drawText("Dr Ledger / Cr Ledger", 300f, 178f, textPaint)
        canvas.drawText("Weight", 460f, 178f, textPaint)
        canvas.drawText("Amount", 515f, 178f, textPaint)

        var currentY = 205f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(33, 33, 33)

        vouchers.forEachIndexed { idx, v ->
            if (currentY > 740f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawHeader(canvas, "Daily Transaction Summary Report")
                
                // Draw table subheader on new page
                paint.color = Color.rgb(212, 175, 55)
                canvas.drawRect(30f, 85f, 565f, 105f, paint)

                textPaint.color = Color.rgb(11, 12, 16)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 9f
                canvas.drawText("Sl", 35f, 98f, textPaint)
                canvas.drawText("Party Account", 60f, 98f, textPaint)
                canvas.drawText("Type", 210f, 98f, textPaint)
                canvas.drawText("Dr Ledger / Cr Ledger", 300f, 98f, textPaint)
                canvas.drawText("Weight", 460f, 98f, textPaint)
                canvas.drawText("Amount", 515f, 98f, textPaint)

                currentY = 125f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.rgb(33, 33, 33)
            }

            canvas.drawText("${idx + 1}", 35f, currentY, textPaint)
            
            val dispParty = if (v.partyName.length > 22) v.partyName.take(20) + ".." else v.partyName
            canvas.drawText(dispParty, 60f, currentY, textPaint)

            canvas.drawText(v.label, 210f, currentY, textPaint)

            // Double Entry Ledgers
            textPaint.textSize = 8f
            canvas.drawText("Dr: ${v.debitLedger}", 300f, currentY - 3f, textPaint)
            canvas.drawText("Cr: ${v.creditLedger}", 300f, currentY + 7f, textPaint)
            textPaint.textSize = 9f

            val goldWeightStr = if (v.goldWeight > 0) "${String.format(Locale.US, "%.3f", v.goldWeight)}g" else "-"
            canvas.drawText(goldWeightStr, 460f, currentY, textPaint)

            val amountStr = if (v.amount > 0) "₹${formatAmount(v.amount)}" else "-"
            canvas.drawText(amountStr, 515f, currentY, textPaint)

            // Horizontal line
            paint.color = Color.rgb(235, 235, 235)
            canvas.drawLine(30f, currentY + 12f, 565f, currentY + 12f, paint)

            currentY += 28f
        }

        // Section B: Detailed estimates & invoices itemwise report
        val hasDetailedItems = estimatesForDay.isNotEmpty() || invoicesForDay.isNotEmpty()
        var itemY = currentY

        if (hasDetailedItems) {
            // Force start Section B on a fresh new page for clean presentation
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            drawHeader(canvas, "Estimates & Invoices Itemized Report")
            itemY = 95f

            estimatesForDay.forEach { est ->
                val itemsList = est.itemsJson.deserializeItems()
                if (itemsList.isEmpty()) return@forEach

                if (itemY > 680f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas, "Estimates & Invoices Itemized Report")
                    itemY = 95f
                }

                // Draw bill header bar
                paint.color = Color.rgb(240, 240, 240)
                canvas.drawRect(30f, itemY, 565f, itemY + 22f, paint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 9f
                textPaint.color = Color.rgb(11, 12, 16)
                val typeLabel = if (est.isPurchase) "PURCHASE BILL" else "SALES BILL"
                canvas.drawText("$typeLabel #${est.estimateNumber} - ${est.partyName}", 35f, itemY + 14f, textPaint)
                textPaint.color = Color.rgb(100, 100, 100)
                canvas.drawText("Total: ₹${formatAmount(est.totalAmount)}", 440f, itemY + 14f, textPaint)

                itemY += 32f

                // Table subheaders
                textPaint.color = Color.rgb(120, 120, 120)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 7.5f
                canvas.drawText("Item Name", 40f, itemY, textPaint)
                canvas.drawText("Gross Wt", 145f, itemY, textPaint)
                canvas.drawText("Stone Wt", 195f, itemY, textPaint)
                canvas.drawText("Net Wt", 240f, itemY, textPaint)
                canvas.drawText("Wastage %", 285f, itemY, textPaint)
                canvas.drawText("Fine Wt", 335f, itemY, textPaint)
                canvas.drawText("Rate", 395f, itemY, textPaint)
                canvas.drawText("Charges / MC", 445f, itemY, textPaint)
                canvas.drawText("Total Amount", 515f, itemY, textPaint)

                itemY += 10f
                paint.color = Color.rgb(200, 200, 200)
                canvas.drawLine(30f, itemY, 565f, itemY, paint)
                itemY += 12f

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.rgb(33, 33, 33)
                textPaint.textSize = 8f

                itemsList.forEach { item ->
                    if (itemY > 740f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        drawHeader(canvas, "Estimates & Invoices Itemized Report")

                        itemY = 95f
                        // Subheaders
                        textPaint.color = Color.rgb(120, 120, 120)
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textPaint.textSize = 7.5f
                        canvas.drawText("Item Name", 40f, itemY, textPaint)
                        canvas.drawText("Gross Wt", 145f, itemY, textPaint)
                        canvas.drawText("Stone Wt", 195f, itemY, textPaint)
                        canvas.drawText("Net Wt", 240f, itemY, textPaint)
                        canvas.drawText("Wastage %", 285f, itemY, textPaint)
                        canvas.drawText("Fine Wt", 335f, itemY, textPaint)
                        canvas.drawText("Rate", 395f, itemY, textPaint)
                        canvas.drawText("Charges / MC", 445f, itemY, textPaint)
                        canvas.drawText("Total Amount", 515f, itemY, textPaint)
                        itemY += 10f
                        paint.color = Color.rgb(200, 200, 200)
                        canvas.drawLine(30f, itemY, 565f, itemY, paint)
                        itemY += 12f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textPaint.color = Color.rgb(33, 33, 33)
                        textPaint.textSize = 8f
                    }

                    val nameStr = if (item.name.length > 20) item.name.take(18) + ".." else item.name
                    canvas.drawText(nameStr, 40f, itemY, textPaint)

                    if (item.itemType == "METAL") {
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.grossWeight)}g", 145f, itemY, textPaint)
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.stoneWeight)}g", 195f, itemY, textPaint)
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.weight)}g", 240f, itemY, textPaint)
                        canvas.drawText("${item.wastagePercent}%", 285f, itemY, textPaint)
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.pureFineWeight)}g", 335f, itemY, textPaint)
                        canvas.drawText(if (item.isRateLocked) "₹${formatAmount(item.rate)}" else "Unfixed", 395f, itemY, textPaint)

                        val extraSum = item.stoneCharges + item.freightCost + item.hallmarkingCharges
                        val makingStr = "MC: ₹${formatAmount(item.makingCharges)}"
                        val extraStr = if (extraSum > 0) " + Ex: ₹${formatAmount(extraSum)}" else ""
                        canvas.drawText(makingStr + extraStr, 445f, itemY, textPaint)
                    } else {
                        canvas.drawText("-", 145f, itemY, textPaint)
                        canvas.drawText("-", 195f, itemY, textPaint)
                        canvas.drawText("-", 240f, itemY, textPaint)
                        canvas.drawText("-", 285f, itemY, textPaint)
                        canvas.drawText("-", 335f, itemY, textPaint)
                        canvas.drawText("₹${formatAmount(item.rate)}", 395f, itemY, textPaint)
                        canvas.drawText("Qty: ${item.pieces} pcs", 445f, itemY, textPaint)
                    }

                    canvas.drawText("₹${formatAmount(item.total)}", 515f, itemY, textPaint)

                    paint.color = Color.rgb(240, 240, 240)
                    canvas.drawLine(30f, itemY + 6f, 565f, itemY + 6f, paint)

                    itemY += 18f
                }
                itemY += 12f
            }

            invoicesForDay.forEach { inv ->
                val itemsList = inv.itemsJson.deserializeItems()
                if (itemsList.isEmpty()) return@forEach

                if (itemY > 680f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas, "Estimates & Invoices Itemized Report")
                    itemY = 95f
                }

                paint.color = Color.rgb(240, 240, 240)
                canvas.drawRect(30f, itemY, 565f, itemY + 22f, paint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 9f
                textPaint.color = Color.rgb(11, 12, 16)
                canvas.drawText("TAX INVOICE #${inv.invoiceNumber} - ${inv.partyName}", 35f, itemY + 14f, textPaint)
                textPaint.color = Color.rgb(100, 100, 100)
                canvas.drawText("Total: ₹${formatAmount(inv.totalAmount)}", 440f, itemY + 14f, textPaint)

                itemY += 32f

                // Table subheaders
                textPaint.color = Color.rgb(120, 120, 120)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 7.5f
                canvas.drawText("Item Name", 40f, itemY, textPaint)
                canvas.drawText("Gross Wt", 145f, itemY, textPaint)
                canvas.drawText("Stone Wt", 195f, itemY, textPaint)
                canvas.drawText("Net Wt", 240f, itemY, textPaint)
                canvas.drawText("Wastage %", 285f, itemY, textPaint)
                canvas.drawText("Fine Wt", 335f, itemY, textPaint)
                canvas.drawText("Rate", 395f, itemY, textPaint)
                canvas.drawText("Charges / MC", 445f, itemY, textPaint)
                canvas.drawText("Total Amount", 515f, itemY, textPaint)

                itemY += 10f
                paint.color = Color.rgb(200, 200, 200)
                canvas.drawLine(30f, itemY, 565f, itemY, paint)
                itemY += 12f

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.rgb(33, 33, 33)
                textPaint.textSize = 8f

                itemsList.forEach { item ->
                    if (itemY > 740f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        drawHeader(canvas, "Estimates & Invoices Itemized Report")

                        itemY = 95f
                        // Subheaders
                        textPaint.color = Color.rgb(120, 120, 120)
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textPaint.textSize = 7.5f
                        canvas.drawText("Item Name", 40f, itemY, textPaint)
                        canvas.drawText("Gross Wt", 145f, itemY, textPaint)
                        canvas.drawText("Stone Wt", 195f, itemY, textPaint)
                        canvas.drawText("Net Wt", 240f, itemY, textPaint)
                        canvas.drawText("Wastage %", 285f, itemY, textPaint)
                        canvas.drawText("Fine Wt", 335f, itemY, textPaint)
                        canvas.drawText("Rate", 395f, itemY, textPaint)
                        canvas.drawText("Charges / MC", 445f, itemY, textPaint)
                        canvas.drawText("Total Amount", 515f, itemY, textPaint)
                        itemY += 10f
                        paint.color = Color.rgb(200, 200, 200)
                        canvas.drawLine(30f, itemY, 565f, itemY, paint)
                        itemY += 12f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textPaint.color = Color.rgb(33, 33, 33)
                        textPaint.textSize = 8f
                    }

                    val nameStr = if (item.name.length > 20) item.name.take(18) + ".." else item.name
                    canvas.drawText(nameStr, 40f, itemY, textPaint)

                    if (item.itemType == "METAL") {
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.grossWeight)}g", 145f, itemY, textPaint)
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.stoneWeight)}g", 195f, itemY, textPaint)
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.weight)}g", 240f, itemY, textPaint)
                        canvas.drawText("${item.wastagePercent}%", 285f, itemY, textPaint)
                        canvas.drawText("${String.format(Locale.US, "%.3f", item.pureFineWeight)}g", 335f, itemY, textPaint)
                        canvas.drawText(if (item.isRateLocked) "₹${formatAmount(item.rate)}" else "Unfixed", 395f, itemY, textPaint)

                        val extraSum = item.stoneCharges + item.freightCost + item.hallmarkingCharges
                        val makingStr = "MC: ₹${formatAmount(item.makingCharges)}"
                        val extraStr = if (extraSum > 0) " + Ex: ₹${formatAmount(extraSum)}" else ""
                        canvas.drawText(makingStr + extraStr, 445f, itemY, textPaint)
                    } else {
                        canvas.drawText("-", 145f, itemY, textPaint)
                        canvas.drawText("-", 195f, itemY, textPaint)
                        canvas.drawText("-", 240f, itemY, textPaint)
                        canvas.drawText("-", 285f, itemY, textPaint)
                        canvas.drawText("-", 335f, itemY, textPaint)
                        canvas.drawText("₹${formatAmount(item.rate)}", 395f, itemY, textPaint)
                        canvas.drawText("Qty: ${item.pieces} pcs", 445f, itemY, textPaint)
                    }

                    canvas.drawText("₹${formatAmount(item.total)}", 515f, itemY, textPaint)

                    paint.color = Color.rgb(240, 240, 240)
                    canvas.drawLine(30f, itemY + 6f, 565f, itemY + 6f, paint)

                    itemY += 18f
                }
                itemY += 12f
            }
        }

        // Draw Signatures on the last page
        val endY = if (hasDetailedItems) itemY else currentY
        val sigY = if (endY > 670f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawHeader(canvas, "Daily Accountant Summary")
            120f
        } else {
            (endY + 40f).coerceAtLeast(670f)
        }

        paint.color = Color.rgb(180, 180, 180)
        canvas.drawLine(50f, sigY, 200f, sigY, paint)
        canvas.drawLine(395f, sigY, 545f, sigY, paint)

        textPaint.textSize = 9f
        textPaint.color = Color.rgb(100, 100, 100)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Accountant", 100f, sigY + 15f, textPaint)
        canvas.drawText("Authorized Sign", 435f, sigY + 15f, textPaint)

        // Footer line
        paint.color = Color.rgb(220, 220, 220)
        canvas.drawRect(30f, 785f, 565f, 786f, paint)

        textPaint.color = Color.rgb(117, 117, 117)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Daily Transactions & Estimate Report Generated on ${formatDate(System.currentTimeMillis())}. Confidential.", 40f, 800f, textPaint)

        pdfDocument.finishPage(page)

        val fileName = "Daily_Report_${dateStr.replace(" ", "_")}.pdf"
        val cacheFile = File(context.cacheDir, fileName)
        try {
            FileOutputStream(cacheFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            shareFile(context, cacheFile, "Daily Report $dateStr")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}

