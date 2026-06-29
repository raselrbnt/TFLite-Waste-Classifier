package com.example.sampahclassifierapp.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sampahclassifierapp.data.ScanEntity
import com.example.sampahclassifierapp.databinding.ItemScanHistoryBinding
import com.example.sampahclassifierapp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanHistoryAdapter : ListAdapter<ScanEntity, ScanHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemScanHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        fun bind(scan: ScanEntity) {
            val context = binding.root.context
            binding.tvHistoryCategory.text = scan.category
            binding.tvHistoryConfidence.text = String.format(
                context.getString(R.string.confidence_display),
                scan.confidence.toInt()
            )
            binding.tvHistoryDate.text = dateFormat.format(Date(scan.timestamp))

            // Dapatkan warna berdasarkan kategori
            if (scan.category == "TIDAK TERDETEKSI") {
                binding.tvHistoryCategory.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_warning_red))
                binding.viewHistoryIndicator.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_warning_red))
            } else {
                binding.tvHistoryCategory.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_dark_charcoal))
                binding.viewHistoryIndicator.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_neon))
            }

            // Load image from internal storage path
            val imgFile = File(scan.imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imgHistory.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScanEntity>() {
        override fun areItemsTheSame(oldItem: ScanEntity, newItem: ScanEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanEntity, newItem: ScanEntity): Boolean {
            return oldItem == newItem
        }
    }
}
