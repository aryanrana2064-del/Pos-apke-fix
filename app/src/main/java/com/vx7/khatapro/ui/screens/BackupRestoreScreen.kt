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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.core.utils.ShareUtils
import com.vx7.khatapro.data.database.entities.BackupLogEntity
import com.vx7.khatapro.ui.components.ConfirmationDialog
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import java.io.File

@Composable
fun BackupRestoreScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backups by viewModel.backups.collectAsState()

    var isCreatingBackup by remember { mutableStateOf(false) }
    var selectedBackupToRestore by remember { mutableStateOf<File?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Backup & Restore Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Securely export all company records, customer accounts, orders, and payment histories to offline JSON snapshot files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Create Instant Snapshot", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        "Generates a complete offline backup file saved safely to your Android storage. You can share this file to Google Drive, WhatsApp, or computer.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            isCreatingBackup = true
                            viewModel.createBackup(context) { file ->
                                isCreatingBackup = false
                                ShareUtils.sharePdf(context, file, "VX7 Khata Pro Data Backup")
                            }
                        },
                        enabled = !isCreatingBackup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_backup_button")
                    ) {
                        if (isCreatingBackup) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create & Share Backup Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Backup History
        item {
            Text(
                "Local Backup Snapshots (${backups.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (backups.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No backup files recorded yet.", fontWeight = FontWeight.Medium)
                        Text("Tap 'Create & Share Backup' above to create one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(backups) { log ->
                val backupFile = File(log.filePath)
                val exists = backupFile.exists()

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.backupName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Date: ${DateUtils.formatDateTime(log.backupDate)} • ${log.sizeBytes / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (exists) {
                                IconButton(onClick = { ShareUtils.sharePdf(context, backupFile, "VX7 Khata Pro Backup") }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { selectedBackupToRestore = backupFile }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.secondary)
                                }
                            } else {
                                Text("File Moved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedBackupToRestore?.let { file ->
        ConfirmationDialog(
            title = "Restore from Backup?",
            message = "Restoring from '${file.name}' will update your database records with the snapshot data. Continue?",
            confirmText = "Restore Data",
            onConfirm = {
                viewModel.restoreBackup(context, file) {
                    selectedBackupToRestore = null
                }
            },
            onDismiss = { selectedBackupToRestore = null }
        )
    }
}
