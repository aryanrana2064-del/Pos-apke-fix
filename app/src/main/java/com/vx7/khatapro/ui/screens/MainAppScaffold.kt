package com.vx7.khatapro.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vx7.khatapro.R
import com.vx7.khatapro.data.database.entities.CompanyProfileEntity
import com.vx7.khatapro.ui.components.ConfirmationDialog
import com.vx7.khatapro.ui.viewmodel.AppScreen
import com.vx7.khatapro.ui.viewmodel.KhataViewModel
import kotlinx.coroutines.launch

data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: KhataViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val company by viewModel.companyProfile.collectAsState()
    val license by viewModel.license.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val bottomNavItems = listOf(
        NavItem(AppScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        NavItem(AppScreen.CUSTOMERS, "Customers", Icons.Default.People),
        NavItem(AppScreen.ORDERS, "Orders", Icons.Default.ShoppingBag),
        NavItem(AppScreen.PAYMENTS, "Payments", Icons.Default.Payments),
        NavItem(AppScreen.REPORTS, "Reports", Icons.Default.BarChart)
    )

    val drawerNavItems = listOf(
        NavItem(AppScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        NavItem(AppScreen.CUSTOMERS, "Customers", Icons.Default.People),
        NavItem(AppScreen.ORDERS, "Orders", Icons.Default.ShoppingBag),
        NavItem(AppScreen.PAYMENTS, "Payments", Icons.Default.Payments),
        NavItem(AppScreen.REPORTS, "Reports", Icons.Default.BarChart),
        NavItem(AppScreen.COMPANY_PROFILE, "Company Profile", Icons.Default.Business),
        NavItem(AppScreen.BACKUP_RESTORE, "Backup & Restore", Icons.Default.Backup),
        NavItem(AppScreen.SETTINGS, "Settings", Icons.Default.Settings),
        NavItem(AppScreen.LICENSE, "License & Info", Icons.Default.Verified)
    )

    val currentTitle = when (currentScreen) {
        AppScreen.DASHBOARD -> company?.companyName ?: "VX7 Khata Pro"
        AppScreen.CUSTOMERS -> "Customer Ledger"
        AppScreen.CUSTOMER_LEDGER -> "Account Statement"
        AppScreen.ORDERS -> "Order Register"
        AppScreen.PAYMENTS -> "Payment Register"
        AppScreen.REPORTS -> "Financial Reports"
        AppScreen.COMPANY_PROFILE -> "Company Profile"
        AppScreen.SETTINGS -> "App Settings"
        AppScreen.LICENSE -> "Product License"
        AppScreen.BACKUP_RESTORE -> "Backup & Restore"
        else -> "VX7 Khata Pro"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!company?.logoPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = company?.logoPath,
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

                        Text(
                            company?.companyName ?: "VX7 Khata Pro Business",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${company?.ownerName ?: "Admin"} • ${company?.mobile ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Pro ${license?.licenseType ?: "Trial"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                drawerNavItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentScreen == item.screen,
                        onClick = {
                            viewModel.navigateTo(item.screen)
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showLogoutConfirmation = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(currentTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            if (currentScreen == AppScreen.DASHBOARD) {
                                Text(
                                    "Smart Khata & Business Ledger",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("drawer_toggle_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.COMPANY_PROFILE) }) {
                            Icon(Icons.Default.Storefront, contentDescription = "Company Profile")
                        }
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                // Show BottomBar on primary tabs
                val showBottomBar = currentScreen in listOf(
                    AppScreen.DASHBOARD,
                    AppScreen.CUSTOMERS,
                    AppScreen.ORDERS,
                    AppScreen.PAYMENTS,
                    AppScreen.REPORTS
                )

                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentScreen == item.screen,
                                onClick = { viewModel.navigateTo(item.screen) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppScreen.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                    AppScreen.CUSTOMER_LEDGER -> CustomerLedgerScreen(viewModel = viewModel)
                    AppScreen.ORDERS -> OrdersScreen(viewModel = viewModel)
                    AppScreen.PAYMENTS -> PaymentsScreen(viewModel = viewModel)
                    AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                    AppScreen.COMPANY_PROFILE -> CompanyProfileScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    AppScreen.LICENSE -> LicenseScreen(viewModel = viewModel)
                    AppScreen.BACKUP_RESTORE -> BackupRestoreScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showLogoutConfirmation) {
        ConfirmationDialog(
            title = "Confirm Logout",
            message = "Are you sure you want to log out of KhataPro?",
            confirmText = "Logout",
            onConfirm = {
                showLogoutConfirmation = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirmation = false }
        )
    }
}
