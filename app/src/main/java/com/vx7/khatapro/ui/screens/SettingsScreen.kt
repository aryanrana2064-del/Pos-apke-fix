package com.vx7.khatapro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.vx7.khatapro.core.utils.BackupManager
import com.vx7.khatapro.core.utils.ShareUtils
import com.vx7.khatapro.ui.components.ConfirmationDialog
import com.vx7.khatapro.ui.viewmodel.AppScreen
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val license by viewModel.license.collectAsState()

    val context = LocalContext.current
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isExportingExcel by remember { mutableStateOf(false) }
    var isImportingExcel by remember { mutableStateOf(false) }
    var importSummary by remember { mutableStateOf<BackupManager.ExcelImportSummary?>(null) }

    val importExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImportingExcel = true
        try {
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.xlsx")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.importExcel(context, tempFile) { summary ->
                isImportingExcel = false
                importSummary = summary
                tempFile.delete()
            }
        } catch (e: Exception) {
            isImportingExcel = false
            Toast.makeText(context, "Could not read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val currencies = listOf("₹" to "INR (₹)", "$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)", "AED " to "AED")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Application Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // General Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Regional & Display", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                // Currency Dropdown
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = currencies.find { it.first == settings.currency }?.second ?: settings.currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency Symbol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        currencies.forEach { (sym, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateSettings(settings.copy(currency = sym))
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remember Login Session", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Stay logged in when opening the app without requiring master password each time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.rememberLogin,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(rememberLogin = it)) }
                    )
                }
            }
        }

        // Navigation Shortcuts Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Database & License", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                ListItem(
                    headlineContent = { Text("Database Backup & Restore") },
                    supportingContent = { Text("Create offline JSON snapshots or restore previous backups") },
                    leadingContent = { Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.BACKUP_RESTORE) }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Export to Excel") },
                    supportingContent = { Text("Save Customers, Orders & Payments as an offline .xlsx file") },
                    leadingContent = {
                        if (isExportingExcel) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier
                        .clickable(enabled = !isExportingExcel) {
                            isExportingExcel = true
                            viewModel.exportExcel(context) { file ->
                                isExportingExcel = false
                                ShareUtils.shareFile(
                                    context,
                                    file,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "VX7 Khata Excel Export"
                                )
                            }
                        }
                        .testTag("export_excel_button")
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Import from Excel") },
                    supportingContent = { Text("Add Customers, Orders & Payments from a .xlsx file (existing records are kept)") },
                    leadingContent = {
                        if (isImportingExcel) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    modifier = Modifier
                        .clickable(enabled = !isImportingExcel) {
                            importExcelLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                        .testTag("import_excel_button")
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("License & Offline Activation") },
                    supportingContent = { Text("Current Status: ${license?.licenseType ?: "Trial"} (${license?.status ?: "Active"})") },
                    leadingContent = { Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.LICENSE) }
                )
            }
        }

        // Danger Zone: Factory Reset
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Danger Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Factory reset will permanently wipe all customers, orders, payments, company profile, and settings from this device. Please ensure you have made a backup first.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { showResetConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("factory_reset_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Wipe Database & Reset App")
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showResetConfirmation) {
        ConfirmationDialog(
            title = "Factory Reset Database?",
            message = "This action CANNOT be undone. All offline ledger entries, orders, and company configurations will be permanently deleted.",
            confirmText = "Yes, Wipe Everything",
            isDestructive = true,
            onConfirm = {
                viewModel.factoryReset {
                    showResetConfirmation = false
                }
            },
            onDismiss = { showResetConfirmation = false }
        )
    }

    importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { importSummary = null },
            icon = { Icon(Icons.Default.GridOn, contentDescription = null) },
            title = { Text("Excel Import Complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Customers added: ${summary.customersAdded}" + if (summary.customersSkipped > 0) " (${summary.customersSkipped} skipped)" else "")
                    Text("Orders added: ${summary.ordersAdded}" + if (summary.ordersSkipped > 0) " (${summary.ordersSkipped} skipped)" else "")
                    Text("Payments added: ${summary.paymentsAdded}" + if (summary.paymentsSkipped > 0) " (${summary.paymentsSkipped} skipped)" else "")
                }
            },
            confirmButton = {
                TextButton(onClick = { importSummary = null }) { Text("OK") }
            }
        )
    }
}
