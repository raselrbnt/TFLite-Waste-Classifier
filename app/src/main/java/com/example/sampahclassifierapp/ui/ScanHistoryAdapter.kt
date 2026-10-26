package com.example.sampahclassifierapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.data.ScanEntity
import com.example.sampahclassifierapp.databinding.ItemScanHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ScanHistoryAdapter(
    private val onItemClick: (ScanEntity) -> Unit,
    private val onDeleteClick: (ScanEntity) -> Unit
) : ListAdapter<ScanEntity, ScanHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemScanHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        private var imageLoadJob: Job? = null

        fun bind(scan: ScanEntity, onItemClick: (ScanEntity) -> Unit, onDeleteClick: (ScanEntity) -> Unit) {
            val context = binding.root.context
            binding.tvHistoryCategory.text = scan.category
            binding.tvHistoryConfidence.text = String.format(
                context.getString(R.string.confidence_display),
                scan.confidence.roundToInt().coerceIn(0, 100)
            )
            binding.tvHistoryDate.text = dateFormat.format(Date(scan.timestamp))

            // Warna indikator dan teks kategori sesuai jenis hasil (Organik vs Anorganik vs Tidak Terdeteksi)
            val isUndetected = scan.category == "TIDAK TERDETEKSI"
            val indicatorColorRes = when {
                isUndetected -> R.color.brand_warning_red
                scan.category.contains("ORGANIK") && !scan.category.contains("ANORGANIK") -> R.color.brand_primary
                else -> R.color.brand_neon
            }
            val titleColorRes = if (isUndetected) R.color.brand_warning_red else R.color.brand_dark_charcoal

            binding.tvHistoryCategory.setTextColor(ContextCompat.getColor(context, titleColorRes))
            binding.viewHistoryIndicator.setBackgroundColor(ContextCompat.getColor(context, indicatorColorRes))

            // Muat thumbnail secara asinkron dengan LruCache + downsampling agar perpindahan tab & scroll 60fps
            imageLoadJob?.cancel()
            val path = scan.imagePath
            binding.imgHistory.tag = path

            val cachedBitmap = thumbnailCache.get(path)
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                binding.imgHistory.setImageBitmap(cachedBitmap)
            } else {
                binding.imgHistory.setImageResource(android.R.drawable.ic_menu_gallery)
                imageLoadJob = adapterScope.launch {
                    val decoded = withContext(Dispatchers.IO) {
                        decodeThumbnail(path, 160, 160)
                    }
                    if (decoded != null) {
                        thumbnailCache.put(path, decoded)
                        if (binding.imgHistory.tag == path) {
                            binding.imgHistory.setImageBitmap(decoded)
                        }
                    }
                }
            }

            binding.root.setOnClickListener {
                onItemClick(scan)
            }

            binding.btnDeleteHistory.setOnClickListener {
                onDeleteClick(scan)
            }
        }

        fun clearJob() {
            imageLoadJob?.cancel()
            imageLoadJob = null
        }

        private fun decodeThumbnail(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
            return try {
                val imgFile = File(path)
                if (!imgFile.exists()) return null

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imgFile.absolutePath, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false

                val rawBitmap = BitmapFactory.decodeFile(imgFile.absolutePath, options) ?: return null
                applyExifOrientation(rawBitmap, imgFile.absolutePath)
            } catch (e: OutOfMemoryError) {
                System.gc()
                null
            } catch (e: Exception) {
                null
            }
        }

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

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clearJob()
        super.onViewRecycled(holder)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScanEntity>() {
        private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        // Cache thumbnail di memori (maksimal 40 item) agar scroll & pindah tab instan
        private val thumbnailCache = object : LruCache<String, Bitmap>(40) {}

        fun evictFromCache(path: String) {
            thumbnailCache.remove(path)
        }

        override fun areItemsTheSame(oldItem: ScanEntity, newItem: ScanEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanEntity, newItem: ScanEntity): Boolean {
            return oldItem == newItem
        }
    }
}
