package com.example.sampahclassifierapp.data

/**
 * Data model untuk konten edukasi di Tab Basis Data (Pelajari)
 */
data class LearnEntity(
    val title: String,
    val description: String,
    val iconResId: Int,
    var isExpanded: Boolean = false
)
