package com.vx7.khatapro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vx7.khatapro.data.database.dao.*
import com.vx7.khatapro.data.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        CompanyProfileEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentEntity::class,
        SettingsEntity::class,
        LicenseEntity::class,
        BackupLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KhataDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun settingsDao(): SettingsDao
    abstract fun licenseDao(): LicenseDao
    abstract fun backupLogDao(): BackupLogDao

    companion object {
        @Volatile
        private var INSTANCE: KhataDatabase? = null

        fun getDatabase(context: Context): KhataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KhataDatabase::class.java,
                    "khata_pro.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
