package com.example.sampahclassifierapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.sampahclassifierapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Paksa tema Light Mode agar konsisten dengan Design System Lumina Tech-Eco
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Setup Navigation dengan proteksi perpindahan tab cepat (anti-force-close)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentDestId = navController.currentDestination?.id
            if (currentDestId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            try {
                // Jika sedang di halaman ResultFragment, bersihkan dari backstack terlebih dahulu
                if (currentDestId == R.id.navigation_result) {
                    navController.popBackStack(R.id.navigation_result, true)
                }

                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(
                        navController.graph.findStartDestination().id,
                        inclusive = false,
                        saveState = true
                    )
                    .build()

                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (e: Exception) {
                false
            }
        }

        // Cegah re-kreasi fragment / re-bind kamera saat tab yang sedang aktif ditekan berulang kali dengan cepat
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            try {
                if (navController.currentDestination?.id == R.id.navigation_result) {
                    navController.popBackStack(item.itemId, false)
                }
            } catch (e: Exception) {
                // Abaikan jika transisi sedang berjalan
            }
        }

        // Sinkronkan status item terpilih pada BottomNavigationView dengan destinasi aktif
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_dashboard,
                R.id.navigation_scanner,
                R.id.navigation_history -> {
                    binding.bottomNavigation.menu.findItem(destination.id)?.isChecked = true
                }
            }
        }
    }
}
