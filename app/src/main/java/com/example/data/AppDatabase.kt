package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CustomerDao
import com.example.data.dao.DueEntryDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DueEntryEntity
import com.example.data.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CustomerEntity::class,
        DueEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun customerDao(): CustomerDao
    abstract fun dueEntryDao(): DueEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hishab_khata_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
