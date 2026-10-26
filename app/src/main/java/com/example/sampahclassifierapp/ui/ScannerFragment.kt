package com.example.sampahclassifierapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.databinding.FragmentScannerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScannerFragment : Fragment(R.layout.fragment_scanner) {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var isFlashOn = false
    private var isCapturing = false

    /**
     * Menyimpan referensi ke file sementara yang sedang menunggu keputusan pengguna
     * (simpan ke catatan atau tidak). File ini berada di cacheDir dan akan dibersihkan
     * secara otomatis oleh ResultFragment jika pengguna kembali tanpa menyimpan.
     */
    private var pendingTempFile: File? = null

    private val getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && isAdded && _binding != null) {
            processGalleryUri(uri)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isAdded || _binding == null) return@registerForActivityResult
        if (isGranted) {
            startCamera()
        } else {
            context?.let { ctx ->
                Toast.makeText(ctx, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScannerBinding.bind(view)
        isCapturing = false
        isFlashOn = false

        // Inisialisasi tint tombol flash
        binding.btnFlash.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.brand_surface_white)
        )

        // Animasi retikel berdenyut halus (subtle pulse effect) dengan null-safe guard
        startReticlePulseAnimation()

        checkPermissions()

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            if (!isCapturing && isAdded) {
                try {
                    getContentLauncher.launch("image/*")
                } catch (e: Exception) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Tidak dapat membuka galeri", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnFlash.setOnClickListener {
            val currentBinding = _binding ?: return@setOnClickListener
            val cam = camera
            if (cam != null && cam.cameraInfo.hasFlashUnit()) {
                isFlashOn = !isFlashOn
                cam.cameraControl.enableTorch(isFlashOn)
                val colorRes = if (isFlashOn) R.color.brand_neon else R.color.brand_surface_white
                currentBinding.btnFlash.setColorFilter(
                    ContextCompat.getColor(requireContext(), colorRes)
                )
            } else {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Flash tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Menjalankan animasi denyut halus pada retikel dengan aman terhadap siklus hidup View.
     * Menggunakan referensi `_binding` nullable agar tidak memicu NullPointerException
     * saat pengguna berpindah halaman dengan cepat.
     */
    private fun startReticlePulseAnimation() {
        val reticle = _binding?.imgReticle ?: return
        reticle.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .alpha(0.85f)
            .setDuration(1200)
            .withEndAction {
                val innerReticle = _binding?.imgReticle ?: return@withEndAction
                innerReticle.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(1200)
                    .start()
            }
            .start()
    }

    private fun processGalleryUri(uri: Uri) {
        val appContext = context?.applicationContext ?: return
        setControlsEnabled(false)

        viewLifecycleOwner.lifecycleScope.launch {
            val savedTempFile = withContext(Dispatchers.IO) {
                try {
                    val tempFile = File(
                        appContext.cacheDir,
                        "TEMP_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())}_gallery.jpg"
                    )
                    val copied = appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        true
                    } ?: false

                    if (copied && tempFile.exists() && tempFile.length() > 0L) tempFile else null
                } catch (e: Exception) {
                    null
                }
            }

            if (_binding == null || !isAdded) {
                savedTempFile?.delete()
                return@launch
            }

            setControlsEnabled(true)

            if (savedTempFile != null) {
                pendingTempFile = savedTempFile
                navigateToResultSafely(savedTempFile.absolutePath)
            } else {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Gagal memproses gambar dari galeri", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPermissions() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val ctx = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            // Proteksi wajib jika pengguna berpindah halaman dengan cepat sebelum kamera selesai inisialisasi
            val currentBinding = _binding
            if (currentBinding == null || !isAdded || view == null) {
                return@addListener
            }

            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(currentBinding.viewFinder.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                // Sinkronkan status flash setelah re-bind
                isFlashOn = false
                context?.let { safeCtx ->
                    currentBinding.btnFlash.setColorFilter(
                        ContextCompat.getColor(safeCtx, R.color.brand_surface_white)
                    )
                }
            } catch (e: Exception) {
                if (isAdded) {
                    context?.let { safeCtx ->
                        Toast.makeText(safeCtx, getString(R.string.camera_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun takePhoto() {
        val captureUseCase = imageCapture ?: return
        val currentBinding = _binding ?: return
        val ctx = context ?: return
        if (isCapturing || !isAdded) return

        isCapturing = true
        setControlsEnabled(false)

        // Animasi Shutter Flash Efek Kamera Premium (null-safe terhadap perpindahan halaman cepat)
        currentBinding.viewShutterFlash.visibility = View.VISIBLE
        currentBinding.viewShutterFlash.alpha = 1f
        currentBinding.viewShutterFlash.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                _binding?.viewShutterFlash?.visibility = View.GONE
            }
            .start()

        // Simpan foto di cacheDir sebagai berkas sementara (prefiks TEMP_).
        val tempFile = File(
            ctx.cacheDir,
            "TEMP_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())}.jpg"
        )
        pendingTempFile = tempFile

        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        captureUseCase.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    if (_binding == null || !isAdded) {
                        tempFile.delete()
                        return
                    }
                    setControlsEnabled(true)
                    navigateToResultSafely(tempFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    pendingTempFile?.delete()
                    pendingTempFile = null
                    if (_binding == null || !isAdded) return
                    setControlsEnabled(true)
                    context?.let { safeCtx ->
                        Toast.makeText(safeCtx, getString(R.string.photo_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun navigateToResultSafely(imagePath: String) {
        if (!isAdded || _binding == null) return
        try {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.navigation_scanner) {
                val bundle = Bundle().apply {
                    putString("imagePath", imagePath)
                    putBoolean("isReadOnly", false)
                }
                navController.navigate(R.id.action_scanner_to_result, bundle)
            } else {
                // Jika sudah berpindah destinasi saat foto selesai disimpan, hapus file sementara
                File(imagePath).delete()
            }
        } catch (e: Exception) {
            File(imagePath).delete()
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        _binding?.let { b ->
            b.btnCapture.isEnabled = enabled
            b.btnGallery.isEnabled = enabled
            b.btnFlash.isEnabled = enabled
        }
    }

    override fun onDestroyView() {
        // Batalkan semua animasi ViewPropertyAnimator agar callback withEndAction tidak jalan setelah view hancur
        _binding?.imgReticle?.animate()?.cancel()
        _binding?.viewShutterFlash?.animate()?.cancel()

        // Lepaskan kamera secara eksplisit untuk membebaskan resource hardware seketika saat pindah tab cepat
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Abaikan
        }
        camera = null
        imageCapture = null
        cameraProvider = null
        isCapturing = false

        super.onDestroyView()
        _binding = null
    }
}
