package com.vx7.khatapro.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "company_name")
    val companyName: String = "",
    @ColumnInfo(name = "owner_name")
    val ownerName: String = "",
    val mobile: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    @ColumnInfo(name = "gst_number")
    val gstNumber: String? = null,
    @ColumnInfo(name = "logo_path")
    val logoPath: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["phone"])]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customer_id"]), Index(value = ["order_date"])]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "order_date")
    val orderDate: Long = System.currentTimeMillis(),
    val total: Double = 0.0, // Calculated: sum of all order_items' (quantity * rate)
    val notes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

// A single line item (item name + qty + rate) that belongs to one order.
// One order can now hold multiple of these, so a customer taking several
// different items in a single visit is recorded as one order with several
// OrderItemEntity rows instead of forcing one order per item.
@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["order_id"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Long,
    val item: String,
    val quantity: Double,
    val rate: Double,
    val total: Double, // Calculated: quantity * rate
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["order_id"]), Index(value = ["customer_id"]), Index(value = ["payment_date"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    val amount: Double,
    @ColumnInfo(name = "payment_date")
    val paymentDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "Cash", // Cash, UPI, Bank Transfer, Cheque, Other
    val notes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val theme: String = "system", // light, dark, system
    val currency: String = "₹",
    @ColumnInfo(name = "date_format")
    val dateFormat: String = "dd/MM/yyyy",
    @ColumnInfo(name = "remember_login")
    val rememberLogin: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "licenses")
data class LicenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "license_key")
    val licenseKey: String,
    val status: String = "Active", // Active, Trial, Expired, Suspended
    @ColumnInfo(name = "license_type")
    val licenseType: String = "Trial", // Trial, Monthly, Yearly, Lifetime
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "activated_at")
    val activatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "backups")
data class BackupLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "backup_name")
    val backupName: String,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "backup_date")
    val backupDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long = 0L
)
