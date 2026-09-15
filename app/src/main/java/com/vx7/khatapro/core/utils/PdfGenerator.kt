package com.vx7.khatapro.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vx7.khatapro.data.database.entities.CompanyProfileEntity
import com.vx7.khatapro.data.database.entities.CustomerEntity
import com.vx7.khatapro.data.database.entities.OrderEntity
import com.vx7.khatapro.data.database.entities.OrderItemEntity
import com.vx7.khatapro.data.database.entities.PaymentEntity
import com.vx7.khatapro.data.database.models.CustomerLedgerReport
import com.vx7.khatapro.data.database.models.LedgerEntry
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f

    private fun formatQuantity(qty: Double): String =
        if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()

    fun generateOrderReceiptPdf(
        context: Context,
        company: CompanyProfileEntity?,
        customer: CustomerEntity,
        order: OrderEntity,
        items: List<OrderItemEntity>,
        payments: List<PaymentEntity>,
        currencySymbol: String = "₹"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Top Accent Bar (Brand Navy)
        paint.color = Color.rgb(30, 58, 138) // #1E3A8A
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, paint)

        var y = 45f

        // Company Logo if exists
        company?.logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (bitmap != null) {
                        val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
                        canvas.drawBitmap(scaled, MARGIN, y, paint)
                    }
                }
            } catch (_: Exception) {}
        }

        val textStartX = if (company?.logoPath != null) MARGIN + 60f else MARGIN

        // Company Info
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText(company?.companyName ?: "VX7 Khata Pro Business", textStartX, y + 16f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        val companySub = buildString {
            append(company?.ownerName ?: "Authorized Dealer")
            company?.mobile?.let { if (it.isNotBlank()) append(" • Ph: $it") }
            company?.email?.let { if (it.isNotBlank()) append(" • $it") }
        }
        canvas.drawText(companySub, textStartX, y + 30f, paint)

        val addressLine = buildString {
            company?.address?.let { if (it.isNotBlank()) append(it) }
            company?.city?.let { if (it.isNotBlank()) append(", $it") }
            company?.state?.let { if (it.isNotBlank()) append(", $it") }
            company?.gstNumber?.let { if (it.isNotBlank()) append(" • GSTIN: $it") }
        }
        if (addressLine.isNotBlank()) {
            canvas.drawText(addressLine, textStartX, y + 42f, paint)
        }

        // Receipt Badge on top right
        paint.color = Color.rgb(219, 234, 254) // light blue bg
        val badgeRect = RectF(PAGE_WIDTH - MARGIN - 130f, y, PAGE_WIDTH - MARGIN, y + 42f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

        paint.color = Color.rgb(30, 58, 138)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("ORDER RECEIPT", PAGE_WIDTH - MARGIN - 120f, y + 18f, paint)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("No: #${order.id}", PAGE_WIDTH - MARGIN - 120f, y + 32f, paint)

        y += 65f

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        y += 20f

        // Two Column Section: Bill To & Order Metadata
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.rgb(30, 58, 138)
        canvas.drawText("BILL TO CUSTOMER:", MARGIN, y, paint)
        canvas.drawText("ORDER DETAILS:", PAGE_WIDTH / 2f + 20f, y, paint)

        y += 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText(customer.name, MARGIN, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Date: ${DateUtils.formatDateTime(order.orderDate)}", PAGE_WIDTH / 2f + 20f, y, paint)

        y += 14f
        canvas.drawText("Phone: ${customer.phone}", MARGIN, y, paint)
        val totalPaid = payments.sumOf { it.amount }
        val balance = (order.total - totalPaid).coerceAtLeast(0.0)
        val status = when {
            balance <= 0.001 -> "PAID"
            totalPaid > 0 -> "PARTIALLY PAID"
            else -> "UNPAID"
        }
        canvas.drawText("Status: $status", PAGE_WIDTH / 2f + 20f, y, paint)

        if (customer.address.isNotBlank()) {
            y += 14f
            canvas.drawText("Address: ${customer.address}", MARGIN, y, paint)
        }

        y += 28f

        // Item Table Header
        val tableTop = y
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + 24f), 4f, 4f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("ITEM DESCRIPTION", MARGIN + 10f, tableTop + 16f, paint)
        canvas.drawText("QTY", PAGE_WIDTH - MARGIN - 210f, tableTop + 16f, paint)
        canvas.drawText("RATE", PAGE_WIDTH - MARGIN - 130f, tableTop + 16f, paint)
        canvas.drawText("TOTAL", PAGE_WIDTH - MARGIN - 50f, tableTop + 16f, paint)

        y += 38f

        // Item Rows (supports multiple items on a single order)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        val rowsToRender = if (items.isNotEmpty()) items else emptyList()
        for (lineItem in rowsToRender) {
            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(lineItem.item, MARGIN + 10f, y, paint)
            canvas.drawText(formatQuantity(lineItem.quantity), PAGE_WIDTH - MARGIN - 210f, y, paint)
            canvas.drawText(CurrencyFormatter.format(lineItem.rate, currencySymbol), PAGE_WIDTH - MARGIN - 130f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(CurrencyFormatter.format(lineItem.total, currencySymbol), PAGE_WIDTH - MARGIN - 50f, y, paint)
            y += 18f
        }

        y += 6f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        y += 20f

        // Financial Summary Box on Right
        val summaryX = PAGE_WIDTH - MARGIN - 220f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 10f
        canvas.drawText("Subtotal / Order Total:", summaryX, y, paint)
        canvas.drawText(CurrencyFormatter.format(order.total, currencySymbol), PAGE_WIDTH - MARGIN - 50f, y, paint)

        y += 18f
        paint.color = Color.rgb(4, 120, 87) // Green
        canvas.drawText("Total Amount Paid:", summaryX, y, paint)
        canvas.drawText(CurrencyFormatter.format(totalPaid, currencySymbol), PAGE_WIDTH - MARGIN - 50f, y, paint)

        y += 18f
        val balanceColor = if (balance > 0) Color.rgb(220, 38, 38) else Color.rgb(4, 120, 87)
        paint.color = balanceColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Remaining Balance:", summaryX, y, paint)
        canvas.drawText(CurrencyFormatter.format(balance, currencySymbol), PAGE_WIDTH - MARGIN - 50f, y, paint)

        y += 35f

        // Payment History Section
        if (payments.isNotEmpty()) {
            paint.color = Color.rgb(30, 58, 138)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText("PAYMENT BREAKDOWN (${payments.size} Transactions):", MARGIN, y, paint)
            y += 16f

            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, paint)
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 8f
            canvas.drawText("DATE", MARGIN + 10f, y + 14f, paint)
            canvas.drawText("METHOD", MARGIN + 140f, y + 14f, paint)
            canvas.drawText("NOTES", MARGIN + 240f, y + 14f, paint)
            canvas.drawText("AMOUNT PAID", PAGE_WIDTH - MARGIN - 70f, y + 14f, paint)

            y += 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(15, 23, 42)
            for (p in payments) {
                canvas.drawText(DateUtils.formatDateTime(p.paymentDate), MARGIN + 10f, y, paint)
                canvas.drawText(p.paymentMethod, MARGIN + 140f, y, paint)
                val noteText = if (p.notes.isBlank()) "-" else p.notes.take(25)
                canvas.drawText(noteText, MARGIN + 240f, y, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(4, 120, 87)
                canvas.drawText(CurrencyFormatter.format(p.amount, currencySymbol), PAGE_WIDTH - MARGIN - 70f, y, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(15, 23, 42)
                y += 18f
            }
        }

        // Terms / Footer Signature
        val footerY = PAGE_HEIGHT - MARGIN - 50f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY, paint)

        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("This is a computer-generated invoice from VX7 Khata Pro.", MARGIN, footerY + 20f, paint)
        canvas.drawText("Smart Khata & Business Ledger", MARGIN, footerY + 32f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Authorized Signature", PAGE_WIDTH - MARGIN - 120f, footerY + 32f, paint)
        canvas.drawLine(PAGE_WIDTH - MARGIN - 130f, footerY + 18f, PAGE_WIDTH - MARGIN, footerY + 18f, paint)

        pdfDocument.finishPage(page)

        // Save to cache directory
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(cacheDir, "Receipt_Order_${order.id}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    fun generateCustomerLedgerPdf(
        context: Context,
        company: CompanyProfileEntity?,
        customer: CustomerEntity,
        ledgerReport: CustomerLedgerReport,
        currencySymbol: String = "₹"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Top brand bar
        paint.color = Color.rgb(30, 58, 138)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, paint)

        var y = 45f

        // Company Logo if exists
        company?.logoPath?.let { path ->
            try {
                val logoFile = File(path)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (bitmap != null) {
                        val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
                        canvas.drawBitmap(scaled, MARGIN, y, paint)
                    }
                }
            } catch (_: Exception) {}
        }

        val textStartX = if (company?.logoPath != null) MARGIN + 60f else MARGIN

        // Company Info
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText(company?.companyName ?: "VX7 Khata Pro Business", textStartX, y + 16f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("STATEMENT OF ACCOUNT / KHATA LEDGER", textStartX, y + 30f, paint)

        val addressLine = buildString {
            company?.ownerName?.let { append(it) }
            company?.mobile?.let { append(" • Ph: $it") }
            company?.gstNumber?.let { if (it.isNotBlank()) append(" • GST: $it") }
        }
        canvas.drawText(addressLine, textStartX, y + 42f, paint)

        y += 65f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        y += 20f

        // Customer Info Card Box
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 60f), 6f, 6f, paint)

        paint.color = Color.rgb(30, 58, 138)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("CUSTOMER: ${customer.name}", MARGIN + 12f, y + 20f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        canvas.drawText("Mobile: ${customer.phone}", MARGIN + 12f, y + 35f, paint)
        canvas.drawText("Address: ${if (customer.address.isBlank()) "N/A" else customer.address}", MARGIN + 12f, y + 48f, paint)

        // Summary on right of box
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("TOTAL BILLED:", PAGE_WIDTH - MARGIN - 180f, y + 20f, paint)
        canvas.drawText(CurrencyFormatter.format(ledgerReport.totalDebit, currencySymbol), PAGE_WIDTH - MARGIN - 60f, y + 20f, paint)

        paint.color = Color.rgb(4, 120, 87)
        canvas.drawText("TOTAL PAID:", PAGE_WIDTH - MARGIN - 180f, y + 35f, paint)
        canvas.drawText(CurrencyFormatter.format(ledgerReport.totalCredit, currencySymbol), PAGE_WIDTH - MARGIN - 60f, y + 35f, paint)

        val balColor = if (ledgerReport.netBalance > 0) Color.rgb(220, 38, 38) else Color.rgb(4, 120, 87)
        paint.color = balColor
        canvas.drawText("CLOSING DUE:", PAGE_WIDTH - MARGIN - 180f, y + 50f, paint)
        canvas.drawText(CurrencyFormatter.format(ledgerReport.netBalance, currencySymbol), PAGE_WIDTH - MARGIN - 60f, y + 50f, paint)

        y += 80f

        // Table Header
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f), 4f, 4f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("DATE", MARGIN + 8f, y + 15f, paint)
        canvas.drawText("TRANSACTION / DETAILS", MARGIN + 80f, y + 15f, paint)
        canvas.drawText("DEBIT (₹)", PAGE_WIDTH - MARGIN - 160f, y + 15f, paint)
        canvas.drawText("CREDIT (₹)", PAGE_WIDTH - MARGIN - 70f, y + 15f, paint)

        y += 34f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        // Entries
        for (entry in ledgerReport.entries.take(25)) {
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(DateUtils.formatNumericDate(entry.date), MARGIN + 8f, y, paint)

            paint.color = Color.rgb(15, 23, 42)
            val desc = entry.description.take(40)
            canvas.drawText(desc, MARGIN + 80f, y, paint)

            if (entry.debitAmount > 0) {
                paint.color = Color.rgb(220, 38, 38)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(CurrencyFormatter.format(entry.debitAmount, currencySymbol), PAGE_WIDTH - MARGIN - 160f, y, paint)
            } else {
                paint.color = Color.rgb(148, 163, 184)
                canvas.drawText("-", PAGE_WIDTH - MARGIN - 160f, y, paint)
            }

            if (entry.creditAmount > 0) {
                paint.color = Color.rgb(4, 120, 87)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(CurrencyFormatter.format(entry.creditAmount, currencySymbol), PAGE_WIDTH - MARGIN - 70f, y, paint)
            } else {
                paint.color = Color.rgb(148, 163, 184)
                canvas.drawText("-", PAGE_WIDTH - MARGIN - 70f, y, paint)
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            y += 18f
        }

        val footerY = PAGE_HEIGHT - MARGIN - 40f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY, paint)

        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("Generated by VX7 Khata Pro Ledger System • Date: ${DateUtils.formatDateTime(System.currentTimeMillis())}", MARGIN, footerY + 18f, paint)

        pdfDocument.finishPage(page)

        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(cacheDir, "Ledger_${customer.name.replace("\\s+".toRegex(), "_")}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }
}
