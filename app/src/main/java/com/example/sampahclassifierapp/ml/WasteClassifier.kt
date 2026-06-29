package com.example.sampahclassifierapp.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Implementasi WasteClassifier menggunakan TensorFlow Lite.
 * Dioptimalkan dengan Memory Mapping (mmap) dan Singleton Interpreter Caching.
 * Input: 224x224, RGB, Float32.
 */
class WasteClassifier(context: Context) {

    private val labels: List<String>
    private val inputSize = 224

    data class Recognition(
        val category: String,
        val confidence: Float
    )

    init {
        try {
            labels = loadLabels(context, "labels.txt")
            initializeInterpreter(context)
        } catch (e: Exception) {
            throw RuntimeException("Gagal memuat model ML: ${e.message}", e)
        }
    }

    private fun loadLabels(context: Context, filename: String): List<String> {
        return try {
            context.assets.open(filename).use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    reader.readLines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            listOf("ORGANIK", "ANORGANIK")
        }
    }

    fun classify(bitmap: Bitmap): Recognition {
        try {
            val interp = getInterpreter() ?: throw RuntimeException("Interpreter tidak terinisialisasi")
            
            // Resize bitmap ke ukuran input model (224x224)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            
            // Konversi piksel bitmap ke direct ByteBuffer Float32
            // 4 bytes per float (Float32) * 224 * 224 * 3 channels
            val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
                order(ByteOrder.nativeOrder())
            }
            
            val pixels = IntArray(inputSize * inputSize)
            resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            
            // Recycle resizedBitmap secara langsung jika merupakan instansi baru (hemat memori)
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
            byteBuffer.rewind()

            // Siapkan output buffer (1 x jumlah label)
            val outputBuffer = Array(1) { FloatArray(labels.size) }

            // Jalankan inferensi secara thread-safe
            synchronized(interp) {
                interp.run(byteBuffer, outputBuffer)
            }

            // Temukan skor tertinggi
            val results = outputBuffer[0]
            var maxIdx = -1
            var maxScore = -1f
            
            for (i in results.indices) {
                if (results[i] > maxScore) {
                    maxScore = results[i]
                    maxIdx = i
                }
            }

            // Normalisasi confidence score
            val normalizedConfidence = kotlin.math.min(kotlin.math.max(maxScore, 0f), 1.0f)

            return if (maxIdx != -1 && maxIdx < labels.size) {
                Recognition(
                    category = labels[maxIdx].uppercase(),
                    confidence = normalizedConfidence
                )
            } else {
                Recognition(
                    category = "TIDAK_TERDETEKSI",
                    confidence = 0f
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Recognition(
                category = "TIDAK_TERDETEKSI",
                confidence = 0f
            )
        }
    }

    fun close() {
        // Jangan menutup interpreter secara langsung di sini agar tetap hangat untuk pemindaian berikutnya.
        // Interpreter akan dibersihkan secara otomatis oleh sistem operasi saat aplikasi dihentikan.
    }

    companion object {
        private var sharedInterpreter: Interpreter? = null

        /**
         * Memeriksa ketersediaan dan kesiapan model tanpa alokasi objek baru yang mahal.
         */
        fun isModelAvailable(context: Context): Boolean {
            return try {
                initializeInterpreter(context)
                sharedInterpreter != null
            } catch (e: Exception) {
                false
            }
        }

        @Synchronized
        private fun initializeInterpreter(context: Context) {
            if (sharedInterpreter == null) {
                // Memuat file TFLite menggunakan Memory Mapping (mmap)
                val fileDescriptor = context.assets.openFd("model_sampah.tflite")
                val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val buffer = fileChannel.map(
                    java.nio.channels.FileChannel.MapMode.READ_ONLY, 
                    startOffset, 
                    declaredLength
                )
                sharedInterpreter = Interpreter(buffer)
            }
        }

        fun getInterpreter(): Interpreter? {
            return sharedInterpreter
        }
    }
}
