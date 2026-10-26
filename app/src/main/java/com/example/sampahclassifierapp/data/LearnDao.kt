package com.example.sampahclassifierapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * @deprecated Arsip kode lama. Tab "Basis Data" menggunakan model in-memory [LearnEntity].
 * DAO ini tidak lagi terdaftar pada [AppDatabase] dan dipertahankan hanya sebagai referensi historis.
 */
@Deprecated("Tab Basis Data menggunakan in-memory LearnEntity. DAO ini tidak lagi aktif di AppDatabase.")
@Suppress("DEPRECATION")
@Dao
interface LearnDao {
    @Query("SELECT * FROM learn_database ORDER BY id ASC")
    fun getAllLearnItems(): Flow<List<LearnDatabaseEntity>>

    @Insert
    suspend fun insertLearnItem(item: LearnDatabaseEntity)

    @Query("SELECT COUNT(*) FROM learn_database")
    fun getLearnItemCount(): Flow<Int>

    @Query("DELETE FROM learn_database")
    suspend fun clearAll()
}
