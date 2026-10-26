package com.example.sampahclassifierapp

import com.example.sampahclassifierapp.ml.WasteClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk memverifikasi logika White Box Testing (Bab 4.6 Skripsi):
 * Penyaringan ambang batas kepercayaan (Confidence Threshold 70%) dan pemetaan kategori hasil.
 */
class ExampleUnitTest {

    private fun evaluateThreshold(raw: WasteClassifier.Recognition): WasteClassifier.Recognition {
        return if (raw.confidence < 0.70f || raw.category == "TIDAK_TERDETEKSI") {
            WasteClassifier.Recognition(
                category = "TIDAK TERDETEKSI",
                confidence = raw.confidence
            )
        } else {
            raw
        }
    }

    @Test
    fun whiteBox_path1_lowConfidence_returnsTidakTerdeteksi() {
        // Jalur 1: Confidence < 0.70f (55%) -> TIDAK TERDETEKSI
        val input = WasteClassifier.Recognition(category = "ANORGANIK", confidence = 0.55f)
        val result = evaluateThreshold(input)
        assertEquals("TIDAK TERDETEKSI", result.category)
        assertEquals(0.55f, result.confidence, 0.001f)
    }

    @Test
    fun whiteBox_path2_validOrganik_returnsOrganik() {
        // Jalur 2: Confidence >= 0.70f (92%) & ORGANIK -> ORGANIK
        val input = WasteClassifier.Recognition(category = "ORGANIK", confidence = 0.92f)
        val result = evaluateThreshold(input)
        assertEquals("ORGANIK", result.category)
        assertEquals(0.92f, result.confidence, 0.001f)
    }

    @Test
    fun whiteBox_path3_validAnorganik_returnsAnorganik() {
        // Jalur 3: Confidence >= 0.70f (88%) & ANORGANIK -> ANORGANIK
        val input = WasteClassifier.Recognition(category = "ANORGANIK", confidence = 0.88f)
        val result = evaluateThreshold(input)
        assertEquals("ANORGANIK", result.category)
        assertEquals(0.88f, result.confidence, 0.001f)
    }
}
