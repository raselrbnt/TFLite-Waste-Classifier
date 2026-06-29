package com.example.sampahclassifierapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,       // ORGANIK, ANORGANIK, TIDAK TERDETEKSI
    val confidence: Double,     // Persentase (misal: 98.4)
    val imagePath: String,      // URI internal storage untuk menampilkan foto
    val timestamp: Long = System.currentTimeMillis()
)
