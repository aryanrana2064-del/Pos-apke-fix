package com.vx7.khatapro.core.utils

import java.text.DecimalFormat
import java.util.Locale

object CurrencyFormatter {
    private val decimalFormat = DecimalFormat("#,##,##0.00")

    fun format(amount: Double, symbol: String = "₹"): String {
        return "$symbol${decimalFormat.format(amount)}"
    }

    fun formatCompact(amount: Double, symbol: String = "₹"): String {
        return when {
            amount >= 10_000_000 -> "$symbol${String.format(Locale.US, "%.2f Cr", amount / 10_000_000)}"
            amount >= 100_000 -> "$symbol${String.format(Locale.US, "%.2f L", amount / 100_000)}"
            amount >= 1_000 -> "$symbol${String.format(Locale.US, "%.1f k", amount / 1_000)}"
            else -> format(amount, symbol)
        }
    }
}
