package com.example.sampahclassifierapp.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentDashboardBinding
import com.example.sampahclassifierapp.ml.WasteClassifier
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ScanRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        // Inisialisasi Repository
        val database = AppDatabase.getDatabase(requireContext())
        repository = ScanRepository(database.scanDao())

        setupStatistics()
        setupMlSystemStatus()

        // Tombol navigasi ke Scanner
        binding.btnMulaiPemindai.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_scanner)
        }
    }

    private fun setupMlSystemStatus() {
        val isMlActive = WasteClassifier.isModelAvailable(requireContext())

        if (isMlActive) {
            binding.tvStatusTitle.text = "SYSTEM ACTIVE"
            binding.tvStatusTitle.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.brand_primary)
            )
            binding.tvStatusSubtitle.text = "Status: Model TFLite offline siap digunakan."
        } else {
            binding.tvStatusTitle.text = "SYSTEM ERROR"
            binding.tvStatusTitle.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.brand_warning_red)
            )
            binding.tvStatusSubtitle.text = "Status: Gagal memuat file model sampah."
        }
    }

    private fun setupStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.totalCount.collectLatest { count ->
                binding.valTotalScan.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.organikCount.collectLatest { count ->
                binding.valOrganik.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.anorganikCount.collectLatest { count ->
                binding.valAnorganik.text = count.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
