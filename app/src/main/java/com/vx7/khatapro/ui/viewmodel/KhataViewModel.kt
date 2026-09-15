package com.vx7.khatapro.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vx7.khatapro.data.database.KhataDatabase
import com.vx7.khatapro.data.database.entities.*
import com.vx7.khatapro.data.database.models.*
import com.vx7.khatapro.data.repository.KhataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen {
    SETUP,
    LOGIN,
    DASHBOARD,
    CUSTOMERS,
    CUSTOMER_LEDGER,
    ORDERS,
    PAYMENTS,
    REPORTS,
    COMPANY_PROFILE,
    SETTINGS,
    LICENSE,
    BACKUP_RESTORE
}

class KhataViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KhataDatabase.getDatabase(application)
    val repository = KhataRepository(database)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSetupCompleted = MutableStateFlow(false)
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val user: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val companyProfile: StateFlow<CompanyProfileEntity?> = repository.companyProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<SettingsEntity> = repository.settingsFlow
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    val license: StateFlow<LicenseEntity?> = repository.licenseFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val customersWithLedger: StateFlow<List<CustomerWithLedger>> = repository.customersWithLedgerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawCustomers: StateFlow<List<CustomerEntity>> = repository.customersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ordersWithDetails: StateFlow<List<OrderWithDetails>> = repository.ordersWithDetailsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPaymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = repository.dashboardStatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val backups: StateFlow<List<BackupLogEntity>> = repository.backupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected customer for Ledger screen
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedCustomerLedger: StateFlow<CustomerLedgerReport?> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) repository.getCustomerLedgerReport(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        checkInitialAppState()
    }

    private fun checkInitialAppState() {
        viewModelScope.launch {
            val setupDone = repository.isSetupCompleted()
            _isSetupCompleted.value = setupDone
            if (!setupDone) {
                _currentScreen.value = AppScreen.SETUP
            } else {
                // Check if user enabled remember_login
                val currentSettings = repository.settingsFlow.firstOrNull()
                if (currentSettings?.rememberLogin == true) {
                    _isLoggedIn.value = true
                    _currentScreen.value = AppScreen.DASHBOARD
                } else {
                    _isLoggedIn.value = false
                    _currentScreen.value = AppScreen.LOGIN
                }
            }
            _isInitialized.value = true
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun completeSetup(
        companyName: String,
        ownerName: String,
        mobile: String,
        whatsapp: String,
        email: String,
        address: String,
        city: String,
        state: String,
        gstNumber: String?,
        logoPath: String?,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = repository.completeSetup(
                    companyName, ownerName, mobile, whatsapp, email,
                    address, city, state, gstNumber, logoPath, username, password
                )
                if (success) {
                    _isSetupCompleted.value = true
                    _isLoggedIn.value = true
                    _currentScreen.value = AppScreen.DASHBOARD
                    showSnackbar("Setup completed! Welcome to KhataPro.")
                    onSuccess()
                } else {
                    onError("Failed to save setup data.")
                }
            } catch (e: Exception) {
                onError("Setup error: ${e.localizedMessage}")
            }
        }
    }

    fun login(password: String, remember: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val valid = repository.verifyLogin(password)
                if (valid) {
                    // Update remember setting
                    val currentSettings = settings.value
                    repository.updateSettings(currentSettings.copy(rememberLogin = remember))
                    _isLoggedIn.value = true
                    _currentScreen.value = AppScreen.DASHBOARD
                    showSnackbar("Logged in successfully!")
                    onSuccess()
                } else {
                    onError("Incorrect password. Please try again.")
                }
            } catch (e: Exception) {
                onError("Login error: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoggedIn.value = false
            _currentScreen.value = AppScreen.LOGIN
            // If remember was on, clear it on manual logout
            val currentSettings = settings.value
            repository.updateSettings(currentSettings.copy(rememberLogin = false))
            showSnackbar("Logged out.")
        }
    }

    fun resetAccess(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.resetPassword(newPassword)
                if (success) {
                    showSnackbar("Password reset successfully. You can now login.")
                    onSuccess()
                } else {
                    onError("Failed to reset password.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error resetting password")
            }
        }
    }

    fun addCustomer(name: String, phone: String, address: String, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.addCustomer(name, phone, address, notes)
                showSnackbar("Customer '$name' added successfully!")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error adding customer: ${e.localizedMessage}")
            }
        }
    }

    fun updateCustomer(customer: CustomerEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateCustomer(customer)
                showSnackbar("Customer updated successfully!")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error updating customer: ${e.localizedMessage}")
            }
        }
    }

    fun deleteCustomer(customerId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customerId)
                showSnackbar("Customer deleted.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error deleting customer: ${e.localizedMessage}")
            }
        }
    }

    fun selectCustomerForLedger(customerId: Long) {
        _selectedCustomerId.value = customerId
        _currentScreen.value = AppScreen.CUSTOMER_LEDGER
    }

    fun addOrder(
        customerId: Long,
        items: List<OrderItemInput>,
        orderDate: Long,
        initialDeposit: Double,
        depositDate: Long,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addOrder(
                    customerId = customerId,
                    items = items,
                    orderDate = orderDate,
                    initialDeposit = initialDeposit,
                    depositDate = depositDate,
                    notes = notes
                )
                val itemLabel = if (items.size > 1) "${items.size} items" else "order"
                showSnackbar("Order created successfully with $itemLabel!")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error creating order: ${e.localizedMessage}")
            }
        }
    }

    fun updateOrder(order: OrderEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateOrder(order)
                showSnackbar("Order updated.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error updating order: ${e.localizedMessage}")
            }
        }
    }

    fun deleteOrder(orderId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteOrder(orderId)
                showSnackbar("Order deleted.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error deleting order: ${e.localizedMessage}")
            }
        }
    }

    fun addPayment(
        orderId: Long,
        customerId: Long,
        amount: Double,
        paymentDate: Long,
        paymentMethod: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addPayment(
                    orderId = orderId,
                    customerId = customerId,
                    amount = amount,
                    paymentDate = paymentDate,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
                showSnackbar("Payment of ₹%.2f recorded!".format(amount))
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error adding payment: ${e.localizedMessage}")
            }
        }
    }

    fun updatePayment(payment: PaymentEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updatePayment(payment)
                showSnackbar("Payment updated.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error updating payment: ${e.localizedMessage}")
            }
        }
    }

    fun deletePayment(paymentId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePayment(paymentId)
                showSnackbar("Payment deleted.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error deleting payment: ${e.localizedMessage}")
            }
        }
    }

    fun updateCompanyProfile(profile: CompanyProfileEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateCompanyProfile(profile)
                showSnackbar("Company profile updated.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Error updating profile: ${e.localizedMessage}")
            }
        }
    }

    fun updateSettings(settings: SettingsEntity) {
        viewModelScope.launch {
            try {
                repository.updateSettings(settings)
                showSnackbar("Settings saved.")
            } catch (e: Exception) {
                showSnackbar("Error saving settings: ${e.localizedMessage}")
            }
        }
    }

    fun activateLicense(key: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.activateLicense(key)
            result.onSuccess { msg ->
                showSnackbar(msg)
                onSuccess()
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Invalid License")
            }
        }
    }

    fun createBackup(context: Context, onSuccess: (File) -> Unit) {
        viewModelScope.launch {
            val result = repository.createBackup(context)
            result.onSuccess { file ->
                showSnackbar("Backup created successfully: ${file.name}")
                onSuccess(file)
            }.onFailure { err ->
                showSnackbar("Backup failed: ${err.localizedMessage}")
            }
        }
    }

    fun exportExcel(context: Context, onSuccess: (File) -> Unit) {
        viewModelScope.launch {
            val result = repository.exportExcel(context)
            result.onSuccess { file ->
                showSnackbar("Excel file created: ${file.name}")
                onSuccess(file)
            }.onFailure { err ->
                showSnackbar("Excel export failed: ${err.localizedMessage}")
            }
        }
    }

    fun importExcel(context: Context, file: File, onResult: (com.vx7.khatapro.core.utils.BackupManager.ExcelImportSummary) -> Unit) {
        viewModelScope.launch {
            val result = repository.importExcel(context, file)
            result.onSuccess { summary ->
                showSnackbar("Excel import complete")
                onResult(summary)
            }.onFailure { err ->
                showSnackbar("Excel import failed: ${err.localizedMessage}")
            }
        }
    }

    fun restoreBackup(context: Context, file: File, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreBackup(context, file)
            result.onSuccess {
                showSnackbar("Backup restored successfully!")
                checkInitialAppState()
                onSuccess()
            }.onFailure { err ->
                showSnackbar("Restore failed: ${err.localizedMessage}")
            }
        }
    }

    fun factoryReset(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.resetDatabase()
                _isLoggedIn.value = false
                _isSetupCompleted.value = false
                _currentScreen.value = AppScreen.SETUP
                showSnackbar("All data has been reset.")
                onSuccess()
            } catch (e: Exception) {
                showSnackbar("Reset failed: ${e.localizedMessage}")
            }
        }
    }
}
