package com.example.adrv.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.adrv.data.RayoRepository
import com.example.adrv.model.MatchEvent
import com.example.adrv.model.RayoData
import com.example.adrv.ui.components.RayoHeader
import com.example.adrv.ui.components.bounceClick
import com.example.adrv.ui.theme.GoldAccent
import com.example.adrv.ui.theme.RayoRed
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HoyScreen() {
    var rayoData by remember { mutableStateOf<RayoData?>(null) }
    var newsData by remember { mutableStateOf<List<com.example.adrv.model.News>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // VARIABLES PARA CONTROLAR LOS DIÁLOGOS Y VISOR WEB
    var selectedNewsUrl by remember { mutableStateOf<String?>(null) }
    var selectedNewsTitle by remember { mutableStateOf("") }
    var selectedMatchForDialog by remember { mutableStateOf<com.example.adrv.model.ApiMatch?>(null) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("RayoAlerts", Context.MODE_PRIVATE) }

    var dynamicNotifications by remember { mutableStateOf<List<String>>(emptyList()) }
    val unreadCount = dynamicNotifications.size

    var novedadesHistorial by remember { mutableStateOf<List<String>>(emptyList()) }

    val today = LocalDate.now(ZoneId.of("Europe/Madrid"))
    val scope = rememberCoroutineScope()

    val refreshState = rememberPullToRefreshState()

    fun refreshData() {
        scope.launch {
            isRefreshing = true
            rayoData = RayoRepository.fetchRayoData()
            newsData = RayoRepository.fetchNewsData()?.news
            novedadesHistorial = sharedPrefs.getStringSet("history_alerts", emptySet())?.toList() ?: emptyList()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            rayoData = RayoRepository.fetchRayoData()
            newsData = RayoRepository.fetchNewsData()?.news
            novedadesHistorial = sharedPrefs.getStringSet("history_alerts", emptySet())?.toList() ?: emptyList()
            isLoading = false
        }
    }

    val nextMatch = remember(rayoData) {
        rayoData?.partidos?.firstOrNull { match ->
            try {
                val matchDate = ZonedDateTime.parse(match.dateText)
                    .withZoneSameInstant(ZoneId.of("Europe/Madrid"))
                    .toLocalDate()
                !matchDate.isBefore(today)
            } catch (e: Exception) {
                false
            }
        } ?: rayoData?.partidos?.lastOrNull()
    }

    LaunchedEffect(rayoData, newsData) {
        if (rayoData != null) {
            val newAlerts = mutableListOf<String>()
            val storedAlerts = sharedPrefs.getStringSet("history_alerts", mutableSetOf())?.toMutableList() ?: mutableListOf()

            val upcomingMatches = rayoData?.partidos?.filter { match ->
                try {
                    !ZonedDateTime.parse(match.dateText)
                        .withZoneSameInstant(ZoneId.of("Europe/Madrid"))
                        .toLocalDate()
                        .isBefore(today)
                } catch (e: Exception) { false }
            }?.take(5) ?: emptyList()

            upcomingMatches.forEach { match ->
                val savedMatchDate = sharedPrefs.getString("last_match_date_${match.id}", "") ?: ""

                if (savedMatchDate.isNotEmpty() && savedMatchDate != match.dateText) {
                    val oldTime = formatTimeForComparison(savedMatchDate)
                    val newTime = formatTimeForComparison(match.dateText)
                    val newDateStr = formatDateForDisplay(match.dateText)

                    val rival = if (match.homeTeam.contains("Rayo", ignoreCase = true)) match.awayTeam else match.homeTeam

                    val alertMsg = if ((oldTime == "00:00" || oldTime == "01:00") && (newTime != "00:00" && newTime != "01:00")) {
                        "MATCH|${match.id}|¡Horario Confirmado! El partido contra $rival será el $newDateStr a las $newTime."
                    } else {
                        "MATCH|${match.id}|Cambio de horario: El partido contra $rival se ha movido al $newDateStr a las $newTime."
                    }

                    newAlerts.add(alertMsg)
                    storedAlerts.add(0, alertMsg)
                }

                sharedPrefs.edit().putString("last_match_date_${match.id}", match.dateText).apply()
            }

            val latestNews = newsData?.firstOrNull()
            if (latestNews != null) {
                val savedNewsTitle = sharedPrefs.getString("last_news_title", "") ?: ""

                if (savedNewsTitle.isNotEmpty() && savedNewsTitle != latestNews.title) {
                    val alertMsg = "NEWS|${latestNews.id}|🗞️ Nueva noticia: ${latestNews.title}"
                    newAlerts.add(alertMsg)
                    storedAlerts.add(0, alertMsg)
                }

                sharedPrefs.edit().putString("last_news_title", latestNews.title).apply()
            }

            if (newAlerts.isNotEmpty()) {
                dynamicNotifications = newAlerts
                val trimmedHistory = storedAlerts.take(5).toSet()
                sharedPrefs.edit().putStringSet("history_alerts", trimmedHistory).apply()
                novedadesHistorial = trimmedHistory.toList()
            }
        }
    }

    // TRANSICIÓN ANIMADA ENTRE PORTADA Y VISOR WEB
    AnimatedContent(
        targetState = selectedNewsUrl,
        transitionSpec = {
            if (targetState != null) {
                // Al entrar a la noticia: sube desde abajo y aparece
                slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn() togetherWith
                        fadeOut()
            } else {
                // Al cerrar la noticia: baja hacia abajo y desaparece
                fadeIn() togetherWith
                        slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
            }
        },
        label = "NavegacionNoticias"
    ) { currentUrl ->
        if (currentUrl != null) {
            // MOSTRAMOS EL VISOR WEB ANIMADO
            WebViewScreen(
                url = currentUrl,
                title = selectedNewsTitle,
                onBackClick = {
                    selectedNewsUrl = null
                    selectedNewsTitle = ""
                }
            )
        } else {
            // MOSTRAMOS LA PORTADA ORIGINAL
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { refreshData() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RayoHeader(
                            onNotificationClick = {
                                if (unreadCount > 0) showNotificationsDialog = true
                            },
                            unreadCount = unreadCount
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (nextMatch != null) {
                            NextMatchCard(match = nextMatch)

                            if (novedadesHistorial.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Últimas Novedades",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                    )

                                    novedadesHistorial.forEach { novedad ->
                                        val parts = novedad.split("|", limit = 3)
                                        val type = parts.getOrNull(0) ?: ""
                                        val id = parts.getOrNull(1) ?: ""
                                        val text = parts.getOrNull(2) ?: novedad

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .bounceClick {
                                                    if (type == "NEWS") {
                                                        val newsItem = newsData?.find { it.id == id }
                                                        if (newsItem != null && newsItem.link.isNotEmpty()) {
                                                            selectedNewsUrl = newsItem.link
                                                            selectedNewsTitle = newsItem.title
                                                        }
                                                    } else if (type == "MATCH") {
                                                        selectedMatchForDialog = rayoData?.partidos?.find { it.id.toString() == id }
                                                    } else {
                                                        if (text.contains("noticia", ignoreCase = true)) {
                                                            val titulo = text.substringAfter(":").trim()
                                                            val newsItem = newsData?.find { it.title.contains(titulo, ignoreCase = true) }
                                                            if (newsItem != null && newsItem.link.isNotEmpty()) {
                                                                selectedNewsUrl = newsItem.link
                                                                selectedNewsTitle = newsItem.title
                                                            }
                                                        }
                                                    }
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = RayoRed, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("No hay datos de partidos disponibles", color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (showNotificationsDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotificationsDialog = false },
                            title = {
                                Text(text = "🔔 Novedades ADRV", fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    dynamicNotifications.forEach { notification ->
                                        val textToShow = notification.split("|", limit = 3).getOrNull(2) ?: notification
                                        Text(text = "• $textToShow")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showNotificationsDialog = false
                                    dynamicNotifications = emptyList()
                                }) {
                                    Text(text = "Entendido", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Diálogo flotante transparente para el partido
    if (selectedMatchForDialog != null) {
        AlertDialog(
            onDismissRequest = { selectedMatchForDialog = null },
            confirmButton = {
                TextButton(onClick = { selectedMatchForDialog = null }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                NextMatchCard(match = selectedMatchForDialog!!)
            },
            containerColor = Color.Transparent
        )
    }
}

// --- Funciones auxiliares para formatear las fechas ---
fun formatTimeForComparison(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val zonedDateTime = instant.atZone(ZoneId.of("Europe/Madrid"))
        zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        "--:--"
    }
}

fun formatDateForDisplay(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val zonedDateTime = instant.atZone(ZoneId.of("Europe/Madrid"))
        zonedDateTime.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES")))
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun MatchEventRow(event: MatchEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (event.type) {
                        "Gol" -> Icons.Default.CheckCircle
                        "Tarjeta Amarilla", "Tarjeta Roja" -> Icons.Default.Warning
                        else -> Icons.Default.Star
                    }
                    val iconColor = when (event.type) {
                        "Gol" -> RayoRed
                        "Tarjeta Amarilla" -> GoldAccent
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RayoRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = event.minute,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = event.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatRow(label: String, homeValue: String, awayValue: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = homeValue, fontWeight = FontWeight.Bold, color = RayoRed)
            Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = awayValue, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.54f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(RayoRed)
            )
        }
    }
}

@Composable
fun NextMatchCard(match: com.example.adrv.model.ApiMatch) {
    val formattedDateTime = remember(match.dateText) {
        try {
            val utcDateTime = ZonedDateTime.parse(match.dateText)
            val madridZone = ZoneId.of("Europe/Madrid")
            val localDateTime = utcDateTime.withZoneSameInstant(madridZone)

            val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM · HH:mm", Locale("es", "ES"))
            localDateTime.format(formatter).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            match.dateText
        }
    }

    // ANIMACIÓN DE PULSO CONTINUO
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f, // Crece un 6% suavemente
        animationSpec = infiniteRepeatable(
            animation = tween(1000), // 1 segundo por ciclo
            repeatMode = RepeatMode.Reverse // Va y viene
        ),
        label = "PulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .bounceClick { },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = match.competition,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (!match.round.isNullOrEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = match.round,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // APLICAMOS EL PULSO AQUÍ: El texto "Próximo partido" respirará suavemente
            Text(
                text = "Próximo partido",
                fontSize = 13.sp,
                color = RayoRed, // Lo ponemos en rojo corporativo para que resalte más con el pulso
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scalePulse)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!match.homeLogoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = match.homeLogoUrl,
                            contentDescription = match.homeTeam,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = match.homeTeam,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Text(
                    text = "VS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!match.awayLogoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = match.awayLogoUrl,
                            contentDescription = match.awayTeam,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = match.awayTeam,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = formattedDateTime,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (!match.venue.isNullOrEmpty() && match.venue != "Estadio por confirmar") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🏟️ ${match.venue}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}