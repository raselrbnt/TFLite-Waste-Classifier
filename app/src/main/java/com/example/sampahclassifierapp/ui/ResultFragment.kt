package com.example.sampahclassifierapp.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.ScanEntity
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentResultBinding
import com.example.sampahclassifierapp.ml.WasteClassifier
import kotlinx.coroutines.launch
import java.io.File

class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ScanRepository
    private lateinit var classifier: WasteClassifier
    private var currentImagePath: String? = null
    private var currentResult: WasteClassifier.Recognition? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)
        
        // Inisialisasi Repository
        val database = AppDatabase.getDatabase(requireContext())
        repository = ScanRepository(database.scanDao())
        
        // Inisialisasi Classifier
        classifier = WasteClassifier(requireContext())

        // Ambil data dari arguments
        currentImagePath = arguments?.getString("imagePath")
        
        displayResult()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPindaiLainnya.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSimpanCatatan.setOnClickListener {
            saveToDatabase()
        }
    }

    private fun displayResult() {
        currentImagePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        binding.imgCaptured.setImageBitmap(bitmap)

                        try {
                            val result = classifier.classify(bitmap)
                            
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

                            binding.tvClassificationResult.text = finalResult.category
                            binding.tvLblConfidence.text = getString(R.string.confidence_label, (finalResult.confidence * 100).toInt())
                            
                            // Sesuaikan warna teks hasil dan indikator secara dinamis
                            val colorRes = if (finalResult.category == "TIDAK TERDETEKSI") R.color.brand_warning_red else R.color.brand_neon
                            val colorValue = androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
                            binding.tvClassificationResult.setTextColor(colorValue)
                            binding.viewResultIndicator.setBackgroundColor(colorValue)
                            
                            updateInstructions(finalResult.category)
                        } catch (e: Exception) {
                            Toast.makeText(context, getString(R.string.error_classification, e.message), Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.error_image_not_loaded), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, getString(R.string.error_file_not_found), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
            }
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

    private fun saveToDatabase() {
        val result = currentResult
        val path = currentImagePath

        if (result != null && path != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val entity = ScanEntity(
                    category = result.category,
                    confidence = (result.confidence * 100).toDouble(),
                    imagePath = path
                )
                repository.insert(entity)
                Toast.makeText(context, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                
                // Bersihkan backstack ke DashboardFragment (start destination) agar navigasi tab bottom bar tidak macet/lockup
                findNavController().popBackStack(R.id.navigation_dashboard, false)
                
                // Pilih tab "Catatan" (navigation_history) pada bottom navigation bar
                val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.navigation_history
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classifier.close()
        _binding = null
    }
}
