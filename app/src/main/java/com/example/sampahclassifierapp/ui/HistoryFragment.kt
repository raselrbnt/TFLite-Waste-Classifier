package com.example.sampahclassifierapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.LearnEntity
import com.example.sampahclassifierapp.data.ScanEntity
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ScanRepository
    private lateinit var historyAdapter: ScanHistoryAdapter
    private lateinit var learnAdapter: LearnAdapter

    // Default ke tab "CATATAN" agar selaras dengan menu navigasi bawah dan alur setelah menyimpan hasil
    private var currentTab = "CATATAN"
    private val learnItems: List<LearnEntity> by lazy { buildLearnData() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        savedInstanceState?.getString(KEY_CURRENT_TAB)?.let { savedTab ->
            currentTab = savedTab
        }

        val database = AppDatabase.getDatabase(requireContext())
        repository = ScanRepository(database.scanDao())

        setupAdapters()
        setupTabs()
        observeData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAB, currentTab)
    }

    private fun setupAdapters() {
        historyAdapter = ScanHistoryAdapter(
            onItemClick = { scan ->
                if (!isAdded || _binding == null) return@ScanHistoryAdapter
                try {
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.navigation_history) {
                        // Kirim argumen mode pratinjau (read-only) beserta data yang sudah tersimpan di DB
                        // agar ResultFragment tidak perlu menjalankan inferensi ML ulang.
                        val bundle = Bundle().apply {
                            putString("imagePath", scan.imagePath)
                            putBoolean("isReadOnly", true)
                            putString("savedCategory", scan.category)
                            putFloat("savedConfidence", scan.confidence.toFloat())
                        }
                        navController.navigate(R.id.action_history_to_result, bundle)
                    }
                } catch (e: Exception) {
                    // Abaikan jika transisi navigasi sedang berlangsung
                }
            },
            onDeleteClick = { scan ->
                deleteScanHistory(scan)
            }
        )
        learnAdapter = LearnAdapter()
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupTabs() {
        binding.tabCatatan.setOnClickListener {
            switchTab("CATATAN")
        }
        binding.tabBasisData.setOnClickListener {
            switchTab("BASIS_DATA")
        }
        switchTab(currentTab)
    }

    private fun switchTab(tab: String) {
        val currentBinding = _binding ?: return
        val ctx = context ?: return
        currentTab = tab

        if (tab == "CATATAN") {
            currentBinding.tabCatatan.setBackgroundResource(R.drawable.bg_tab_active_pill)
            currentBinding.tabCatatan.setTextColor(ContextCompat.getColor(ctx, R.color.brand_surface_white))
            currentBinding.tabBasisData.setBackgroundResource(android.R.color.transparent)
            currentBinding.tabBasisData.setTextColor(ContextCompat.getColor(ctx, R.color.brand_dark_charcoal))
            currentBinding.tvLearnSubtitle.text =
                "Daftar riwayat hasil pemindaian sampah yang tersimpan di perangkat. Ketuk item untuk melihat detail."
            currentBinding.rvItems.adapter = historyAdapter
            updateEmptyState("CATATAN", historyAdapter.currentList.isEmpty())
        } else {
            currentBinding.tabBasisData.setBackgroundResource(R.drawable.bg_tab_active_pill)
            currentBinding.tabBasisData.setTextColor(ContextCompat.getColor(ctx, R.color.brand_surface_white))
            currentBinding.tabCatatan.setBackgroundResource(android.R.color.transparent)
            currentBinding.tabCatatan.setTextColor(ContextCompat.getColor(ctx, R.color.brand_dark_charcoal))
            currentBinding.tvLearnSubtitle.text = getString(R.string.learn_subtitle)
            currentBinding.rvItems.adapter = learnAdapter
            learnAdapter.submitList(learnItems)
            updateEmptyState("BASIS_DATA", false)
        }
    }

    private fun updateEmptyState(tab: String, isEmpty: Boolean) {
        val currentBinding = _binding ?: return
        if (tab == "CATATAN" && isEmpty) {
            currentBinding.layoutEmptyState.visibility = View.VISIBLE
            currentBinding.rvItems.visibility = View.GONE
        } else {
            currentBinding.layoutEmptyState.visibility = View.GONE
            currentBinding.rvItems.visibility = View.VISIBLE
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.allScans.collectLatest { scans ->
                    historyAdapter.submitList(scans) {
                        // Null-safe check wajib karena callback submitList berjalan secara asinkron
                        if (_binding != null && isAdded && currentTab == "CATATAN") {
                            updateEmptyState("CATATAN", scans.isEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun buildLearnData(): List<LearnEntity> {
        return listOf(
            LearnEntity(
                title = "SAMPAH ORGANIK (KOMPOS & BIOLOGIS)",
                description = "• Contoh Material: Sisa makanan, kulit buah, sayuran layu, daun kering, ranting kecil, ampas kopi/teh, dan cangkang telur.\n" +
                    "• Karakteristik: Mudah membusuk dan terurai secara alami oleh mikroorganisme (biodegradable).\n" +
                    "• Cara Penanganan: Pisahkan dari bungkus plastik atau karet pengikat, tiriskan cairan berlebih, lalu masukkan ke wadah khusus kompos atau biopori untuk diolah menjadi pupuk organik.",
                iconResId = R.drawable.ic_organic_apple,
                isExpanded = true
            ),
            LearnEntity(
                title = "SAMPAH ANORGANIK (DAUR ULANG)",
                description = "• Contoh Material: Botol plastik (PET), gelas plastik, kantong kresek, kardus, kertas HVS/koran, kaleng aluminium/besi, dan botol kaca.\n" +
                    "• Karakteristik: Sulit terurai secara alami namun memiliki nilai ekonomis tinggi untuk didaur ulang di Bank Sampah.\n" +
                    "• Cara Penanganan: Kosongkan dan bilas wadah dari sisa makanan/minuman, keringkan, pipihkan botol plastik atau lipat kardus untuk menghemat ruang, lalu setorkan ke Bank Sampah Mekar Sari.",
                iconResId = R.drawable.ic_recycle_bin,
                isExpanded = false
            ),
            LearnEntity(
                title = "PANDUAN PEMINDAIAN AI AKURAT (≥ 70%)",
                description = "• Pencahayaan Cukup: Pastikan objek sampah mendapat cahaya yang merata dan tidak tertutup bayangan gelap.\n" +
                    "• Fokus pada Satu Objek: Tempatkan objek tepat di dalam bingkai retikel tengah dengan jarak sekitar 15–30 cm.\n" +
                    "• Ambang Kepercayaan 70%: Jika skor kepercayaan di bawah 70%, sistem akan menampilkan status TIDAK TERDETEKSI sebagai pengaman agar petugas memotret ulang dari sudut yang lebih jelas.",
                iconResId = R.drawable.ic_scanner,
                isExpanded = false
            ),
            LearnEntity(
                title = "PROSEDUR BANK SAMPAH MEKAR SARI",
                description = "• Pemilahan dari Sumber: Pisahkan sampah organik dan anorganik sebelum dimasukkan ke karung setor untuk memangkas waktu sortir manual.\n" +
                    "• Pengelompokan Material: Kelompokkan sampah anorganik berdasarkan jenis (plastik, kertas/karton, logam, kaca) sebelum ditimbang.\n" +
                    "• Pencatatan Digital: Gunakan fitur Simpan Catatan untuk merekam bukti klasifikasi beserta foto dan tingkat kepercayaan secara luring (offline).",
                iconResId = R.drawable.ic_eco_leaf_logo,
                isExpanded = false
            )
        )
    }

    private fun deleteScanHistory(scan: ScanEntity) {
        val ctx = context ?: return
        if (!isAdded) return

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Hapus Catatan Riwayat?")
            .setMessage("Apakah Anda yakin ingin menghapus catatan pemindaian ${scan.category} ini dari basis data lokal?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                if (!isAdded) return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch {
                    // Hapus berkas foto dari penyimpanan internal (filesDir) di IO thread bersama entitas DB-nya
                    withContext(Dispatchers.IO) {
                        try {
                            val file = File(scan.imagePath)
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            // Abaikan kesalahan penghapusan file
                        }
                    }
                    ScanHistoryAdapter.evictFromCache(scan.imagePath)
                    repository.delete(scan)
                    if (isAdded) {
                        context?.let { safeCtx ->
                            Toast.makeText(safeCtx, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        _binding?.rvItems?.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_CURRENT_TAB = "key_current_tab"
    }
}
