package com.example.sampahclassifierapp.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.AppDatabase
import com.example.sampahclassifierapp.data.LearnEntity
import com.example.sampahclassifierapp.data.ScanRepository
import com.example.sampahclassifierapp.databinding.FragmentHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        historyAdapter = ScanHistoryAdapter()
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
            binding.tabCatatan.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_neon))
            binding.tabCatatan.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_dark_charcoal))
            binding.tabBasisData.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_surface_white))
            binding.tabBasisData.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary))
            binding.rvItems.adapter = historyAdapter
            updateEmptyState("CATATAN", historyAdapter.currentList.isEmpty())
        } else {
            binding.tabBasisData.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_neon))
            binding.tabBasisData.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_dark_charcoal))
            binding.tabCatatan.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_surface_white))
            binding.tabCatatan.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
