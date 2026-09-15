package com.vx7.khatapro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.data.database.entities.PaymentEntity
import com.vx7.khatapro.ui.components.AddPaymentDialog
import com.vx7.khatapro.ui.components.ConfirmationDialog
import com.vx7.khatapro.ui.viewmodel.KhataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val payments by viewModel.payments.collectAsState()
    val orders by viewModel.ordersWithDetails.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("All") }

    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    val orderMap = remember(orders) { orders.associateBy { it.order.id } }

    val filteredPayments = remember(payments, searchQuery, selectedMethod, orderMap) {
        payments.filter { payment ->
            val matchesMethod = if (selectedMethod == "All") true else payment.paymentMethod == selectedMethod
            val relatedOrder = orderMap[payment.orderId]
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                payment.notes.lowercase().contains(q) ||
                        payment.paymentMethod.lowercase().contains(q) ||
                        (relatedOrder?.customerName?.lowercase()?.contains(q) == true) ||
                        payment.orderId.toString().contains(q)
            }
            matchesMethod && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search payments by customer or note") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_payments_input")
            )

            Spacer(Modifier.height(8.dp))

            // Method Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Cash", "UPI", "Bank Transfer").forEach { m ->
                    FilterChip(
                        selected = selectedMethod == m,
                        onClick = { selectedMethod = m },
                        label = { Text(m) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Paid,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "No payments match '$searchQuery'"
                            else "No payments recorded yet",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tap the '+' button below to record a customer deposit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        val relatedOrder = orderMap[payment.orderId]

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF22C55E),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            relatedOrder?.customerName ?: "Customer",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "Order #${payment.orderId} • ${payment.paymentMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            DateUtils.formatDateTime(payment.paymentDate),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        if (payment.notes.isNotBlank()) {
                                            Text(
                                                payment.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        CurrencyFormatter.format(payment.amount, settings.currency),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF22C55E),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(
                                        onClick = { paymentToDelete = payment },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Payment FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("record_payment_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Payments, contentDescription = "Record Payment")
        }
    }

    if (showAddDialog) {
        AddPaymentDialog(
            orders = orders,
            currencySymbol = settings.currency,
            onDismiss = { showAddDialog = false },
            onSave = { orderId, custId, amt, date, method, notes ->
                viewModel.addPayment(orderId, custId, amt, date, method, notes) {
                    showAddDialog = false
                }
            }
        )
    }

    paymentToDelete?.let { payment ->
        ConfirmationDialog(
            title = "Delete Payment?",
            message = "Are you sure you want to delete payment of ${CurrencyFormatter.format(payment.amount, settings.currency)}? This will increase the outstanding balance on Order #${payment.orderId}.",
            confirmText = "Delete Payment",
            isDestructive = true,
            onConfirm = {
                viewModel.deletePayment(payment.id) {
                    paymentToDelete = null
                }
            },
            onDismiss = { paymentToDelete = null }
        )
    }
}
