package com.example.pantrymatch

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
import com.example.pantrymatch.data.repository.AuthRepository
import com.example.pantrymatch.databinding.ActivityMainBinding
import com.example.pantrymatch.util.SettingsPrefs
import com.example.pantrymatch.work.PantryMatchScheduler
import kotlinx.coroutines.launch

// The app's single Activity: hosts the Toolbar, BottomNav and NavHost, and handles the session at startup.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    // result ignored: if the user denies, they simply get no notifications
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {  }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // must be called before super.onCreate()
        // the "Tavola" look is light/warm: force the light theme even if the system is in dark mode
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
            // the bottom bar is shown only on the 5 main sections
            val topLevel = destination.id in appBarConfiguration.topLevelDestinations
            binding.bottomNav.visibility = if (topLevel) View.VISIBLE else View.GONE
            // these screens have their own "Tavola" header: hide the global top bar
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
            // not signed in: start from login and clear the back stack
            navController.navigate(
                R.id.loginFragment,
                null,
                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } },
            )
        } else {
            lifecycleScope.launch {
                // re-sync the email after a possible change; if the session was revoked, go back to login
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
        // honor the user preference: schedule or cancel the worker accordingly
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
