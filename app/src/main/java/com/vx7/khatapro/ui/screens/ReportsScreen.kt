package com.vx7.khatapro.ui.screens

import android.content.Context
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
import com.vx7.khatapro.core.utils.ShareUtils
import com.vx7.khatapro.ui.viewmodel.KhataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.dashboardStats.collectAsState()
    val orders by viewModel.ordersWithDetails.collectAsState()
    val customersWithLedger by viewModel.customersWithLedger.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val company by viewModel.companyProfile.collectAsState()

    var selectedReportType by remember { mutableStateOf("Outstanding Dues") }

    val collectionRate = remember(stats) {
        if (stats.totalOrderAmount > 0) {
            (stats.totalDeposits / stats.totalOrderAmount * 100).coerceIn(0.0, 100.0)
        } else 100.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report Selector Chips
        item {
            Text("Business Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Outstanding Dues", "Daily Summary", "Full Overview").forEach { type ->
                    FilterChip(
                        selected = selectedReportType == type,
                        onClick = { selectedReportType = type },
                        label = { Text(type) }
                    )
                }
            }
        }

        // Summary Performance KPI Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Financial Performance Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Billed", style = MaterialTheme.typography.labelSmall)
                            Text(
                                CurrencyFormatter.format(stats.totalOrderAmount, settings.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text("Collected", style = MaterialTheme.typography.labelSmall)
                            Text(
                                CurrencyFormatter.format(stats.totalDeposits, settings.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Due", style = MaterialTheme.typography.labelSmall)
                            Text(
                                CurrencyFormatter.format(stats.totalOutstanding, settings.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overall Collection Rate:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "%.1f%%".format(collectionRate),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Action Share Summary
        item {
            Button(
                onClick = {
                    val summaryText = buildString {
                        appendLine("📊 *VX7 Khata Pro Business Report*")
                        appendLine("Business: ${company?.companyName ?: "VX7 Khata Pro"}")
                        appendLine("Date: ${DateUtils.formatDateTime(System.currentTimeMillis())}")
                        appendLine("---------------------------")
                        appendLine("Total Customers: ${stats.totalCustomers}")
                        appendLine("Total Orders: ${stats.totalOrders}")
                        appendLine("Total Billed: ${CurrencyFormatter.format(stats.totalOrderAmount, settings.currency)}")
                        appendLine("Total Collected: ${CurrencyFormatter.format(stats.totalDeposits, settings.currency)}")
                        appendLine("Total Outstanding Due: ${CurrencyFormatter.format(stats.totalOutstanding, settings.currency)}")
                        appendLine("Collection Rate: %.1f%%".format(collectionRate))
                    }
                    ShareUtils.shareText(context, summaryText, "VX7 Khata Pro Financial Report")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_report_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share Financial Report via WhatsApp / SMS")
            }
        }

        // Dynamic Table based on report selection
        when (selectedReportType) {
            "Outstanding Dues" -> {
                item {
                    Text(
                        "Customers with Pending Dues",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val dueCustomers = customersWithLedger.filter { it.balance > 0.001 }

                if (dueCustomers.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E))
                                Spacer(Modifier.height(6.dp))
                                Text("All customer dues are completely settled!", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    items(dueCustomers) { cust ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                Column {
                                    Text(cust.customer.name, fontWeight = FontWeight.Bold)
                                    Text(cust.customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        CurrencyFormatter.format(cust.balance, settings.currency),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text("${cust.orderCount} Orders", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            "Daily Summary" -> {
                item {
                    Text(
                        "Today's Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New Orders Today:")
                                Text("${stats.todayOrdersCount}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Today's Billed Sales:")
                                Text(CurrencyFormatter.format(stats.todayOrderAmount, settings.currency), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deposits Recorded Today:")
                                Text("${stats.todayDepositsCount}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Collections Received Today:")
                                Text(CurrencyFormatter.format(stats.todayDepositsAmount, settings.currency), fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Text(
                        "All Active Orders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(orders) { ord ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${ord.customerName} - ${ord.itemsSummary}", fontWeight = FontWeight.SemiBold)
                                Text("Order #${ord.order.id} • ${DateUtils.formatDate(ord.order.orderDate)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(CurrencyFormatter.format(ord.order.total, settings.currency), fontWeight = FontWeight.Bold)
                                Text(
                                    if (ord.balance > 0) "Due: ${CurrencyFormatter.format(ord.balance, settings.currency)}" else "Paid",
                                    color = if (ord.balance > 0) MaterialTheme.colorScheme.error else Color(0xFF22C55E),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
