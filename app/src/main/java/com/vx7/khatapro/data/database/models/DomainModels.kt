package com.vx7.khatapro.data.database.models

import com.vx7.khatapro.data.database.entities.CustomerEntity
import com.vx7.khatapro.data.database.entities.OrderEntity
import com.vx7.khatapro.data.database.entities.OrderItemEntity
import com.vx7.khatapro.data.database.entities.PaymentEntity

enum class OrderStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID
}

// One line (item name + qty + rate) the caller wants added to an order.
// A single order/customer add can now carry several of these at once.
data class OrderItemInput(
    val item: String,
    val quantity: Double,
    val rate: Double
) {
    val total: Double get() = quantity * rate
}

// Builds a short, human-readable summary of an order's line items, e.g.
// "Shirt x3, Trouser x2" or "Shirt x3 +2 more items" when there are many.
fun summarizeOrderItems(items: List<OrderItemEntity>, maxShown: Int = 2): String {
    if (items.isEmpty()) return "No items"
    val shown = items.take(maxShown).joinToString(", ") { "${it.item} x${formatQty(it.quantity)}" }
    val remaining = items.size - maxShown
    return if (remaining > 0) "$shown +$remaining more item${if (remaining > 1) "s" else ""}" else shown
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()

data class OrderWithDetails(
    val order: OrderEntity,
    val items: List<OrderItemEntity> = emptyList(),
    val customerName: String,
    val customerPhone: String,
    val totalPaid: Double,
    val balance: Double,
    val latestPaymentDate: Long?,
    val status: OrderStatus,
    val payments: List<PaymentEntity> = emptyList()
) {
    val itemsSummary: String get() = summarizeOrderItems(items)
}

data class CustomerWithLedger(
    val customer: CustomerEntity,
    val totalOrderAmount: Double,
    val totalPaidAmount: Double,
    val balance: Double,
    val orderCount: Int
)

sealed class LedgerEntry(
    val id: Long,
    val date: Long,
    val description: String,
    val debitAmount: Double, // Order total
    val creditAmount: Double, // Payment amount
    val referenceInfo: String
) {
    class OrderItem(
        val order: OrderEntity,
        val items: List<OrderItemEntity>,
        totalPaidOnOrder: Double,
        orderBalance: Double
    ) : LedgerEntry(
        id = order.id,
        date = order.orderDate,
        description = summarizeOrderItems(items, maxShown = 3),
        debitAmount = order.total,
        creditAmount = 0.0,
        referenceInfo = "Order #${order.id} • Balance: ₹%.2f".format(orderBalance)
    )

    class PaymentItem(
        val payment: PaymentEntity,
        val itemName: String
    ) : LedgerEntry(
        id = payment.id,
        date = payment.paymentDate,
        description = "Payment received via ${payment.paymentMethod} (${itemName})",
        debitAmount = 0.0,
        creditAmount = payment.amount,
        referenceInfo = if (payment.notes.isNotBlank()) payment.notes else "Payment for Order #${payment.orderId}"
    )
}

data class DashboardStats(
    val totalCustomers: Int = 0,
    val totalOrders: Int = 0,
    val totalOrderAmount: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val todayOrdersCount: Int = 0,
    val todayOrderAmount: Double = 0.0,
    val todayDepositsCount: Int = 0,
    val todayDepositsAmount: Double = 0.0
)

data class CustomerLedgerReport(
    val customer: CustomerEntity,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double,
    val entries: List<LedgerEntry>
)
