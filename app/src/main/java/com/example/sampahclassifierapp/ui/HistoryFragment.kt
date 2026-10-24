package com.example.sampahclassifierapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.LearnEntity
import com.example.sampahclassifierapp.data.ScanEntity
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ScanRepository
    private lateinit var historyAdapter: ScanHistoryAdapter
    private lateinit var learnAdapter: LearnAdapter

    private var currentTab = "BASIS_DATA"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        val database = AppDatabase.getDatabase(requireContext())
        repository = ScanRepository(database.scanDao())

        setupAdapters()
        setupTabs()
        observeData()
    }

    private fun setupAdapters() {
        historyAdapter = ScanHistoryAdapter(
            onItemClick = { scan ->
                // Kirim argumen mode pratinjau (read-only) beserta data yang sudah tersimpan di DB
                // agar ResultFragment tidak perlu menjalankan inferensi ML ulang.
                val bundle = Bundle().apply {
                    putString("imagePath", scan.imagePath)
                    putBoolean("isReadOnly", true)
                    putString("savedCategory", scan.category)
                    putFloat("savedConfidence", scan.confidence.toFloat())
                }
                findNavController().navigate(R.id.action_history_to_result, bundle)
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
        currentTab = tab
        if (tab == "CATATAN") {
            binding.tabCatatan.setBackgroundResource(R.drawable.bg_tab_active_pill)
            binding.tabCatatan.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_surface_white))
            binding.tabBasisData.setBackgroundResource(android.R.color.transparent)
            binding.tabBasisData.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_dark_charcoal))
            binding.rvItems.adapter = historyAdapter
            updateEmptyState("CATATAN", historyAdapter.currentList.isEmpty())
        } else {
            binding.tabBasisData.setBackgroundResource(R.drawable.bg_tab_active_pill)
            binding.tabBasisData.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_surface_white))
            binding.tabCatatan.setBackgroundResource(android.R.color.transparent)
            binding.tabCatatan.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_dark_charcoal))
            binding.rvItems.adapter = learnAdapter
            learnAdapter.submitList(getDummyLearnData())
            updateEmptyState("BASIS_DATA", false)
        }
    }

    private fun updateEmptyState(tab: String, isEmpty: Boolean) {
        if (tab == "CATATAN" && isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvItems.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvItems.visibility = View.VISIBLE
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allScans.collectLatest { scans ->
                historyAdapter.submitList(scans) {
                    if (currentTab == "CATATAN") {
                        updateEmptyState("CATATAN", scans.isEmpty())
                    }
                }
            }
        }
    }

    private fun getDummyLearnData(): List<LearnEntity> {
        return listOf(
            LearnEntity("ORGANIK", "Sisa makanan, daun, ranting.", android.R.drawable.ic_menu_delete),
            LearnEntity("ANORGANIK", "Plastik, kertas, logam.", android.R.drawable.ic_menu_share)
        )
    }

    private fun deleteScanHistory(scan: ScanEntity) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Catatan Riwayat?")
            .setMessage("Apakah Anda yakin ingin menghapus catatan pemindaian ${scan.category} ini dari basis data lokal?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Hapus berkas foto dari penyimpanan internal (filesDir) bersama entitas DB-nya
                    try {
                        val file = File(scan.imagePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        // Abaikan kesalahan penghapusan file
                    }
                    repository.delete(scan)
                    Toast.makeText(context, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
