package com.example.sampahclassifierapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity untuk konten edukasi pemilahan sampah
 * Digunakan untuk menyimpan data di Tab "Basis Data" dalam aplikasi
 */
@Entity(tableName = "learn_database")
data class LearnDatabaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val iconResId: Int = 0,
    val category: String = "", // ORGANIK, ANORGANIK, TIDAK TERDETEKSI
    val timestamp: Long = System.currentTimeMillis()
)
