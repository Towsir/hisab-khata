package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.DueEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DueEntryDao {
    @Query("SELECT * FROM due_entries WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getEntriesForCustomer(customerId: Long): Flow<List<DueEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDueEntry(entry: DueEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DueEntryEntity>)

    @Delete
    suspend fun deleteDueEntry(entry: DueEntryEntity)

    @Query("DELETE FROM due_entries WHERE customerId = :customerId")
    suspend fun deleteEntriesForCustomer(customerId: Long)
}
