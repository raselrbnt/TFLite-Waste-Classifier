package com.example.sampahclassifierapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @deprecated Arsip kode lama. Tab "Basis Data" menggunakan model in-memory [LearnEntity].
 * Entitas ini tidak lagi terdaftar pada [AppDatabase] dan dipertahankan hanya sebagai referensi historis.
 */
@Deprecated("Tab Basis Data menggunakan in-memory LearnEntity. Entitas ini tidak lagi aktif di AppDatabase.")
@Entity(tableName = "learn_database")
data class LearnDatabaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val iconResId: Int = 0,
    val category: String = "", // ORGANIK, ANORGANIK, TIDAK TERDETEKSI
    val timestamp: Long = System.currentTimeMillis()
)
