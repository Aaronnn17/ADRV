package com.example.adrv.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adrv.ui.theme.RayoRed

// 1. CAMBIAMOS EL NOMBRE AQUÍ A "Equipo"
sealed class NavTab(val title: String, val icon: ImageVector) {
    object Hoy : NavTab("Hoy", Icons.Default.Home)
    object Noticias : NavTab("Noticias", Icons.Default.Newspaper)
    object Equipo : NavTab("Equipo", Icons.Default.CalendarMonth) // <--- Cambio aquí
    object Abono : NavTab("Abono", Icons.Default.CreditCard)
}

@Composable
fun MainScreen() {
    // 2. ACTUALIZAMOS LA LISTA DE TABS
    val tabs = listOf(NavTab.Hoy, NavTab.Noticias, NavTab.Equipo, NavTab.Abono) // <--- Cambio aquí
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RayoRed,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = RayoRed,
                            indicatorColor = RayoRed,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> HoyScreen()
                1 -> NoticiasScreen()
                2 -> EquipoScreen() // 3. LLAMAMOS A LA NUEVA PANTALLA AQUÍ
                3 -> AbonoScreen()
            }
        }
    }
}