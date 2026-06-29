package com.example.sampahclassifierapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.sampahclassifierapp.R
import com.example.sampahclassifierapp.databinding.FragmentScannerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScannerFragment : Fragment(R.layout.fragment_scanner) {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var camera: androidx.camera.core.Camera? = null
    private var isFlashOn = false

    private val getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val photoFile = File(
                    requireContext().cacheDir,
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + "_gallery.jpg"
                )
                photoFile.outputStream().use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                
                val bundle = Bundle().apply {
                    putString("imagePath", photoFile.absolutePath)
                }
                findNavController().navigate(R.id.action_scanner_to_result, bundle)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memproses gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScannerBinding.bind(view)

        // Inisialisasi tint tombol flash
        binding.btnFlash.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.brand_dark_charcoal)
        )

        checkPermissions()

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            getContentLauncher.launch("image/*")
        }

        binding.btnFlash.setOnClickListener {
            val cam = camera
            if (cam != null && cam.cameraInfo.hasFlashUnit()) {
                isFlashOn = !isFlashOn
                cam.cameraControl.enableTorch(isFlashOn)
                if (isFlashOn) {
                    binding.btnFlash.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.brand_neon)
                    )
                } else {
                    binding.btnFlash.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.brand_dark_charcoal)
                    )
                }
            } else {
                Toast.makeText(context, "Flash tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.camera_failed), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Menonaktifkan kontrol untuk mencegah penekanan ganda
        binding.btnCapture.isEnabled = false
        binding.btnGallery.isEnabled = false
        binding.btnFlash.isEnabled = false

        // Animasi Shutter Flash Efek Kamera Premium
        binding.viewShutterFlash.visibility = View.VISIBLE
        binding.viewShutterFlash.alpha = 1f
        binding.viewShutterFlash.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.viewShutterFlash.visibility = View.GONE
            }
            .start()

        val photoFile = File(
            requireContext().cacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Aktifkan kembali kontrol sebelum berpindah layar
                    binding.btnCapture.isEnabled = true
                    binding.btnGallery.isEnabled = true
                    binding.btnFlash.isEnabled = true

                    val bundle = Bundle().apply {
                        putString("imagePath", photoFile.absolutePath)
                    }
                    findNavController().navigate(R.id.action_scanner_to_result, bundle)
                }

                override fun onError(exception: ImageCaptureException) {
                    // Aktifkan kembali kontrol jika terjadi galat
                    binding.btnCapture.isEnabled = true
                    binding.btnGallery.isEnabled = true
                    binding.btnFlash.isEnabled = true
                    Toast.makeText(context, getString(R.string.photo_failed), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
