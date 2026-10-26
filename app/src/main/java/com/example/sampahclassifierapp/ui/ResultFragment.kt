package com.example.sampahclassifierapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.ScanEntity
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentResultBinding
import com.example.sampahclassifierapp.ml.WasteClassifier
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Menampilkan hasil klasifikasi sampah.
 *
 * Mendukung dua mode:
 *  - Mode Pindai Baru (isReadOnly = false): Menerima path foto sementara (TEMP_*.jpg di cacheDir),
 *    menjalankan inferensi ML di thread latar belakang, lalu memindahkan foto ke filesDir secara
 *    permanen saat Simpan ditekan.
 *
 *  - Mode Pratinjau Riwayat (isReadOnly = true): Menerima path foto permanen beserta data
 *    kategori & confidence yang sudah tersimpan di Room DB. Inferensi ML dilewati,
 *    tombol Simpan disembunyikan untuk mencegah duplikasi data.
 */
class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ScanRepository
    private var classifier: WasteClassifier? = null

    // Path foto sementara (cacheDir) yang akan dipindahkan ke filesDir saat disimpan
    private var tempImagePath: String? = null

    // Path foto permanen (filesDir) yang tersimpan di Room DB
    private var permanentImagePath: String? = null

    // Hasil inferensi ML, digunakan saat menyimpan ke database
    private var currentResult: WasteClassifier.Recognition? = null
    private var isInstructionsExpanded = true

    // Mode baca saja: true = dibuka dari riwayat, false = pemindaian baru
    private var isReadOnly = false

    // Penanda apakah foto sementara sudah disimpan secara permanen
    private var isSaved = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)

        val database = AppDatabase.getDatabase(requireContext())
        repository = ScanRepository(database.scanDao())

        // Baca argumen navigasi
        isReadOnly = arguments?.getBoolean("isReadOnly", false) ?: false
        tempImagePath = arguments?.getString("imagePath")

        // Tangani tombol Back sistem / gesture agar file sementara tetap dibersihkan
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            }
        )

        if (isReadOnly) {
            setupReadOnlyMode()
        } else {
            setupScanMode()
        }

        binding.btnBack.setOnClickListener {
            handleBack()
        }

        binding.btnPindaiLainnya.setOnClickListener {
            handleBack()
        }

        // Toggle Expandable Accordion Dropdown Instruksi Penanganan
        binding.cardInstructions.setOnClickListener {
            val currentBinding = _binding ?: return@setOnClickListener
            isInstructionsExpanded = !isInstructionsExpanded
            currentBinding.layoutBullets.visibility = if (isInstructionsExpanded) View.VISIBLE else View.GONE
            currentBinding.imgInstructionsChevron.animate()
                .rotation(if (isInstructionsExpanded) 0f else 180f)
                .setDuration(250)
                .start()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE PRATINJAU RIWAYAT (Read-Only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Menampilkan data hasil klasifikasi yang sudah tersimpan di Room DB
     * tanpa menjalankan inferensi ML ulang.
     * Tombol "Simpan Catatan" disembunyikan untuk mencegah duplikasi data.
     */
    private fun setupReadOnlyMode() {
        val currentBinding = _binding ?: return

        // Sembunyikan tombol Simpan agar pengguna tidak menyimpan ulang data yang sama
        currentBinding.btnSimpanCatatan.visibility = View.GONE

        // Ganti teks label chip dari "OFFLINE ML" ke "RIWAYAT" sebagai penanda visual
        currentBinding.tvChipOfflineMode.text = "📋 RIWAYAT"

        val savedCategory = arguments?.getString("savedCategory") ?: "TIDAK TERDETEKSI"
        val savedConfidence = arguments?.getFloat("savedConfidence") ?: 0f

        // Tampilkan foto dari path permanen (filesDir) secara asinkron
        tempImagePath?.let { path ->
            permanentImagePath = path
            loadImageAsync(File(path))
        }

        // Tampilkan nilai dari database, tanpa chip latensi inferensi
        currentBinding.tvChipInferenceTime.visibility = View.GONE

        val confidencePct = savedConfidence.roundToInt().coerceIn(0, 100)
        currentBinding.progressConfidence.progress = confidencePct
        currentBinding.tvClassificationResult.text = savedCategory
        currentBinding.tvLblConfidence.text = getString(R.string.confidence_label, confidencePct)

        // Warna teks dinamis sesuai kategori
        val colorRes = when {
            savedCategory.contains("ORGANIK") -> R.color.brand_primary
            savedCategory.contains("ANORGANIK") -> R.color.brand_primary
            else -> R.color.brand_warning_red
        }
        currentBinding.tvClassificationResult.setTextColor(
            ContextCompat.getColor(requireContext(), colorRes)
        )

        updateInstructions(savedCategory)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE PEMINDAIAN BARU
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Menjalankan inferensi ML pada foto baru dari kamera/galeri.
     * Saat pengguna menekan Simpan, foto sementara dipindahkan ke filesDir (permanen)
     * sebelum disimpan ke Room DB.
     */
    private fun setupScanMode() {
        val currentBinding = _binding ?: return
        currentBinding.btnSimpanCatatan.visibility = View.VISIBLE
        currentBinding.btnSimpanCatatan.isEnabled = false

        currentBinding.btnSimpanCatatan.setOnClickListener {
            saveToDatabaseWithPermanentFile()
        }

        displayResultAsync()
    }

    private fun displayResultAsync() {
        val path = tempImagePath ?: return
        val appContext = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch {
            val file = File(path)
            val exists = withContext(Dispatchers.IO) { file.exists() }
            if (!exists) {
                if (isAdded && _binding != null) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, getString(R.string.error_file_not_found), Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            }

            // Decode bitmap + koreksi orientasi EXIF di background IO thread
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmapFromFile(file, 1024, 1024)
            }

            val currentBinding = _binding
            if (currentBinding == null || !isAdded) return@launch

            if (bitmap == null) {
                context?.let { ctx ->
                    Toast.makeText(ctx, getString(R.string.error_image_not_loaded), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            currentBinding.imgCaptured.setImageBitmap(bitmap)

            try {
                // Jalankan inferensi TFLite di Default background dispatcher agar transisi UI mulus
                val (result, inferenceMs) = withContext(Dispatchers.Default) {
                    val mlClassifier = classifier ?: WasteClassifier(appContext).also {
                        classifier = it
                    }
                    val startTime = System.currentTimeMillis()
                    val rec = mlClassifier.classify(bitmap)
                    val elapsed = System.currentTimeMillis() - startTime
                    rec to elapsed
                }

                val b = _binding
                if (b == null || !isAdded) return@launch

                b.tvChipInferenceTime.visibility = View.VISIBLE
                b.tvChipInferenceTime.text = "⚡ $inferenceMs ms"

                // Terapkan ambang batas kepercayaan (confidence threshold) sebesar 70%
                val finalResult = if (result.confidence < 0.70f || result.category == "TIDAK_TERDETEKSI") {
                    WasteClassifier.Recognition(
                        category = "TIDAK TERDETEKSI",
                        confidence = result.confidence
                    )
                } else {
                    result
                }

                currentResult = finalResult
                val confidencePct = (finalResult.confidence * 100f).roundToInt().coerceIn(0, 100)
                b.progressConfidence.progress = confidencePct
                b.tvClassificationResult.text = finalResult.category
                b.tvLblConfidence.text = getString(R.string.confidence_label, confidencePct)

                // Sesuaikan warna teks hasil secara dinamis
                val colorRes = when {
                    finalResult.category.contains("ORGANIK") -> R.color.brand_primary
                    finalResult.category.contains("ANORGANIK") -> R.color.brand_primary
                    else -> R.color.brand_warning_red
                }
                val colorValue = ContextCompat.getColor(requireContext(), colorRes)
                b.tvClassificationResult.setTextColor(colorValue)

                updateInstructions(finalResult.category)
                b.btnSimpanCatatan.isEnabled = true
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    context?.let { ctx ->
                        Toast.makeText(
                            ctx,
                            getString(R.string.error_classification, e.message ?: "Unknown"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIKA PENYIMPANAN & NAVIGASI
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Memindahkan foto sementara (TEMP_* di cacheDir) ke direktori penyimpanan
     * permanen (filesDir/scan_images/) sebelum menyimpan entitas ke Room DB.
     *
     * Pemindahan ke filesDir menjamin foto riwayat tidak hilang saat OS Android
     * membersihkan cache aplikasi secara otomatis.
     */
    private fun saveToDatabaseWithPermanentFile() {
        val result = currentResult
        val tempPath = tempImagePath

        if (result == null || tempPath == null || isSaved) return

        _binding?.btnSimpanCatatan?.isEnabled = false

        val appContext = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Pindahkan file dari cacheDir ke filesDir di latar belakang (IO thread)
                val permanentPath = withContext(Dispatchers.IO) {
                    moveToPermanentStorage(appContext, File(tempPath))
                }

                if (permanentPath == null) {
                    if (isAdded) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Gagal menyimpan foto secara permanen.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    _binding?.btnSimpanCatatan?.isEnabled = true
                    return@launch
                }

                isSaved = true
                permanentImagePath = permanentPath

                val entity = ScanEntity(
                    category = result.category,
                    confidence = (result.confidence * 100.0),
                    imagePath = permanentPath
                )
                repository.insert(entity)

                if (!isAdded || _binding == null) return@launch

                context?.let { ctx ->
                    Toast.makeText(ctx, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                }

                // Bersihkan ResultFragment dari backstack dan pindahkan ke tab "Catatan"
                try {
                    val navController = findNavController()
                    navController.popBackStack(R.id.navigation_result, true)
                } catch (e: Exception) {
                    // Abaikan jika sudah di-pop
                }

                val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.navigation_history

            } catch (e: Exception) {
                if (isAdded) {
                    context?.let { ctx ->
                        Toast.makeText(
                            ctx,
                            getString(R.string.error_generic, e.message ?: "Unknown"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                _binding?.btnSimpanCatatan?.isEnabled = true
            }
        }
    }

    /**
     * Memindahkan file sementara dari cacheDir ke direktori permanen [filesDir]/scan_images/.
     * Mengembalikan path permanen jika berhasil, null jika gagal.
     */
    private fun moveToPermanentStorage(appContext: android.content.Context, tempFile: File): String? {
        return try {
            if (!tempFile.exists()) return null
            val scanImagesDir = File(appContext.filesDir, "scan_images")
            if (!scanImagesDir.exists()) {
                scanImagesDir.mkdirs()
            }
            // Ganti awalan TEMP_ dengan IMG_ untuk menandai file sudah permanen
            val permanentName = tempFile.name.replaceFirst("TEMP_", "IMG_")
            val permanentFile = File(scanImagesDir, permanentName)
            tempFile.copyTo(permanentFile, overwrite = true)
            tempFile.delete() // Bersihkan file sementara dari cache
            permanentFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Menangani tombol Kembali (baik tombol UI maupun sistem back).
     * Jika mode pindai baru dan belum disimpan, file sementara di cacheDir dibersihkan
     * agar tidak menumpuk di penyimpanan perangkat.
     */
    private fun handleBack() {
        cleanupUnsavedTempFile()
        if (!isAdded) return
        try {
            findNavController().navigateUp()
        } catch (e: Exception) {
            // Abaikan jika transisi navigasi sedang berlangsung
        }
    }

    private fun cleanupUnsavedTempFile() {
        if (!isReadOnly && !isSaved) {
            tempImagePath?.let { path ->
                try {
                    val tempFile = File(path)
                    if (tempFile.exists() && tempFile.name.startsWith("TEMP_")) {
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    // Abaikan
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Memuat dan menampilkan gambar secara asinkron dengan downsampling cerdas (smart inSampleSize)
     * serta koreksi orientasi EXIF untuk mencegah OutOfMemoryError dan gambar miring.
     */
    private fun loadImageAsync(file: File) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                if (file.exists()) decodeSampledBitmapFromFile(file, 1024, 1024) else null
            }
            val currentBinding = _binding
            if (currentBinding != null && isAdded && bitmap != null) {
                currentBinding.imgCaptured.setImageBitmap(bitmap)
            }
        }
    }

    private fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var inSampleSize = 1
            val (height: Int, width: Int) = options.outHeight to options.outWidth
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
            applyExifOrientation(rawBitmap, file.absolutePath)
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Membaca metadata EXIF dari file gambar dan memutar Bitmap jika diperlukan
     * agar foto dari kamera atau galeri tidak tampil miring (rotasi 90/180/270 derajat).
     */
    private fun applyExifOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun updateInstructions(category: String) {
        val currentBinding = _binding ?: return
        when {
            category.contains("ANORGANIK") -> {
                currentBinding.tvBullet1.text = getString(R.string.anorganik_bullet1)
                currentBinding.tvBullet2.text = getString(R.string.anorganik_bullet2)
                currentBinding.tvBullet3.text = getString(R.string.anorganik_bullet3)
            }
            category.contains("ORGANIK") -> {
                currentBinding.tvBullet1.text = getString(R.string.organik_bullet1)
                currentBinding.tvBullet2.text = getString(R.string.organik_bullet2)
                currentBinding.tvBullet3.text = getString(R.string.organik_bullet3)
            }
            else -> {
                currentBinding.tvBullet1.text = "Foto kurang jelas atau objek tidak dikenali."
                currentBinding.tvBullet2.text = "Pastikan cahaya cukup dan objek berada di tengah kamera."
                currentBinding.tvBullet3.text = "Coba pindai ulang dengan sudut pengambilan yang berbeda."
            }
        }
    }

    override fun onDestroyView() {
        _binding?.imgInstructionsChevron?.animate()?.cancel()
        classifier?.close()
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Jika pengguna meninggalkan ResultFragment melalui tab bawah tanpa menyimpan
        // (bukan karena rotasi layar), bersihkan file sementara di cacheDir.
        if (activity?.isChangingConfigurations != true) {
            cleanupUnsavedTempFile()
        }
    }
}
