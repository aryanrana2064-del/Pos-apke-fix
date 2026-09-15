package com.vx7.khatapro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.data.database.models.OrderStatus
import com.vx7.khatapro.data.database.models.OrderWithDetails
import com.vx7.khatapro.ui.components.AddCustomerDialog
import com.vx7.khatapro.ui.components.AddOrderDialog
import com.vx7.khatapro.ui.components.AddPaymentDialog
import com.vx7.khatapro.ui.theme.KhataPrimaryLight
import com.vx7.khatapro.ui.theme.KhataSecondaryLight
import com.vx7.khatapro.ui.viewmodel.AppScreen
import com.vx7.khatapro.ui.viewmodel.KhataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val orders by viewModel.ordersWithDetails.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val rawCustomers by viewModel.rawCustomers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val company by viewModel.companyProfile.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddOrderDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }

    val recentOrders = remember(orders) { orders.take(5) }
    val pendingOrders = remember(orders) { orders.filter { it.status != OrderStatus.PAID }.take(3) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Overview Card (Total Outstanding & Net Position)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    company?.companyName ?: "Khata Book",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    "Total Outstanding Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }
                        }

                        Text(
                            text = CurrencyFormatter.format(stats.totalOutstanding, settings.currency),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Billed", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    CurrencyFormatter.format(stats.totalOrderAmount, settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Deposits", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    CurrencyFormatter.format(stats.totalDeposits, settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6EE7B7) // Light Emerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    title = "Add Customer",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { showAddCustomerDialog = true }
                )

                QuickActionButton(
                    icon = Icons.Default.PostAdd,
                    title = "New Order",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { showAddOrderDialog = true }
                )

                QuickActionButton(
                    icon = Icons.Default.Payments,
                    title = "Record Payment",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { showAddPaymentDialog = true }
                )
            }
        }

        // Metrics Grid (Customers, Orders, Today's Activity)
        item {
            Text(
                "Business Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Customers",
                    value = stats.totalCustomers.toString(),
                    subValue = "Active Accounts",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CUSTOMERS) }
                )

                StatCard(
                    title = "Total Orders",
                    value = stats.totalOrders.toString(),
                    subValue = "Orders Logged",
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.ORDERS) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Today's Orders",
                    value = CurrencyFormatter.format(stats.todayOrderAmount, settings.currency),
                    subValue = "${stats.todayOrdersCount} orders today",
                    icon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Today's Collections",
                    value = CurrencyFormatter.format(stats.todayDepositsAmount, settings.currency),
                    subValue = "${stats.todayDepositsCount} deposits today",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Attention / Pending Orders Card
        if (pendingOrders.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    "Pending & Partially Paid Orders",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(onClick = { viewModel.navigateTo(AppScreen.ORDERS) }) {
                                Text("View All")
                            }
                        }

                        pendingOrders.forEach { ord ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectCustomerForLedger(ord.order.customerId) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ord.customerName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${ord.itemsSummary} (Order #${ord.order.id})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Due: ${CurrencyFormatter.format(ord.balance, settings.currency)}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        ord.status.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (ord != pendingOrders.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        // Recent Orders Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Orders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.navigateTo(AppScreen.ORDERS) }) {
                    Text("See All")
                }
            }
        }

        if (recentOrders.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Text("No orders created yet", fontWeight = FontWeight.Medium)
                        Text("Tap 'New Order' above to add your first customer order.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recentOrders) { ord ->
                DashboardOrderCard(
                    order = ord,
                    currencySymbol = settings.currency,
                    onClick = { viewModel.selectCustomerForLedger(ord.order.customerId) }
                )
            }
        }
    }

    // Modal Dialogs
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, notes ->
                viewModel.addCustomer(name, phone, address, notes) {
                    showAddCustomerDialog = false
                }
            }
        )
    }

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

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            orders = orders,
            currencySymbol = settings.currency,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { orderId, custId, amt, date, method, notes ->
                viewModel.addPayment(orderId, custId, amt, date, method, notes) {
                    showAddPaymentDialog = false
                }
            }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    title: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(90.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }

            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DashboardOrderCard(
    order: OrderWithDetails,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(order.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("${order.itemsSummary} (Qty: ${order.items.sumOf { it.quantity }})", style = MaterialTheme.typography.bodyMedium)
                Text(
                    DateUtils.formatDate(order.order.orderDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    CurrencyFormatter.format(order.order.total, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (order.balance > 0.001) {
                    Text(
                        "Due: ${CurrencyFormatter.format(order.balance, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        "Fully Paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
