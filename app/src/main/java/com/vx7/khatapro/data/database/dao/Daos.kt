package com.vx7.khatapro.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vx7.khatapro.data.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserSync(): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("UPDATE users SET password_hash = :hash, updated_at = :updatedAt WHERE id = :id")
    suspend fun updatePassword(id: Long, hash: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile LIMIT 1")
    fun getProfileFlow(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profile LIMIT 1")
    suspend fun getProfileSync(): CompanyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CompanyProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: CompanyProfileEntity)

    @Query("DELETE FROM company_profile")
    suspend fun clearProfile()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdSync(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersSync(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomersFlow(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomerCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY order_date DESC, id DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customer_id = :customerId ORDER BY order_date DESC, id DESC")
    fun getOrdersByCustomerFlow(customerId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    fun getOrderByIdFlow(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderByIdSync(id: Long): OrderEntity?

    @Query("SELECT * FROM orders ORDER BY order_date DESC, id DESC")
    suspend fun getAllOrdersSync(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE order_date BETWEEN :startMs AND :endMs ORDER BY order_date DESC")
    fun getOrdersInDateRangeFlow(startMs: Long, endMs: Long): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders")
    fun getOrderCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("DELETE FROM orders")
    suspend fun clearOrders()
}

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id ASC")
    fun getItemsForOrderFlow(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id ASC")
    suspend fun getItemsForOrderSync(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items ORDER BY order_id ASC, id ASC")
    fun getAllOrderItemsFlow(): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items ORDER BY order_id ASC, id ASC")
    suspend fun getAllOrderItemsSync(): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: OrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>): List<Long>

    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    suspend fun deleteItemsForOrder(orderId: Long)

    @Query("DELETE FROM order_items")
    suspend fun clearOrderItems()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY payment_date DESC, id DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE order_id = :orderId ORDER BY payment_date DESC")
    fun getPaymentsForOrderFlow(orderId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE order_id = :orderId ORDER BY payment_date DESC")
    suspend fun getPaymentsForOrderSync(orderId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE customer_id = :customerId ORDER BY payment_date DESC")
    fun getPaymentsForCustomerFlow(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentByIdSync(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments ORDER BY payment_date DESC, id DESC")
    suspend fun getAllPaymentsSync(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE payment_date BETWEEN :startMs AND :endMs ORDER BY payment_date DESC")
    fun getPaymentsInDateRangeFlow(startMs: Long, endMs: Long): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(amount) FROM payments WHERE order_id = :orderId")
    suspend fun getTotalPaidForOrder(orderId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payments")
    suspend fun clearPayments()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity): Long

    @Update
    suspend fun updateSettings(settings: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun clearSettings()
}

@Dao
interface LicenseDao {
    @Query("SELECT * FROM licenses LIMIT 1")
    fun getLicenseFlow(): Flow<LicenseEntity?>

    @Query("SELECT * FROM licenses LIMIT 1")
    suspend fun getLicenseSync(): LicenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicense(license: LicenseEntity): Long

    @Update
    suspend fun updateLicense(license: LicenseEntity)

    @Query("DELETE FROM licenses")
    suspend fun clearLicense()
}

@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backups ORDER BY backup_date DESC")
    fun getAllBackupsFlow(): Flow<List<BackupLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupLog(log: BackupLogEntity): Long

    @Query("DELETE FROM backups WHERE id = :id")
    suspend fun deleteBackupLog(id: Long)

    @Query("DELETE FROM backups")
    suspend fun clearBackups()
}
