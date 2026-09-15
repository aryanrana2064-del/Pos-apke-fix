package com.vx7.khatapro.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.core.utils.PdfGenerator
import com.vx7.khatapro.core.utils.ShareUtils
import com.vx7.khatapro.data.database.entities.CustomerEntity
import com.vx7.khatapro.data.database.entities.OrderEntity
import com.vx7.khatapro.data.database.models.OrderStatus
import com.vx7.khatapro.data.database.models.OrderWithDetails
import com.vx7.khatapro.ui.components.AddOrderDialog
import com.vx7.khatapro.ui.components.AddPaymentDialog
import com.vx7.khatapro.ui.components.ConfirmationDialog
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val orders by viewModel.ordersWithDetails.collectAsState()
    val rawCustomers by viewModel.rawCustomers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val company by viewModel.companyProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    var showAddOrderDialog by remember { mutableStateOf(false) }
    var orderForPayment by remember { mutableStateOf<OrderWithDetails?>(null) }
    var orderToDelete by remember { mutableStateOf<OrderEntity?>(null) }

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { ord ->
            val matchesFilter = when (selectedFilter) {
                "Unpaid" -> ord.status == OrderStatus.UNPAID
                "Partially Paid" -> ord.status == OrderStatus.PARTIALLY_PAID
                "Paid" -> ord.status == OrderStatus.PAID
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                ord.customerName.lowercase().contains(q) ||
                        ord.items.any { it.item.lowercase().contains(q) } ||
                        ord.order.id.toString().contains(q)
            }
            matchesFilter && matchesSearch
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
                label = { Text("Search orders by customer, item, or ID") },
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
                    .testTag("search_orders_input")
            )

            Spacer(Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Unpaid", "Partially Paid", "Paid").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filteredOrders.isEmpty()) {
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
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "No orders match '$searchQuery'"
                            else "No orders logged yet",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tap the '+' button below to create your first order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it.order.id }) { ord ->
                        OrderCardItem(
                            orderWithDetails = ord,
                            currencySymbol = settings.currency,
                            onAddPayment = { orderForPayment = ord },
                            onShareReceipt = {
                                coroutineScope.launch {
                                    val customer = rawCustomers.find { it.id == ord.order.customerId }
                                        ?: CustomerEntity(name = ord.customerName, phone = ord.customerPhone)
                                    val pdfFile = withContext(Dispatchers.IO) {
                                        PdfGenerator.generateOrderReceiptPdf(
                                            context = context,
                                            company = company,
                                            customer = customer,
                                            order = ord.order,
                                            items = ord.items,
                                            payments = ord.payments,
                                            currencySymbol = settings.currency
                                        )
                                    }
                                    ShareUtils.sharePdf(context, pdfFile, "Invoice #${ord.order.id}")
                                }
                            },
                            onPrintReceipt = {
                                coroutineScope.launch {
                                    val customer = rawCustomers.find { it.id == ord.order.customerId }
                                        ?: CustomerEntity(name = ord.customerName, phone = ord.customerPhone)
                                    val pdfFile = withContext(Dispatchers.IO) {
                                        PdfGenerator.generateOrderReceiptPdf(
                                            context = context,
                                            company = company,
                                            customer = customer,
                                            order = ord.order,
                                            items = ord.items,
                                            payments = ord.payments,
                                            currencySymbol = settings.currency
                                        )
                                    }
                                    ShareUtils.printPdf(context, pdfFile, "Invoice_${ord.order.id}")
                                }
                            },
                            onDelete = { orderToDelete = ord.order }
                        )
                    }
                }
            }
        }

        // Add Order FAB
        FloatingActionButton(
            onClick = { showAddOrderDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_order_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.PostAdd, contentDescription = "Create Order")
        }
    }

    // Add Order Dialog
    if (showAddOrderDialog) {
        AddOrderDialog(
            customers = rawCustomers,
            currencySymbol = settings.currency,
            onDismiss = { showAddOrderDialog = false },
            onSave = { custId, items, date, deposit, depDate, notes ->
                viewModel.addOrder(custId, items, date, deposit, depDate, notes) {
                    showAddOrderDialog = false
                }
            }
        )
    }

    // Record Payment Dialog for selected order
    orderForPayment?.let { ord ->
        AddPaymentDialog(
            orders = listOf(ord),
            preselectedOrder = ord,
            currencySymbol = settings.currency,
            onDismiss = { orderForPayment = null },
            onSave = { orderId, custId, amt, date, method, notes ->
                viewModel.addPayment(orderId, custId, amt, date, method, notes) {
                    orderForPayment = null
                }
            }
        )
    }

    // Delete Order Confirmation
    orderToDelete?.let { ord ->
        ConfirmationDialog(
            title = "Delete Order #${ord.id}?",
            message = "Are you sure you want to delete Order #${ord.id}? This will also remove all its items and any payment records tied to this order.",
            confirmText = "Delete Permanently",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteOrder(ord.id) {
                    orderToDelete = null
                }
            },
            onDismiss = { orderToDelete = null }
        )
    }
}

@Composable
fun OrderCardItem(
    orderWithDetails: OrderWithDetails,
    currencySymbol: String,
    onAddPayment: () -> Unit,
    onShareReceipt: () -> Unit,
    onPrintReceipt: () -> Unit,
    onDelete: () -> Unit
) {
    val ord = orderWithDetails.order

    val statusColor = when (orderWithDetails.status) {
        OrderStatus.PAID -> Color(0xFF22C55E) // Emerald
        OrderStatus.PARTIALLY_PAID -> Color(0xFFD97706) // Amber
        OrderStatus.UNPAID -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Order ID, Date & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Order #${ord.id}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        DateUtils.formatDate(ord.orderDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = orderWithDetails.status.name.replace("_", " "),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Customer and Item Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        orderWithDetails.customerName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        orderWithDetails.itemsSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    CurrencyFormatter.format(ord.total, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Financial Balance Breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Paid: ${CurrencyFormatter.format(orderWithDetails.totalPaid, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        "Due: ${CurrencyFormatter.format(orderWithDetails.balance, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (orderWithDetails.balance > 0.001) MaterialTheme.colorScheme.error else Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShareReceipt, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Receipt PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onPrintReceipt, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (orderWithDetails.balance > 0.001) {
                    Button(
                        onClick = onAddPayment,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Payment", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
