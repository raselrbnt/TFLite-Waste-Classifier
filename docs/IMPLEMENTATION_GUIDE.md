# Panduan Implementasi Teknis (Step-by-Step)

Dokumen ini berisi petunjuk terperinci untuk membangun aplikasi Android EcoTech Waste Classifier di Android Studio.

---

## Langkah 1: Pengaturan Gradle (`app/build.gradle.kts`)
Tambahkan baris dependensi berikut ke dalam file gradle modul aplikasi kamu, lalu tekan tombol **Sync Now** di sudut kanan atas:

```kotlin
dependencies {
    // UI Dasar & Layout
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Navigasi & Fragment
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    
    // Kamera (CameraX SDK)
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    // Room Database (Penyimpanan Lokal)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    
    // TensorFlow Lite (Offline Inference)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}
```

---

## Langkah 2: Membuat Komponen Gambar Kustom (Drawables)
Buat file-file XML berikut di dalam folder `app/src/main/res/drawable/` untuk meniru visual mock-up:

### 1. `bg_card_minimal.xml` (Garis tepi tipis 1px)
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFFFFF" />
    <stroke android:width="1dp" android:color="#F3F4F6" />
    <corners android:radius="4dp" />
</shape>
```

### 2. `bg_card_total_scan.xml` (Garis bawah hijau neon)
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape>
            <solid android:color="#F3F4F6" />
            <corners android:radius="4dp" />
        </shape>
    </item>
    <item android:top="76dp">
        <shape>
            <solid android:color="#00FF41" />
            <corners android:bottomLeftRadius="4dp" android:bottomRightRadius="4dp" />
        </shape>
    </item>
</layer-list>
```

### 3. `bg_card_hazardous.xml` (Garis bawah merah peringatan)
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape>
            <solid android:color="#F3F4F6" />
            <corners android:radius="4dp" />
        </shape>
    </item>
    <item android:top="76dp">
        <shape>
            <solid android:color="#FF3366" />
            <corners android:bottomLeftRadius="4dp" android:bottomRightRadius="4dp" />
        </shape>
    </item>
</layer-list>
```

### 4. `viewfinder_reticle.xml` (Batas bidik kamera)
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="250dp"
    android:height="250dp"
    android:viewportWidth="250"
    android:viewportHeight="250">
    <path android:pathData="M 10 30 L 10 10 L 30 10" android:strokeColor="#00FF41" android:strokeWidth="4" />
    <path android:pathData="M 220 10 L 240 10 L 240 30" android:strokeColor="#00FF41" android:strokeWidth="4" />
    <path android:pathData="M 10 220 L 10 240 L 30 240" android:strokeColor="#00FF41" android:strokeWidth="4" />
    <path android:pathData="M 220 240 L 240 240 L 240 220" android:strokeColor="#00FF41" android:strokeWidth="4" />
</vector>
```

---

## Langkah 3: Layout Utama Aplikasi

### `activity_main.xml` (Root dengan BottomNavigationView)
Salin kode berikut ke `app/src/main/res/layout/activity_main.xml`. Pastikan package `BottomNavigationView` sudah tepat:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph"
        app:layout_constraintBottom_toTopOf="@id/bottom_navigation"
        app:layout_constraintTop_toTopOf="parent" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_navigation"
        android:layout_width="match_parent"
        android:layout_height="64dp"
        android:background="#FFFFFF"
        app:menu="@menu/bottom_nav_menu"
        app:itemIconTint="@color/brand_primary"
        app:itemTextColor="@color/brand_dark_charcoal"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## Langkah 4: Logika Database Lokal (Room)
Buat file Kotlin berikut di dalam package data kamu (`com.ecotech.wasteclassifier.data`):

### 1. Entity: `ScanEntity.kt`
```kotlin
package com.ecotech.wasteclassifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val confidence: Double,
    val imagePath: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 2. DAO: `ScanDao.kt`
```kotlin
package com.ecotech.wasteclassifier.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Insert
    suspend fun insertScan(scan: ScanEntity)

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE category = 'ORGANIK'")
    fun getOrganikCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE category = 'ANORGANIK'")
    fun getAnorganikCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE category = 'BERBAHAYA'")
    fun getBerbahayaCount(): Flow<Int>
}
```

---

## Langkah 5: Kelas Pembantu Machine Learning (TFLite)
Buat file Kotlin ini untuk melakukan inferensi offline dari model `.tflite` yang kamu letakkan di folder `app/src/main/assets/waste_classifier_model.tflite`.

### `WasteClassifier.kt`
```kotlin
package com.ecotech.wasteclassifier.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WasteClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = listOf("ORGANIK", "ANORGANIK", "BERBAHAYA")

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "waste_classifier_model.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(modelBuffer, options)
    }

    fun classifyImage(bitmap: Bitmap): ClassificationResult {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.Method.BILINEAR))
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = ByteBuffer.allocateDirect(1 * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter?.run(tensorImage.buffer, outputBuffer)
        outputBuffer.rewind()
        
        val scores = FloatArray(3)
        outputBuffer.asFloatBuffer().get(scores)

        var maxIdx = 0
        var maxScore = 0.0f
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxIdx = i
            }
        }

        return ClassificationResult(
            category = labels[maxIdx],
            confidence = maxScore * 100.0
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    data class ClassificationResult(val category: String, val confidence: Double)
}
```
