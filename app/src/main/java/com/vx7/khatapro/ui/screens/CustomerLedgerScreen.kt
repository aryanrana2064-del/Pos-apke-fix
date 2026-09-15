package com.vx7.khatapro.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.core.utils.PdfGenerator
import com.vx7.khatapro.core.utils.ShareUtils
import com.vx7.khatapro.data.database.models.LedgerEntry
import com.vx7.khatapro.ui.components.AddOrderDialog
import com.vx7.khatapro.ui.components.AddPaymentDialog
import com.vx7.khatapro.ui.viewmodel.AppScreen
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ledgerReport by viewModel.selectedCustomerLedger.collectAsState()
    val company by viewModel.companyProfile.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val orders by viewModel.ordersWithDetails.collectAsState()
    val rawCustomers by viewModel.rawCustomers.collectAsState()

    var showAddOrderDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    if (ledgerReport == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No customer selected", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { viewModel.navigateTo(AppScreen.CUSTOMERS) }) {
                    Text("Go to Customers")
                }
            }
        }
        return
    }

    val report = ledgerReport!!
    val customerOrders = remember(orders, report.customer.id) {
        orders.filter { it.order.customerId == report.customer.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(report.customer.name, fontWeight = FontWeight.Bold)
                        Text(
                            report.customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.CUSTOMERS) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${report.customer.phone}")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call Customer")
                    }
                    IconButton(
                        onClick = {
                            val summary = "Dear ${report.customer.name}, your current outstanding Khata balance with ${company?.companyName ?: "us"} is ${CurrencyFormatter.format(report.netBalance, settings.currency)}. Please settle at your earliest convenience."
                            ShareUtils.shareText(context, summary, "Ledger Summary")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Text Summary")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Financial Summary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Account Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Billed (Debit)", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CurrencyFormatter.format(report.totalDebit, settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text("Total Received (Credit)", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CurrencyFormatter.format(report.totalCredit, settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Balance Due", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CurrencyFormatter.format(report.netBalance, settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.netBalance > 0.001) MaterialTheme.colorScheme.error else Color(0xFF22C55E)
                                )
                            }
                        }
                    }
                }
            }

            // PDF Export, Share & Print Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isGeneratingPdf = true
                                val pdfFile = withContext(Dispatchers.IO) {
                                    PdfGenerator.generateCustomerLedgerPdf(
                                        context = context,
                                        company = company,
                                        customer = report.customer,
                                        ledgerReport = report,
                                        currencySymbol = settings.currency
                                    )
                                }
                                isGeneratingPdf = false
                                ShareUtils.sharePdf(context, pdfFile, "Khata Statement - ${report.customer.name}")
                            }
                        },
                        enabled = !isGeneratingPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isGeneratingPdf = true
                                val pdfFile = withContext(Dispatchers.IO) {
                                    PdfGenerator.generateCustomerLedgerPdf(
                                        context = context,
                                        company = company,
                                        customer = report.customer,
                                        ledgerReport = report,
                                        currencySymbol = settings.currency
                                    )
                                }
                                isGeneratingPdf = false
                                ShareUtils.printPdf(context, pdfFile, "Ledger_${report.customer.name}")
                            }
                        },
                        enabled = !isGeneratingPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Print")
                    }
                }
            }

            // Quick Add Transaction Row for this Customer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showAddOrderDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Order")
                    }

                    FilledTonalButton(
                        onClick = { showAddPaymentDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Payment")
                    }
                }
            }

            // Ledger Entries Timeline Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaction History (${report.entries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (report.entries.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("No transactions logged for this customer yet.")
                        }
                    }
                }
            } else {
                items(report.entries) { entry ->
                    LedgerEntryCard(entry = entry, currencySymbol = settings.currency)
                }
            }
        }
    }

    // Add Order for this customer dialog
    if (showAddOrderDialog) {
        AddOrderDialog(
            customers = rawCustomers,
            selectedCustomerPreselect = report.customer,
            currencySymbol = settings.currency,
            onDismiss = { showAddOrderDialog = false },
            onSave = { custId, items, date, deposit, depDate, notes ->
                viewModel.addOrder(custId, items, date, deposit, depDate, notes) {
                    showAddOrderDialog = false
                }
            }
        )
    }

    // Add Payment for this customer dialog
    if (showAddPaymentDialog) {
        AddPaymentDialog(
            orders = customerOrders,
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
fun LedgerEntryCard(
    entry: LedgerEntry,
    currencySymbol: String
) {
    val isDebit = entry.debitAmount > 0

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
                    color = if (isDebit) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isDebit) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entry.description,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        DateUtils.formatDateTime(entry.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        entry.referenceInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isDebit) {
                    Text(
                        "+${CurrencyFormatter.format(entry.debitAmount, currencySymbol)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("Debit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        "-${CurrencyFormatter.format(entry.creditAmount, currencySymbol)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("Credit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
                }
            }
        }
    }
}
