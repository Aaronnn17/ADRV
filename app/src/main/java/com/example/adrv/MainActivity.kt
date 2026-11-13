package com.example.adrv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.adrv.ui.screens.MainScreen
import com.example.adrv.ui.theme.ADRVTheme
import java.util.concurrent.TimeUnit

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}

interface ThemeController {
    val isDarkTheme: Boolean
    fun toggleTheme()
}

// Variable global temporal para saber si la app se abrió desde una notificación
object NotificationNavigator {
    var pendingTargetType: String? = null
    var pendingTargetData: String? = null
}
class MainActivity : ComponentActivity() {

    // Lanzador para pedir permisos de notificación en Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) { startBackgroundWorker() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Capturamos los datos si el usuario pulsó la notificación
        intent?.let {
            val type = it.getStringExtra("target_type")
            val data = it.getStringExtra("target_data")
            if (!type.isNullOrEmpty()) {
                NotificationNavigator.pendingTargetType = type
                NotificationNavigator.pendingTargetData = data
                it.removeExtra("target_type") // Limpiamos para no repetirlo
            }
        }

        val sharedPrefs = getSharedPreferences("adrv_prefs", Context.MODE_PRIVATE)

        // Comprobamos permisos y arrancamos la comprobación de fondo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startBackgroundWorker()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startBackgroundWorker()
        }

        setContent {
            val systemTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("is_dark_theme", systemTheme)) }

            val themeController = remember {
                object : ThemeController {
                    override val isDarkTheme: Boolean get() = isDarkTheme
                    override fun toggleTheme() {
                        isDarkTheme = !isDarkTheme
                        sharedPrefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                    }
                }
            }

            CompositionLocalProvider(LocalThemeController provides themeController) {
                ADRVTheme(darkTheme = isDarkTheme) {
                    MainScreen()
                }
            }
        }
    }

    private fun startBackgroundWorker() {
        // Programa la tarea para ejecutarse aprox cada 15 minutos en segundo plano
        val workRequest = PeriodicWorkRequestBuilder<AdrvUpdateWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AdrvUpdates",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}