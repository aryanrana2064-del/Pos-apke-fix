package com.vx7.khatapro.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vx7.khatapro.R
import com.vx7.khatapro.data.database.entities.CompanyProfileEntity
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val company by viewModel.companyProfile.collectAsState()

    var companyName by remember(company) { mutableStateOf(company?.companyName ?: "") }
    var ownerName by remember(company) { mutableStateOf(company?.ownerName ?: "") }
    var mobile by remember(company) { mutableStateOf(company?.mobile ?: "") }
    var whatsapp by remember(company) { mutableStateOf(company?.whatsapp ?: "") }
    var email by remember(company) { mutableStateOf(company?.email ?: "") }
    var address by remember(company) { mutableStateOf(company?.address ?: "") }
    var city by remember(company) { mutableStateOf(company?.city ?: "") }
    var state by remember(company) { mutableStateOf(company?.state ?: "") }
    var gstNumber by remember(company) { mutableStateOf(company?.gstNumber ?: "") }
    var logoPath by remember(company) { mutableStateOf(company?.logoPath) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanent = copyLogoToInternal(context, uri)
            logoPath = permanent
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Company Profile & Branding", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "These details will appear on your tax receipts, customer statements, and shared PDF invoices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Logo Row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!logoPath.isNullOrBlank()) {
                        AsyncImage(
                            model = logoPath,
                            contentDescription = "Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_khata_logo),
                            contentDescription = "Default Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Business Logo", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Change Logo")
                        }
                        if (logoPath != null) {
                            OutlinedButton(
                                onClick = { logoPath = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Company / Business Name *") },
            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = { Text("Merchant / Owner Name *") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp") },
                leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email *") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text("State") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = gstNumber,
            onValueChange = { gstNumber = it },
            label = { Text("GSTIN / Tax ID (Optional)") },
            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                val updated = (company ?: CompanyProfileEntity()).copy(
                    companyName = companyName.trim(),
                    ownerName = ownerName.trim(),
                    mobile = mobile.trim(),
                    whatsapp = whatsapp.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    city = city.trim(),
                    state = state.trim(),
                    gstNumber = gstNumber.trim().ifBlank { null },
                    logoPath = logoPath
                )
                viewModel.updateCompanyProfile(updated) {}
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_company_profile_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(80.dp))
    }
}

private fun copyLogoToInternal(context: Context, uri: Uri): String? {
    return try {
        val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
        val logoFile = File(logosDir, "company_logo_${System.currentTimeMillis()}.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(logoFile).use { output ->
                input.copyTo(output)
            }
        }
        logoFile.absolutePath
    } catch (_: Exception) {
        null
    }
}
