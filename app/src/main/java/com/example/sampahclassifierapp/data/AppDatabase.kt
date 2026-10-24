package com.example.sampahclassifierapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database utama aplikasi.
 * Hanya memuat satu entitas aktif: ScanEntity (tabel scan_history).
 * Entitas LearnDatabaseEntity dihapus karena tab "Basis Data" menggunakan
 * data statis in-memory (LearnEntity) dan tidak membutuhkan persistensi SQLite.
 */
@Database(
    entities = [ScanEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ecotech_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
