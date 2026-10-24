package com.example.sampahclassifierapp.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Menampilkan hasil klasifikasi sampah.
 *
 * Mendukung dua mode:
 *  - Mode Pindai Baru (isReadOnly = false): Menerima path foto sementara (TEMP_*.jpg di cacheDir),
 *    menjalankan inferensi ML, lalu memindahkan foto ke filesDir secara permanen saat Simpan ditekan.
 *
 *  - Mode Pratinjau Riwayat (isReadOnly = true): Menerima path foto permanen beserta data
 *    kategori & confidence yang sudah tersimpan di Room DB. Inferensi ML dilewati,
 *    tombol Simpan disembunyikan untuk mencegah duplikasi data.
 */
class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ScanRepository
    private lateinit var classifier: WasteClassifier

    // Path foto sementara (cacheDir) yang akan dipindahkan ke filesDir saat disimpan
    private var tempImagePath: String? = null

    // Path foto permanen (filesDir) yang tersimpan di Room DB
    private var permanentImagePath: String? = null

    // Hasil inferensi ML, digunakan saat menyimpan ke database
    private var currentResult: WasteClassifier.Recognition? = null
    private var isInstructionsExpanded = true

    // Mode baca saja: true = dibuka dari riwayat, false = pemindaian baru
    private var isReadOnly = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)

        val database = AppDatabase.getDatabase(requireContext())
        repository = ScanRepository(database.scanDao())
        classifier = WasteClassifier(requireContext())

        // Baca argumen navigasi
        isReadOnly = arguments?.getBoolean("isReadOnly", false) ?: false
        tempImagePath = arguments?.getString("imagePath")

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
            isInstructionsExpanded = !isInstructionsExpanded
            binding.layoutBullets.visibility = if (isInstructionsExpanded) View.VISIBLE else View.GONE
            binding.imgInstructionsChevron.animate()
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
        // Sembunyikan tombol Simpan agar pengguna tidak menyimpan ulang data yang sama
        binding.btnSimpanCatatan.visibility = View.GONE

        // Ganti teks label chip dari "OFFLINE ML" ke "RIWAYAT" sebagai penanda visual
        binding.tvChipOfflineMode.text = "📋 RIWAYAT"

        val savedCategory = arguments?.getString("savedCategory") ?: "TIDAK TERDETEKSI"
        val savedConfidence = arguments?.getFloat("savedConfidence") ?: 0f

        // Tampilkan foto dari path permanen (filesDir)
        tempImagePath?.let { path ->
            permanentImagePath = path
            loadImage(File(path))
        }

        // Tampilkan nilai dari database, tanpa chip latensi inferensi
        binding.tvChipInferenceTime.visibility = View.GONE

        val confidencePct = savedConfidence.toInt()
        binding.progressConfidence.progress = confidencePct
        binding.tvClassificationResult.text = savedCategory
        binding.tvLblConfidence.text = getString(R.string.confidence_label, confidencePct)

        // Warna teks dinamis sesuai kategori
        val colorRes = when {
            savedCategory.contains("ORGANIK") -> R.color.brand_primary
            savedCategory.contains("ANORGANIK") -> R.color.brand_primary
            else -> R.color.brand_warning_red
        }
        binding.tvClassificationResult.setTextColor(
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
        binding.btnSimpanCatatan.visibility = View.VISIBLE

        binding.btnSimpanCatatan.setOnClickListener {
            saveToDatabaseWithPermanentFile()
        }

        displayResult()
    }

    private fun displayResult() {
        tempImagePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = decodeSampledBitmapFromFile(file, 1024, 1024)
                    if (bitmap != null && _binding != null) {
                        binding.imgCaptured.setImageBitmap(bitmap)

                        try {
                            val startTime = System.currentTimeMillis()
                            val result = classifier.classify(bitmap)
                            val inferenceMs = System.currentTimeMillis() - startTime

                            binding.tvChipInferenceTime.text = "⚡ $inferenceMs ms"

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
                            val confidencePct = (finalResult.confidence * 100).toInt()
                            binding.progressConfidence.progress = confidencePct
                            binding.tvClassificationResult.text = finalResult.category
                            binding.tvLblConfidence.text = getString(R.string.confidence_label, confidencePct)

                            // Sesuaikan warna teks hasil secara dinamis
                            val colorRes = when {
                                finalResult.category.contains("ORGANIK") -> R.color.brand_primary
                                finalResult.category.contains("ANORGANIK") -> R.color.brand_primary
                                else -> R.color.brand_warning_red
                            }
                            val colorValue = ContextCompat.getColor(requireContext(), colorRes)
                            binding.tvClassificationResult.setTextColor(colorValue)

                            updateInstructions(finalResult.category)
                        } catch (e: Exception) {
                            if (isAdded) {
                                Toast.makeText(context, getString(R.string.error_classification, e.message), Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        if (isAdded) {
                            Toast.makeText(context, getString(R.string.error_image_not_loaded), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(context, getString(R.string.error_file_not_found), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(context, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
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

        if (result == null || tempPath == null) return

        binding.btnSimpanCatatan.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Pindahkan file dari cacheDir ke filesDir di latar belakang (IO thread)
                val permanentPath = withContext(Dispatchers.IO) {
                    moveToPermanentStorage(File(tempPath))
                }

                if (permanentPath == null) {
                    if (isAdded) {
                        Toast.makeText(context, "Gagal menyimpan foto secara permanen.", Toast.LENGTH_SHORT).show()
                    }
                    binding.btnSimpanCatatan.isEnabled = true
                    return@launch
                }

                permanentImagePath = permanentPath

                val entity = ScanEntity(
                    category = result.category,
                    confidence = (result.confidence * 100).toDouble(),
                    imagePath = permanentPath
                )
                repository.insert(entity)

                if (isAdded) {
                    Toast.makeText(context, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                }

                // Bersihkan backstack ke DashboardFragment agar navigasi tab bottom bar tidak lockup
                findNavController().popBackStack(R.id.navigation_dashboard, false)

                // Pindahkan otomatis ke tab "Catatan"
                val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.navigation_history

            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(context, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                }
                binding.btnSimpanCatatan.isEnabled = true
            }
        }
    }

    /**
     * Memindahkan file sementara dari cacheDir ke direktori permanen [filesDir]/scan_images/.
     * Mengembalikan path permanen jika berhasil, null jika gagal.
     */
    private fun moveToPermanentStorage(tempFile: File): String? {
        return try {
            val scanImagesDir = File(requireContext().filesDir, "scan_images")
            if (!scanImagesDir.exists()) {
                scanImagesDir.mkdirs()
            }
            // Ganti awalan TEMP_ dengan IMG_ untuk menandai file sudah permanen
            val permanentName = tempFile.name.replace("TEMP_", "IMG_")
            val permanentFile = File(scanImagesDir, permanentName)
            tempFile.copyTo(permanentFile, overwrite = true)
            tempFile.delete() // Bersihkan file sementara dari cache
            permanentFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Menangani tombol Kembali.
     * Jika mode pindai baru, file sementara di cacheDir dibersihkan
     * agar tidak menumpuk di penyimpanan perangkat.
     */
    private fun handleBack() {
        if (!isReadOnly) {
            // Bersihkan file sementara yang belum disimpan (pengguna membatalkan)
            tempImagePath?.let { path ->
                val tempFile = File(path)
                if (tempFile.exists() && tempFile.name.startsWith("TEMP_")) {
                    tempFile.delete()
                }
            }
        }
        findNavController().navigateUp()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Memuat dan menampilkan gambar dengan downsampling cerdas (smart inSampleSize)
     * untuk mencegah OutOfMemoryError pada kamera beresolusi tinggi.
     */
    private fun loadImage(file: File) {
        val bitmap = decodeSampledBitmapFromFile(file, 1024, 1024)
        if (bitmap != null && _binding != null) {
            binding.imgCaptured.setImageBitmap(bitmap)
        }
    }

    private fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
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
                this.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        }
    }

    private fun updateInstructions(category: String) {
        when {
            category.contains("ANORGANIK") -> {
                binding.tvBullet1.text = getString(R.string.anorganik_bullet1)
                binding.tvBullet2.text = getString(R.string.anorganik_bullet2)
                binding.tvBullet3.text = getString(R.string.anorganik_bullet3)
            }
            category.contains("ORGANIK") -> {
                binding.tvBullet1.text = getString(R.string.organik_bullet1)
                binding.tvBullet2.text = getString(R.string.organik_bullet2)
                binding.tvBullet3.text = getString(R.string.organik_bullet3)
            }
            else -> {
                binding.tvBullet1.text = "Foto kurang jelas atau objek tidak dikenali."
                binding.tvBullet2.text = "Pastikan cahaya cukup dan objek berada di tengah kamera."
                binding.tvBullet3.text = "Coba pindai ulang dengan sudut pengambilan yang berbeda."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classifier.close()
        _binding = null
    }
}
