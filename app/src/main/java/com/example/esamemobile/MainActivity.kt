package com.example.esamemobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.ActivityMainBinding
import com.example.esamemobile.work.PantryMatchScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Step 7: richiesta del permesso notifiche (Android 13+); l'esito non blocca l'app.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        // Le 4 destinazioni di primo livello (niente freccia "back", titolo dalla label)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.pantryFragment, R.id.searchFragment, R.id.friendsFragment, R.id.profileFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        // Nascondi la bottom bar nelle schermate di dettaglio/login
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevel = destination.id in appBarConfiguration.topLevelDestinations
            binding.bottomNav.visibility = if (topLevel) View.VISIBLE else View.GONE
        }

        // Gestione sessione: se non autenticato, parti dal login svuotando il back stack.
        val loggedIn = runCatching { AuthRepository().isLoggedIn }.getOrDefault(false)
        if (!loggedIn) {
            navController.navigate(
                R.id.loginFragment,
                null,
                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } },
            )
        }

        // Step 7: chiedi il permesso notifiche e pianifica il task periodico in background.
        ensureNotificationPermission()
        PantryMatchScheduler.schedule(applicationContext)
    }

    /** Su Android 13+ il permesso POST_NOTIFICATIONS va richiesto a runtime. */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
