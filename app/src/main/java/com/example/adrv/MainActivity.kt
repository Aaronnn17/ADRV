package com.example.adrv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import com.example.adrv.ui.screens.MainScreen
import com.example.adrv.ui.theme.ADRVTheme

// Creamos un controlador global accesible desde cualquier pantalla
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}

interface ThemeController {
    val isDarkTheme: Boolean
    fun toggleTheme()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Usamos SharedPreferences para recordar la elección del usuario
        val sharedPrefs = getSharedPreferences("adrv_prefs", Context.MODE_PRIVATE)

        setContent {
            // Por defecto mira el tema del sistema, a menos que el usuario lo haya cambiado antes
            val systemTheme = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(sharedPrefs.getBoolean("is_dark_theme", systemTheme))
            }

            val themeController = remember {
                object : ThemeController {
                    override val isDarkTheme: Boolean get() = isDarkTheme

                    override fun toggleTheme() {
                        isDarkTheme = !isDarkTheme
                        sharedPrefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                    }
                }
            }

            // Inyectamos el controlador globalmente
            CompositionLocalProvider(LocalThemeController provides themeController) {
                // Pasamos el booleano a tu tema (asegúrate de que ADRVTheme tiene el parámetro darkTheme)
                ADRVTheme(darkTheme = isDarkTheme) {
                    MainScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppPreview() {
    ADRVTheme {
        MainScreen()
    }
}