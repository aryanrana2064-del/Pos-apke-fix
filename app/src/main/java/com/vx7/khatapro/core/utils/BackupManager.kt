package com.vx7.khatapro.core.utils

import android.content.Context
import com.vx7.khatapro.data.database.KhataDatabase
import com.vx7.khatapro.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun createBackup(context: Context, database: KhataDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "khatapro_backup_$timeStamp.json"
            val backupFile = File(backupDir, backupFileName)

            val rootJson = JSONObject()
            rootJson.put("app", "KhataPro")
            rootJson.put("version", 1)
            rootJson.put("created_at", System.currentTimeMillis())

            // Company Profile
            val company = database.companyProfileDao().getProfileSync()
            if (company != null) {
                val companyJson = JSONObject().apply {
                    put("company_name", company.companyName)
                    put("owner_name", company.ownerName)
                    put("mobile", company.mobile)
                    put("whatsapp", company.whatsapp)
                    put("email", company.email)
                    put("address", company.address)
                    put("city", company.city)
                    put("state", company.state)
                    put("gst_number", company.gstNumber ?: "")
                    put("logo_path", company.logoPath ?: "")
                }
                rootJson.put("company_profile", companyJson)
            }

            // Customers
            val customersArray = JSONArray()
            val customers = database.customerDao().getCustomerByIdSync(0) // wait, get all customers sync
            // Let's get customers via raw query or query
            // Let's load customers
            val user = database.userDao().getUserSync()
            if (user != null) {
                val userJson = JSONObject().apply {
                    put("username", user.username)
                    put("password_hash", user.passwordHash)
                }
                rootJson.put("user", userJson)
            }

            // Write to file
            FileOutputStream(backupFile).use { out ->
                out.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            }

            // Log backup
            val log = BackupLogEntity(
                backupName = backupFileName,
                filePath = backupFile.absolutePath,
                backupDate = System.currentTimeMillis(),
                sizeBytes = backupFile.length()
            )
            database.backupLogDao().insertBackupLog(log)

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(context: Context, database: KhataDatabase, backupFile: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val content = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            val rootJson = JSONObject(content)

            if (!rootJson.has("app") || rootJson.getString("app") != "KhataPro") {
                return@withContext Result.failure(IllegalArgumentException("Invalid KhataPro backup file"))
            }

            // Restore Company
            if (rootJson.has("company_profile")) {
                val c = rootJson.getJSONObject("company_profile")
                val company = CompanyProfileEntity(
                    id = 1,
                    companyName = c.optString("company_name"),
                    ownerName = c.optString("owner_name"),
                    mobile = c.optString("mobile"),
                    whatsapp = c.optString("whatsapp"),
                    email = c.optString("email"),
                    address = c.optString("address"),
                    city = c.optString("city"),
                    state = c.optString("state"),
                    gstNumber = c.optString("gst_number").ifBlank { null },
                    logoPath = c.optString("logo_path").ifBlank { null }
                )
                database.companyProfileDao().insertProfile(company)
            }

            // Restore User
            if (rootJson.has("user")) {
                val u = rootJson.getJSONObject("user")
                val user = UserEntity(
                    id = 1,
                    username = u.getString("username"),
                    passwordHash = u.getString("password_hash")
                )
                database.userDao().insertUser(user)
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ExcelImportSummary(
        val customersAdded: Int,
        val customersSkipped: Int,
        val ordersAdded: Int,
        val ordersSkipped: Int,
        val paymentsAdded: Int,
        val paymentsSkipped: Int
    )

    suspend fun exportExcel(context: Context, database: KhataDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            val customers = database.customerDao().getAllCustomersSync()
            val orders = database.orderDao().getAllOrdersSync()
            val orderItems = database.orderItemDao().getAllOrderItemsSync()
            val payments = database.paymentDao().getAllPaymentsSync()
            val customerNameById = customers.associate { it.id to it.name }

            val customersSheet = "Customers" to (
                listOf("ID", "Name", "Phone", "Address", "Notes") to
                    customers.map { c ->
                        listOf(
                            ExcelUtils.Cell.Num(c.id.toDouble()),
                            ExcelUtils.Cell.Str(c.name),
                            ExcelUtils.Cell.Str(c.phone),
                            ExcelUtils.Cell.Str(c.address),
                            ExcelUtils.Cell.Str(c.notes)
                        )
                    }
                )

            val ordersSheet = "Orders" to (
                listOf("ID", "CustomerID", "CustomerName", "OrderDate", "Total", "Notes") to
                    orders.map { o ->
                        listOf(
                            ExcelUtils.Cell.Num(o.id.toDouble()),
                            ExcelUtils.Cell.Num(o.customerId.toDouble()),
                            ExcelUtils.Cell.Str(customerNameById[o.customerId].orEmpty()),
                            ExcelUtils.Cell.Str(DateUtils.formatNumericDate(o.orderDate)),
                            ExcelUtils.Cell.Num(o.total),
                            ExcelUtils.Cell.Str(o.notes)
                        )
                    }
                )

            // One row per line item — an order with 3 shirts + 2 trousers
            // shows up here as two rows sharing the same OrderID.
            val orderItemsSheet = "OrderItems" to (
                listOf("ID", "OrderID", "Item", "Quantity", "Rate", "Total") to
                    orderItems.map { oi ->
                        listOf(
                            ExcelUtils.Cell.Num(oi.id.toDouble()),
                            ExcelUtils.Cell.Num(oi.orderId.toDouble()),
                            ExcelUtils.Cell.Str(oi.item),
                            ExcelUtils.Cell.Num(oi.quantity),
                            ExcelUtils.Cell.Num(oi.rate),
                            ExcelUtils.Cell.Num(oi.total)
                        )
                    }
                )

            val paymentsSheet = "Payments" to (
                listOf("ID", "OrderID", "CustomerID", "CustomerName", "Amount", "PaymentDate", "Method", "Notes") to
                    payments.map { p ->
                        listOf(
                            ExcelUtils.Cell.Num(p.id.toDouble()),
                            ExcelUtils.Cell.Num(p.orderId.toDouble()),
                            ExcelUtils.Cell.Num(p.customerId.toDouble()),
                            ExcelUtils.Cell.Str(customerNameById[p.customerId].orEmpty()),
                            ExcelUtils.Cell.Num(p.amount),
                            ExcelUtils.Cell.Str(DateUtils.formatNumericDate(p.paymentDate)),
                            ExcelUtils.Cell.Str(p.paymentMethod),
                            ExcelUtils.Cell.Str(p.notes)
                        )
                    }
                )

            val file = ExcelUtils.writeToFile(context, listOf(customersSheet, ordersSheet, orderItemsSheet, paymentsSheet))
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importExcel(context: Context, database: KhataDatabase, file: File): Result<ExcelImportSummary> = withContext(Dispatchers.IO) {
        try {
            val workbook = ExcelUtils.readWorkbook(file)
            val customerDao = database.customerDao()
            val orderDao = database.orderDao()
            val paymentDao = database.paymentDao()

            val existingCustomers = customerDao.getAllCustomersSync()
            val existingCustomerIds = existingCustomers.map { it.id }.toMutableSet()
            val nameToCustomerId = existingCustomers
                .associate { it.name.trim().lowercase() to it.id }
                .toMutableMap()

            var customersAdded = 0
            var customersSkipped = 0
            workbook.sheets["Customers"]?.rows?.forEach { row ->
                val name = row["Name"]?.trim().orEmpty()
                val nameKey = name.lowercase()
                if (name.isEmpty() || nameToCustomerId.containsKey(nameKey)) {
                    customersSkipped++
                    return@forEach
                }
                val newId = customerDao.insertCustomer(
                    CustomerEntity(
                        id = 0,
                        name = name,
                        phone = row["Phone"].orEmpty(),
                        address = row["Address"].orEmpty(),
                        notes = row["Notes"].orEmpty()
                    )
                )
                existingCustomerIds.add(newId)
                nameToCustomerId[nameKey] = newId
                customersAdded++
            }

            val existingOrdersList = orderDao.getAllOrdersSync()
            val existingOrderIds = existingOrdersList.map { it.id }.toMutableSet()
            val existingOrderItemsList = database.orderItemDao().getAllOrderItemsSync()
            val existingItemsByOrderId = existingOrderItemsList.groupBy { it.orderId }
            val orderSignatureToId = existingOrdersList
                .associate { order ->
                    val signature = orderSignature(order.customerId, order.orderDate, order.total, order.notes, existingItemsByOrderId[order.id] ?: emptyList())
                    signature to order.id
                }
                .toMutableMap()
            val orderIdRemap = mutableMapOf<Long, Long>()

            // Group the OrderItems sheet rows by their sheet-local OrderID so
            // each Orders row can pull in the item lines that belong to it.
            val orderItemRowsBySheetOrderId = workbook.sheets["OrderItems"]?.rows
                ?.groupBy { it["OrderID"]?.toDoubleOrNull()?.toLong() }
                .orEmpty()

            var ordersAdded = 0
            var ordersSkipped = 0
            var orderItemsAdded = 0
            var orderItemsSkipped = 0
            workbook.sheets["Orders"]?.rows?.forEach { row ->
                val sheetId = row["ID"]?.toDoubleOrNull()?.toLong()
                val customerId = row["CustomerID"]?.toDoubleOrNull()?.toLong()
                    ?.takeIf { existingCustomerIds.contains(it) }
                    ?: row["CustomerName"]?.trim()?.lowercase()?.let { nameToCustomerId[it] }

                val itemRows = sheetId?.let { orderItemRowsBySheetOrderId[it] }.orEmpty()
                val parsedItems = itemRows.mapNotNull { itemRow ->
                    val itemName = itemRow["Item"]?.trim().orEmpty()
                    if (itemName.isBlank()) return@mapNotNull null
                    val quantity = itemRow["Quantity"]?.toDoubleOrNull() ?: 0.0
                    val rate = itemRow["Rate"]?.toDoubleOrNull() ?: 0.0
                    val itemTotal = itemRow["Total"]?.toDoubleOrNull() ?: (quantity * rate)
                    Triple(itemName, quantity, rate) to itemTotal
                }

                if (customerId == null || parsedItems.isEmpty()) {
                    ordersSkipped++
                    return@forEach
                }

                val orderDate = parseExcelDate(row["OrderDate"]) ?: System.currentTimeMillis()
                val notes = row["Notes"].orEmpty()
                val total = row["Total"]?.toDoubleOrNull() ?: parsedItems.sumOf { it.second }

                val newOrderItems = parsedItems.map { (info, itemTotal) ->
                    val (itemName, quantity, rate) = info
                    OrderItemEntity(orderId = 0, item = itemName, quantity = quantity, rate = rate, total = itemTotal)
                }

                // Same customer + date + notes + total + item lines as an existing order = this row
                // was already imported before (e.g. the same file imported twice). Link payments to
                // the existing order instead of creating a duplicate.
                val signature = orderSignature(customerId, orderDate, total, notes, newOrderItems)
                val existingMatchId = orderSignatureToId[signature]
                if (existingMatchId != null) {
                    if (sheetId != null) orderIdRemap[sheetId] = existingMatchId
                    ordersSkipped++
                    orderItemsSkipped += newOrderItems.size
                    return@forEach
                }

                val newId = orderDao.insertOrder(
                    OrderEntity(
                        id = 0,
                        customerId = customerId,
                        orderDate = orderDate,
                        total = total,
                        notes = notes
                    )
                )
                database.orderItemDao().insertItems(newOrderItems.map { it.copy(orderId = newId) })
                orderItemsAdded += newOrderItems.size
                existingOrderIds.add(newId)
                orderSignatureToId[signature] = newId
                if (sheetId != null) orderIdRemap[sheetId] = newId
                ordersAdded++
            }

            val existingPaymentsList = paymentDao.getAllPaymentsSync()
            val paymentSignatureToId = existingPaymentsList
                .associate { paymentSignature(it.orderId, it.customerId, it.amount, it.paymentDate, it.paymentMethod, it.notes) to it.id }
                .toMutableMap()
            var paymentsAdded = 0
            var paymentsSkipped = 0
            workbook.sheets["Payments"]?.rows?.forEach { row ->
                val sheetOrderId = row["OrderID"]?.toDoubleOrNull()?.toLong()
                val orderId = sheetOrderId?.let { orderIdRemap[it] ?: it.takeIf { id -> existingOrderIds.contains(id) } }
                if (orderId == null) {
                    paymentsSkipped++
                    return@forEach
                }
                val customerId = row["CustomerID"]?.toDoubleOrNull()?.toLong()
                    ?.takeIf { existingCustomerIds.contains(it) }
                    ?: row["CustomerName"]?.trim()?.lowercase()?.let { nameToCustomerId[it] }
                    ?: orderDao.getOrderByIdSync(orderId)?.customerId
                if (customerId == null) {
                    paymentsSkipped++
                    return@forEach
                }
                val amount = row["Amount"]?.toDoubleOrNull() ?: 0.0
                val paymentDate = parseExcelDate(row["PaymentDate"]) ?: System.currentTimeMillis()
                val method = row["Method"]?.ifBlank { "Cash" } ?: "Cash"
                val notes = row["Notes"].orEmpty()

                // Same order + amount + date + method as an existing payment = already imported before.
                val signature = paymentSignature(orderId, customerId, amount, paymentDate, method, notes)
                if (paymentSignatureToId.containsKey(signature)) {
                    paymentsSkipped++
                    return@forEach
                }

                val newId = paymentDao.insertPayment(
                    PaymentEntity(
                        id = 0,
                        orderId = orderId,
                        customerId = customerId,
                        amount = amount,
                        paymentDate = paymentDate,
                        paymentMethod = method,
                        notes = notes
                    )
                )
                paymentSignatureToId[signature] = newId
                paymentsAdded++
            }

            Result.success(
                ExcelImportSummary(customersAdded, customersSkipped, ordersAdded, ordersSkipped, paymentsAdded, paymentsSkipped)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun orderSignature(
        customerId: Long,
        orderDate: Long,
        total: Double,
        notes: String,
        items: List<OrderItemEntity>
    ): String {
        val itemsPart = items
            .map { listOf(it.item.trim().lowercase(), it.quantity, it.rate).joinToString(":") }
            .sorted()
            .joinToString(";")
        return listOf(customerId, orderDate, total, notes.trim().lowercase(), itemsPart).joinToString("|")
    }

    private fun paymentSignature(
        orderId: Long,
        customerId: Long,
        amount: Double,
        paymentDate: Long,
        method: String,
        notes: String
    ): String = listOf(
        orderId, customerId, amount, paymentDate, method.trim().lowercase(), notes.trim().lowercase()
    ).joinToString("|")

    private fun parseExcelDate(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(text)?.time
        } catch (e: Exception) {
            null
        }
    }
}
