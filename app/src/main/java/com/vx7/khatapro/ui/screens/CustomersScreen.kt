package com.vx7.khatapro.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.data.database.entities.CustomerEntity
import com.vx7.khatapro.data.database.models.CustomerWithLedger
import com.vx7.khatapro.ui.components.AddCustomerDialog
import com.vx7.khatapro.ui.components.ConfirmationDialog
import com.vx7.khatapro.ui.viewmodel.KhataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customersWithLedger by viewModel.customersWithLedger.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customersWithLedger, searchQuery) {
        if (searchQuery.isBlank()) {
            customersWithLedger
        } else {
            val q = searchQuery.trim().lowercase()
            customersWithLedger.filter {
                it.customer.name.lowercase().contains(q) ||
                        it.customer.phone.contains(q) ||
                        it.customer.address.lowercase().contains(q)
            }
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
                label = { Text("Search by name or phone") },
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
                    .testTag("search_customers_input")
            )

            Spacer(Modifier.height(12.dp))

            // Customer List
            if (filteredCustomers.isEmpty()) {
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
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isNotBlank()) "No customers match '$searchQuery'"
                            else "No customers registered yet",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tap the '+' button below to add your first customer.",
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
                    items(filteredCustomers, key = { it.customer.id }) { item ->
                        CustomerLedgerCard(
                            item = item,
                            currencySymbol = settings.currency,
                            onClick = { viewModel.selectCustomerForLedger(item.customer.id) },
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${item.customer.phone}")
                                }
                                context.startActivity(intent)
                            },
                            onEdit = { customerToEdit = item.customer },
                            onDelete = { customerToDelete = item.customer }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_customer_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, address, notes ->
                viewModel.addCustomer(name, phone, address, notes) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Customer Dialog
    customerToEdit?.let { cust ->
        AddCustomerDialog(
            initialCustomer = cust,
            onDismiss = { customerToEdit = null },
            onSave = { name, phone, address, notes ->
                viewModel.updateCustomer(cust.copy(name = name, phone = phone, address = address, notes = notes)) {
                    customerToEdit = null
                }
            }
        )
    }

    // Delete Confirmation
    customerToDelete?.let { cust ->
        ConfirmationDialog(
            title = "Delete Customer?",
            message = "Are you sure you want to delete '${cust.name}'? This will permanently delete all associated orders and payments.",
            confirmText = "Delete Permanently",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCustomer(cust.id) {
                    customerToDelete = null
                }
            },
            onDismiss = { customerToDelete = null }
        )
    }
}

@Composable
fun CustomerLedgerCard(
    item: CustomerWithLedger,
    currencySymbol: String,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        item.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.balance > 0.001) "Due: ${CurrencyFormatter.format(item.balance, currencySymbol)}" else "No Dues",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.balance > 0.001) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${item.orderCount} Orders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.customer.address.isNotBlank()) {
                Text(
                    item.customer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Button(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("View Ledger", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
