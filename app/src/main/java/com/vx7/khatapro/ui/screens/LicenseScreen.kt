package com.vx7.khatapro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.ui.viewmodel.KhataViewModel

@Composable
fun LicenseScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val license by viewModel.license.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    var isActivating by remember { mutableStateOf(false) }

    val isLifetime = license?.licenseType.equals("Lifetime", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("License & Offline Activation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (license?.status == "Active") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (license?.status == "Active") Icons.Default.Verified else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (license?.status == "Active") Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                        )
                        Text(
                            license?.licenseType ?: "Standard Trial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            license?.status ?: "Active",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("License Key:", style = MaterialTheme.typography.bodySmall)
                    Text(license?.licenseKey ?: "N/A", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Valid Until:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (isLifetime) "Never Expires (Lifetime)" else license?.expiryDate?.let { DateUtils.formatDate(it) } ?: "N/A",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Device ID:", style = MaterialTheme.typography.bodySmall)
                    Text(license?.deviceId ?: "KP-DEVICE", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Activation Form Card
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
                Text("Activate Product Key", fontWeight = FontWeight.Bold)
                Text(
                    "Enter your KhataPro activation license key. Available offline keys: 'KP-PRO-LIFETIME-2026' or 'KP-PRO-ANNUAL-2026'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        if (it.isNotBlank()) keyError = null
                    },
                    label = { Text("Product License Key") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    isError = keyError != null,
                    supportingText = keyError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_key_input")
                )

                Button(
                    onClick = {
                        if (keyInput.trim().isBlank()) {
                            keyError = "Please enter an activation key"
                        } else {
                            isActivating = true
                            viewModel.activateLicense(
                                key = keyInput.trim(),
                                onSuccess = {
                                    isActivating = false
                                    keyInput = ""
                                },
                                onError = { err ->
                                    isActivating = false
                                    keyError = err
                                }
                            )
                        }
                    },
                    enabled = !isActivating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isActivating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Activate License")
                    }
                }
            }
        }

        // Offline Assurance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("100% Offline Software", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "VX7 Khata Pro is completely self-contained on your Android device. It requires no continuous internet access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
