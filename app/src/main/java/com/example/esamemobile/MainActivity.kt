package com.example.esamemobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.ActivityMainBinding
import com.example.esamemobile.util.SettingsPrefs
import com.example.esamemobile.work.PantryMatchScheduler
import kotlinx.coroutines.launch

// Unica Activity dell'app: ospita Toolbar, BottomNav e il NavHost, e gestisce la sessione all'avvio.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    // esito ignorato: se l'utente nega, semplicemente non riceve le notifiche
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {  }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // va chiamato prima di super.onCreate()
        // il look "Tavola" e chiaro/caldo: forzo il tema chiaro anche se il sistema e in dark
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.pantryFragment, R.id.searchFragment, R.id.friendsFragment,
                R.id.recipeBookFragment, R.id.profileFragment,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // la bottom bar compare solo sulle 5 sezioni principali
            val topLevel = destination.id in appBarConfiguration.topLevelDestinations
            binding.bottomNav.visibility = if (topLevel) View.VISIBLE else View.GONE
            // queste schermate hanno un header interno "Tavola": nascondo la top bar globale
            val ownHeader = destination.id == R.id.searchFragment ||
                destination.id == R.id.pantryFragment ||
                destination.id == R.id.friendsFragment ||
                destination.id == R.id.recipeBookFragment ||
                destination.id == R.id.profileFragment ||
                destination.id == R.id.friendRecipesFragment ||
                destination.id == R.id.recipesGridFragment
            binding.appBar.visibility = if (ownHeader) View.GONE else View.VISIBLE
        }

        val authRepository = AuthRepository()
        val loggedIn = runCatching { authRepository.isLoggedIn }.getOrDefault(false)
        if (!loggedIn) {
            // non autenticato: parto dal login svuotando il back stack
            navController.navigate(
                R.id.loginFragment,
                null,
                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } },
            )
        } else {
            lifecycleScope.launch {
                // riallineo l'email dopo un eventuale cambio; se la sessione e stata revocata torno al login
                val result = runCatching { authRepository.reconcileEmail() }.getOrNull()
                if (result is AuthRepository.EmailSync.SessionRevoked) {
                    Toast.makeText(
                        this@MainActivity, R.string.session_revoked_relogin, Toast.LENGTH_LONG
                    ).show()
                    navController.navigate(
                        R.id.loginFragment,
                        null,
                        navOptions { popUpTo(R.id.nav_graph) { inclusive = true } },
                    )
                }
            }
        }

        ensureNotificationPermission()
        // rispetto la preferenza utente: pianifico o annullo il worker di conseguenza
        if (SettingsPrefs.notificationsEnabled(applicationContext)) {
            PantryMatchScheduler.schedule(applicationContext)
        } else {
            PantryMatchScheduler.cancel(applicationContext)
        }
    }

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
