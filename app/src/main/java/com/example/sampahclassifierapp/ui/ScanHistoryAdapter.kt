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

class ScanHistoryAdapter(
    private val onItemClick: (ScanEntity) -> Unit,
    private val onDeleteClick: (ScanEntity) -> Unit
) : ListAdapter<ScanEntity, ScanHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemScanHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        fun bind(scan: ScanEntity, onItemClick: (ScanEntity) -> Unit, onDeleteClick: (ScanEntity) -> Unit) {
            val context = binding.root.context
            binding.tvHistoryCategory.text = scan.category
            binding.tvHistoryConfidence.text = String.format(
                context.getString(R.string.confidence_display),
                scan.confidence.toInt()
            )
            binding.tvHistoryDate.text = dateFormat.format(Date(scan.timestamp))

            // Dapatkan warna indikator berdasarkan kategori (Organik vs Anorganik vs Tidak Terdeteksi)
            val indicatorColorRes = when {
                scan.category == "TIDAK TERDETEKSI" -> R.color.brand_warning_red
                scan.category.contains("ORGANIK") -> R.color.brand_primary
                else -> R.color.brand_neon
            }
            val indicatorColor = androidx.core.content.ContextCompat.getColor(context, indicatorColorRes)
            binding.tvHistoryCategory.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_dark_charcoal))
            binding.viewHistoryIndicator.setBackgroundColor(indicatorColor)

            // Load image with downsampling to optimize RAM memory and ensure 60fps smooth scrolling
            val imgFile = File(scan.imagePath)
            if (imgFile.exists()) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imgFile.absolutePath, options)
                options.inSampleSize = calculateInSampleSize(options, 120, 120)
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath, options)
                binding.imgHistory.setImageBitmap(bitmap)
            }

            binding.root.setOnClickListener {
                onItemClick(scan)
            }

            binding.btnDeleteHistory.setOnClickListener {
                onDeleteClick(scan)
            }
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onDeleteClick)
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
