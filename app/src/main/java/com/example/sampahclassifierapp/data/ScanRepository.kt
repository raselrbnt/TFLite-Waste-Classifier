package com.example.sampahclassifierapp.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {

    val allScans: Flow<List<ScanEntity>> = scanDao.getAllScans()
    val totalCount: Flow<Int> = scanDao.getTotalCount()
    val organikCount: Flow<Int> = scanDao.getOrganikCount()
    val anorganikCount: Flow<Int> = scanDao.getAnorganikCount()

    suspend fun insert(scan: ScanEntity) {
        scanDao.insertScan(scan)
    }

    suspend fun delete(scan: ScanEntity) {
        scanDao.deleteScan(scan)
    }
}
