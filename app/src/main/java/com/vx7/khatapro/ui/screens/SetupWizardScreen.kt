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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vx7.khatapro.R
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1 Fields
    var companyName by remember { mutableStateOf("Sharma General Store") }
    var ownerName by remember { mutableStateOf("Rajesh Sharma") }
    var mobile by remember { mutableStateOf("9876543210") }
    var whatsapp by remember { mutableStateOf("9876543210") }
    var email by remember { mutableStateOf("contact@sharmastore.in") }
    var address by remember { mutableStateOf("Shop 12, Main Bazaar") }
    var city by remember { mutableStateOf("New Delhi") }
    var state by remember { mutableStateOf("Delhi") }
    var gstNumber by remember { mutableStateOf("07AAAAA0000A1Z5") }
    var savedLogoPath by remember { mutableStateOf<String?>(null) }

    // Validation states
    var companyNameError by remember { mutableStateOf<String?>(null) }
    var ownerNameError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Step 2 Fields
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }
    var confirmPassword by remember { mutableStateOf("admin123") }
    var passwordVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    // Modern Photo Picker for Logo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentPath = copyUriToInternalStorage(context, uri)
            savedLogoPath = permanentPath
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VX7 Khata Pro Setup Wizard", fontWeight = FontWeight.Bold)
                        Text(
                            "Step $currentStep of 2: ${if (currentStep == 1) "Company Details" else "Security & Login"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { if (currentStep == 1) 0.5f else 1.0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            )

            if (currentStep == 1) {
                // STEP 1: COMPANY PROFILE
                Text(
                    "Business Profile & Logo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "This information and logo will appear on your receipts, ledger statements, and PDF reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Logo Picker Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                            if (savedLogoPath != null) {
                                AsyncImage(
                                    model = savedLogoPath,
                                    contentDescription = "Company Logo",
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
                            Text("Company Logo", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Choose Logo")
                                }

                                if (savedLogoPath != null) {
                                    OutlinedButton(
                                        onClick = { savedLogoPath = null },
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
                    onValueChange = {
                        companyName = it
                        if (it.isNotBlank()) companyNameError = null
                    },
                    label = { Text("Company / Shop Name *") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    isError = companyNameError != null,
                    supportingText = companyNameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("company_name_input")
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = {
                        ownerName = it
                        if (it.isNotBlank()) ownerNameError = null
                    },
                    label = { Text("Owner / Merchant Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = ownerNameError != null,
                    supportingText = ownerNameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = {
                            mobile = it
                            if (it.isNotBlank()) mobileError = null
                        },
                        label = { Text("Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        isError = mobileError != null,
                        supportingText = mobileError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp (Opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isNotBlank()) emailError = null
                    },
                    label = { Text("Business Email *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        var hasError = false
                        if (companyName.trim().isBlank()) {
                            companyNameError = "Company name is required"
                            hasError = true
                        }
                        if (ownerName.trim().isBlank()) {
                            ownerNameError = "Owner name is required"
                            hasError = true
                        }
                        if (mobile.trim().isBlank()) {
                            mobileError = "Mobile number is required"
                            hasError = true
                        }
                        if (email.trim().isBlank() || !email.contains("@")) {
                            emailError = "Valid business email required"
                            hasError = true
                        }
                        if (!hasError) {
                            currentStep = 2
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("next_step_button")
                ) {
                    Text("Proceed to Login Setup", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }

            } else {
                // STEP 2: LOGIN CREDENTIALS
                Text(
                    "Administrator Security",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Set up your secure master credentials to protect your Khata and customer ledger. Passwords are cryptographically salted and hashed offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Minimum 6 characters required. Stored securely on this Android device.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (it.isNotBlank()) usernameError = null
                    },
                    label = { Text("Admin Username / Login ID *") },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    isError = usernameError != null,
                    supportingText = usernameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (it.length >= 6) passwordError = null
                    },
                    label = { Text("Password (min 6 characters) *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (it == password) confirmPasswordError = null
                    },
                    label = { Text("Confirm Password *") },
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_password_input")
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            var hasError = false
                            if (username.trim().isBlank()) {
                                usernameError = "Username is required"
                                hasError = true
                            }
                            if (password.length < 6) {
                                passwordError = "Password must be at least 6 characters"
                                hasError = true
                            }
                            if (confirmPassword != password) {
                                confirmPasswordError = "Passwords do not match"
                                hasError = true
                            }

                            if (!hasError) {
                                isLoading = true
                                viewModel.completeSetup(
                                    companyName = companyName,
                                    ownerName = ownerName,
                                    mobile = mobile,
                                    whatsapp = whatsapp,
                                    email = email,
                                    address = address,
                                    city = city,
                                    state = state,
                                    gstNumber = gstNumber,
                                    logoPath = savedLogoPath,
                                    username = username,
                                    password = password,
                                    onSuccess = { isLoading = false },
                                    onError = { err ->
                                        isLoading = false
                                        viewModel.showSnackbar(err)
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("finish_setup_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Finish Setup & Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
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
