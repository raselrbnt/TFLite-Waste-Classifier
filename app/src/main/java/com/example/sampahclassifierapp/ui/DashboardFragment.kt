package com.example.sampahclassifierapp.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentDashboardBinding
import com.example.sampahclassifierapp.ml.WasteClassifier
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Tombol navigasi ke Scanner (sinkron dengan BottomNavigationView & aman dari double-tap)
        binding.btnMulaiPemindai.setOnClickListener {
            navigateToTab(R.id.navigation_scanner, R.id.action_dashboard_to_scanner)
        }

        // Kartu statistik dapat diketuk untuk langsung melihat riwayat di tab Catatan
        val openHistoryListener = View.OnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.navigation_history
            }
        }
        binding.gridTotalScan.setOnClickListener(openHistoryListener)
        binding.gridOrganik.setOnClickListener(openHistoryListener)
        binding.gridAnorganik.setOnClickListener(openHistoryListener)
    }

    private fun navigateToTab(tabId: Int, fallbackActionId: Int) {
        if (!isAdded || _binding == null) return
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (bottomNav != null) {
            bottomNav.selectedItemId = tabId
        } else {
            try {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.navigation_dashboard) {
                    navController.navigate(fallbackActionId)
                }
            } catch (e: Exception) {
                // Abaikan jika transisi navigasi sedang berlangsung
            }
        }
    }

    private fun setupMlSystemStatus() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val isMlActive = withContext(Dispatchers.IO) {
                WasteClassifier.isModelAvailable(appContext)
            }
            val currentBinding = _binding ?: return@launch
            if (!isAdded) return@launch

            if (isMlActive) {
                currentBinding.tvStatusTitle.text = "SYSTEM ACTIVE"
                currentBinding.tvStatusTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.brand_primary)
                )
                currentBinding.tvStatusSubtitle.text = "Status: Model TFLite offline siap digunakan."
                currentBinding.tvStandby.text = "[ ML CORE STANDBY ]"
            } else {
                currentBinding.tvStatusTitle.text = "SYSTEM ERROR"
                currentBinding.tvStatusTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.brand_warning_red)
                )
                currentBinding.tvStatusSubtitle.text = "Status: Gagal memuat file model sampah."
                currentBinding.tvStandby.text = "[ ML CORE ERROR ]"
            }
        }
    }

    private fun setupStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repository.totalCount.collectLatest { count ->
                        _binding?.valTotalScan?.text = count.toString()
                    }
                }
                launch {
                    repository.organikCount.collectLatest { count ->
                        _binding?.valOrganik?.text = count.toString()
                    }
                }
                launch {
                    repository.anorganikCount.collectLatest { count ->
                        _binding?.valAnorganik?.text = count.toString()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
