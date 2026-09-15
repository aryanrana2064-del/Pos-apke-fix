package com.vx7.khatapro.data.repository

import android.content.Context
import com.vx7.khatapro.core.utils.BackupManager
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.core.utils.SecurityUtils
import com.vx7.khatapro.data.database.KhataDatabase
import com.vx7.khatapro.data.database.entities.*
import com.vx7.khatapro.data.database.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

class KhataRepository(private val database: KhataDatabase) {

    // User & Setup
    val userFlow: Flow<UserEntity?> = database.userDao().getUserFlow()
    val companyProfileFlow: Flow<CompanyProfileEntity?> = database.companyProfileDao().getProfileFlow()
    val settingsFlow: Flow<SettingsEntity?> = database.settingsDao().getSettingsFlow()
    val licenseFlow: Flow<LicenseEntity?> = database.licenseDao().getLicenseFlow()
    val customersFlow: Flow<List<CustomerEntity>> = database.customerDao().getAllCustomersFlow()
    val allOrdersFlow: Flow<List<OrderEntity>> = database.orderDao().getAllOrdersFlow()
    val allOrderItemsFlow: Flow<List<OrderItemEntity>> = database.orderItemDao().getAllOrderItemsFlow()
    val allPaymentsFlow: Flow<List<PaymentEntity>> = database.paymentDao().getAllPaymentsFlow()
    val backupsFlow: Flow<List<BackupLogEntity>> = database.backupLogDao().getAllBackupsFlow()

    suspend fun isSetupCompleted(): Boolean = withContext(Dispatchers.IO) {
        val userCount = database.userDao().getUserCount()
        val company = database.companyProfileDao().getProfileSync()
        userCount > 0 && company != null
    }

    suspend fun completeSetup(
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
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        val company = CompanyProfileEntity(
            companyName = companyName.trim(),
            ownerName = ownerName.trim(),
            mobile = mobile.trim(),
            whatsapp = whatsapp.trim().ifBlank { mobile.trim() },
            email = email.trim(),
            address = address.trim(),
            city = city.trim(),
            state = state.trim(),
            gstNumber = gstNumber?.trim()?.ifBlank { null },
            logoPath = logoPath
        )
        database.companyProfileDao().insertProfile(company)

        val user = UserEntity(
            username = username.trim(),
            passwordHash = SecurityUtils.hashPassword(password)
        )
        database.userDao().insertUser(user)

        // Default Settings
        val settings = SettingsEntity(
            theme = "system",
            currency = "₹",
            dateFormat = "dd/MM/yyyy",
            rememberLogin = true
        )
        database.settingsDao().insertSettings(settings)

        // Initialize 30-Day Pro Trial License
        val trialExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        val license = LicenseEntity(
            licenseKey = "KP-TRIAL-30DAYS",
            status = "Active",
            licenseType = "Trial",
            expiryDate = trialExpiry,
            deviceId = SecurityUtils.getOrCreateDeviceId()
        )
        database.licenseDao().insertLicense(license)

        true
    }

    suspend fun verifyLogin(password: String): Boolean = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserSync() ?: return@withContext false
        SecurityUtils.verifyPassword(password, user.passwordHash)
    }

    suspend fun resetPassword(newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserSync() ?: return@withContext false
        val newHash = SecurityUtils.hashPassword(newPassword)
        database.userDao().updatePassword(user.id, newHash)
        true
    }

    // Orders with details combined with items, payments and customers
    val ordersWithDetailsFlow: Flow<List<OrderWithDetails>> = combine(
        allOrdersFlow,
        allOrderItemsFlow,
        customersFlow,
        allPaymentsFlow
    ) { orders, orderItems, customers, payments ->
        val customerMap = customers.associateBy { it.id }
        val paymentGroup = payments.groupBy { it.orderId }
        val itemsGroup = orderItems.groupBy { it.orderId }

        orders.map { order ->
            val cust = customerMap[order.customerId]
            val orderPayments = paymentGroup[order.id] ?: emptyList()
            val totalPaid = orderPayments.sumOf { it.amount }
            val balance = (order.total - totalPaid).coerceAtLeast(0.0)
            val latestPaymentDate = orderPayments.maxOfOrNull { it.paymentDate }

            val status = when {
                balance <= 0.001 -> OrderStatus.PAID
                totalPaid > 0 -> OrderStatus.PARTIALLY_PAID
                else -> OrderStatus.UNPAID
            }

            OrderWithDetails(
                order = order,
                items = itemsGroup[order.id] ?: emptyList(),
                customerName = cust?.name ?: "Unknown Customer",
                customerPhone = cust?.phone ?: "",
                totalPaid = totalPaid,
                balance = balance,
                latestPaymentDate = latestPaymentDate,
                status = status,
                payments = orderPayments
            )
        }
    }

    // Customers with Ledger Summary
    val customersWithLedgerFlow: Flow<List<CustomerWithLedger>> = combine(
        customersFlow,
        allOrdersFlow,
        allPaymentsFlow
    ) { customers, orders, payments ->
        val ordersByCustomer = orders.groupBy { it.customerId }
        val paymentsByCustomer = payments.groupBy { it.customerId }

        customers.map { cust ->
            val custOrders = ordersByCustomer[cust.id] ?: emptyList()
            val custPayments = paymentsByCustomer[cust.id] ?: emptyList()

            val totalOrderAmount = custOrders.sumOf { it.total }
            val totalPaidAmount = custPayments.sumOf { it.amount }
            val balance = (totalOrderAmount - totalPaidAmount).coerceAtLeast(0.0)

            CustomerWithLedger(
                customer = cust,
                totalOrderAmount = totalOrderAmount,
                totalPaidAmount = totalPaidAmount,
                balance = balance,
                orderCount = custOrders.size
            )
        }
    }

    // Reactive Dashboard Statistics
    val dashboardStatsFlow: Flow<DashboardStats> = combine(
        customersFlow,
        allOrdersFlow,
        allPaymentsFlow
    ) { customers, orders, payments ->
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.getStartOfDay(now)
        val endOfToday = DateUtils.getEndOfDay(now)

        val totalCustomers = customers.size
        val totalOrders = orders.size
        val totalOrderAmount = orders.sumOf { it.total }
        val totalDeposits = payments.sumOf { it.amount }
        val totalOutstanding = (totalOrderAmount - totalDeposits).coerceAtLeast(0.0)

        val todayOrders = orders.filter { it.orderDate in startOfToday..endOfToday }
        val todayPayments = payments.filter { it.paymentDate in startOfToday..endOfToday }

        DashboardStats(
            totalCustomers = totalCustomers,
            totalOrders = totalOrders,
            totalOrderAmount = totalOrderAmount,
            totalDeposits = totalDeposits,
            totalOutstanding = totalOutstanding,
            todayOrdersCount = todayOrders.size,
            todayOrderAmount = todayOrders.sumOf { it.total },
            todayDepositsCount = todayPayments.size,
            todayDepositsAmount = todayPayments.sumOf { it.amount }
        )
    }

    // Customer Operations
    suspend fun addCustomer(name: String, phone: String, address: String, notes: String): Long = withContext(Dispatchers.IO) {
        val customer = CustomerEntity(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            notes = notes.trim()
        )
        database.customerDao().insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        database.customerDao().updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCustomer(customerId: Long) = withContext(Dispatchers.IO) {
        database.customerDao().deleteCustomerById(customerId)
    }

    // Order Operations
    // Creates ONE order that can carry MULTIPLE line items (e.g. a customer
    // taking 3 shirts + 2 trousers in a single visit) instead of forcing a
    // separate order per item. The order's total, and the deposit/balance
    // shown for it, are computed across all items together.
    suspend fun addOrder(
        customerId: Long,
        items: List<OrderItemInput>,
        orderDate: Long,
        initialDeposit: Double,
        depositDate: Long,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        require(items.isNotEmpty()) { "An order needs at least one item" }
        val total = items.sumOf { it.total }
        val order = OrderEntity(
            customerId = customerId,
            orderDate = orderDate,
            total = total,
            notes = notes.trim()
        )
        val orderId = database.orderDao().insertOrder(order)

        val itemEntities = items.map { input ->
            OrderItemEntity(
                orderId = orderId,
                item = input.item.trim(),
                quantity = input.quantity,
                rate = input.rate,
                total = input.total
            )
        }
        database.orderItemDao().insertItems(itemEntities)

        // Automatically create initial payment record if deposit entered
        if (initialDeposit > 0) {
            val payment = PaymentEntity(
                orderId = orderId,
                customerId = customerId,
                amount = initialDeposit,
                paymentDate = depositDate,
                paymentMethod = "Cash",
                notes = "Initial deposit at order creation"
            )
            database.paymentDao().insertPayment(payment)
        }

        orderId
    }

    // Replaces every line item on an existing order with a new set (used when
    // editing an order) and recalculates the order's total accordingly.
    suspend fun updateOrderItems(
        orderId: Long,
        items: List<OrderItemInput>,
        orderDate: Long,
        notes: String
    ) = withContext(Dispatchers.IO) {
        require(items.isNotEmpty()) { "An order needs at least one item" }
        val existing = database.orderDao().getOrderByIdSync(orderId) ?: return@withContext
        val total = items.sumOf { it.total }
        database.orderDao().updateOrder(
            existing.copy(orderDate = orderDate, total = total, notes = notes.trim(), updatedAt = System.currentTimeMillis())
        )
        database.orderItemDao().deleteItemsForOrder(orderId)
        database.orderItemDao().insertItems(
            items.map { input ->
                OrderItemEntity(
                    orderId = orderId,
                    item = input.item.trim(),
                    quantity = input.quantity,
                    rate = input.rate,
                    total = input.total
                )
            }
        )
    }

    suspend fun updateOrder(order: OrderEntity) = withContext(Dispatchers.IO) {
        database.orderDao().updateOrder(order.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteOrder(orderId: Long) = withContext(Dispatchers.IO) {
        database.orderDao().deleteOrderById(orderId)
    }

    // Payment Operations
    suspend fun addPayment(
        orderId: Long,
        customerId: Long,
        amount: Double,
        paymentDate: Long,
        paymentMethod: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val payment = PaymentEntity(
            orderId = orderId,
            customerId = customerId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            notes = notes.trim()
        )
        database.paymentDao().insertPayment(payment)
    }

    suspend fun updatePayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        database.paymentDao().updatePayment(payment.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePayment(paymentId: Long) = withContext(Dispatchers.IO) {
        database.paymentDao().deletePaymentById(paymentId)
    }

    // Customer Ledger Report Generator
    fun getCustomerLedgerReport(customerId: Long): Flow<CustomerLedgerReport?> {
        return combine(
            database.customerDao().getCustomerByIdFlow(customerId),
            database.orderDao().getOrdersByCustomerFlow(customerId),
            allOrderItemsFlow,
            database.paymentDao().getPaymentsForCustomerFlow(customerId)
        ) { customer, orders, allItems, payments ->
            if (customer == null) return@combine null

            val entries = mutableListOf<LedgerEntry>()
            val itemsByOrder = allItems.groupBy { it.orderId }

            val paymentsByOrder = payments.groupBy { it.orderId }

            for (order in orders) {
                val orderPayments = paymentsByOrder[order.id] ?: emptyList()
                val paidOnOrder = orderPayments.sumOf { it.amount }
                val orderBalance = (order.total - paidOnOrder).coerceAtLeast(0.0)
                val orderItems = itemsByOrder[order.id] ?: emptyList()
                entries.add(LedgerEntry.OrderItem(order, orderItems, paidOnOrder, orderBalance))
            }

            for (payment in payments) {
                val associatedOrder = orders.find { it.id == payment.orderId }
                val itemName = associatedOrder?.let { summarizeOrderItems(itemsByOrder[it.id] ?: emptyList()) }
                    ?: "Order #${payment.orderId}"
                entries.add(LedgerEntry.PaymentItem(payment, itemName))
            }

            entries.sortBy { it.date }

            val totalDebit = orders.sumOf { it.total }
            val totalCredit = payments.sumOf { it.amount }
            val netBalance = (totalDebit - totalCredit).coerceAtLeast(0.0)

            CustomerLedgerReport(
                customer = customer,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = netBalance,
                entries = entries
            )
        }
    }

    // Company Profile Updates
    suspend fun updateCompanyProfile(profile: CompanyProfileEntity) = withContext(Dispatchers.IO) {
        database.companyProfileDao().insertProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    // Settings Updates
    suspend fun updateSettings(settings: SettingsEntity) = withContext(Dispatchers.IO) {
        database.settingsDao().insertSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    // License Activation
    suspend fun activateLicense(key: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = key.trim().uppercase()
        when {
            trimmedKey.contains("LIFETIME") || trimmedKey == "KP-PRO-LIFETIME-2026" -> {
                val license = LicenseEntity(
                    licenseKey = trimmedKey,
                    status = "Active",
                    licenseType = "Lifetime",
                    expiryDate = Long.MAX_VALUE,
                    deviceId = SecurityUtils.getOrCreateDeviceId()
                )
                database.licenseDao().insertLicense(license)
                Result.success("Lifetime License Activated Successfully!")
            }
            trimmedKey.contains("ANNUAL") || trimmedKey == "KP-PRO-ANNUAL-2026" -> {
                val oneYear = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
                val license = LicenseEntity(
                    licenseKey = trimmedKey,
                    status = "Active",
                    licenseType = "Yearly",
                    expiryDate = oneYear,
                    deviceId = SecurityUtils.getOrCreateDeviceId()
                )
                database.licenseDao().insertLicense(license)
                Result.success("Annual License Activated Successfully!")
            }
            trimmedKey.contains("PRO") -> {
                val ninetyDays = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)
                val license = LicenseEntity(
                    licenseKey = trimmedKey,
                    status = "Active",
                    licenseType = "Quarterly",
                    expiryDate = ninetyDays,
                    deviceId = SecurityUtils.getOrCreateDeviceId()
                )
                database.licenseDao().insertLicense(license)
                Result.success("Professional License Activated Successfully!")
            }
            else -> {
                Result.failure(IllegalArgumentException("Invalid License Key. Please enter a valid KhataPro activation key."))
            }
        }
    }

    // Backup & Restore
    suspend fun createBackup(context: Context): Result<File> {
        return BackupManager.createBackup(context, database)
    }

    suspend fun restoreBackup(context: Context, file: File): Result<Boolean> {
        return BackupManager.restoreBackup(context, database, file)
    }

    suspend fun exportExcel(context: Context): Result<File> {
        return BackupManager.exportExcel(context, database)
    }

    suspend fun importExcel(context: Context, file: File): Result<BackupManager.ExcelImportSummary> {
        return BackupManager.importExcel(context, database, file)
    }

    // Factory Reset Database
    suspend fun resetDatabase() = withContext(Dispatchers.IO) {
        database.paymentDao().clearPayments()
        database.orderItemDao().clearOrderItems()
        database.orderDao().clearOrders()
        database.customerDao().clearCustomers()
        database.backupLogDao().clearBackups()
        database.licenseDao().clearLicense()
        database.settingsDao().clearSettings()
        database.companyProfileDao().clearProfile()
        database.userDao().clearUsers()
    }
}
