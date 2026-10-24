package com.example.sampahclassifierapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE category = 'ORGANIK'")
    fun getOrganikCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE category = 'ANORGANIK'")
    fun getAnorganikCount(): Flow<Int>
}
