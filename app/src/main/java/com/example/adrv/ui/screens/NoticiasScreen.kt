package com.example.adrv.ui.screens

import androidx.activity.compose.BackHandler // <--- 1. Importa el BackHandler de Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.adrv.data.RayoRepository
import com.example.adrv.model.News
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NoticiasScreen() {
    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedArticle by remember { mutableStateOf<News?>(null) }
    val scope = rememberCoroutineScope()

    // Estado para el filtro actual
    var selectedSource by remember { mutableStateOf("Todas") }
    val sources = listOf("Todas", "Oficial", "Unión Rayo", "Matagigantes", "Bukaneros")

    // Lista filtrada dinámicamente según el botón pulsado
    val filteredNews = remember(selectedSource, newsList) {
        if (selectedSource == "Todas") {
            newsList
        } else {
            newsList.filter { it.fuente == selectedSource }
        }
    }

    BackHandler(enabled = selectedArticle != null) {
        selectedArticle = null
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val response = RayoRepository.fetchNewsData()
            if (response != null) {
                newsList = response.news
            }
            isLoading = false
        }
    }

    if (selectedArticle != null) {
        NewsDetailScreen(article = selectedArticle!!)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- FILA DE FILTROS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sources.forEach { source ->
                    FilterChip(
                        selected = (source == selectedSource),
                        onClick = { selectedSource = source },
                        label = { Text(source, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            // --- CONTENIDO DE NOTICIAS ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredNews.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay noticias de esta fuente ahora mismo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNews) { article ->
                        NewsCard(
                            article = article,
                            onClick = { selectedArticle = article }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: News, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Imagen destacada
            if (!article.image.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(article.image)
                            .crossfade(true)
                            // Esto ayuda a que no falle al decodificar formatos modernos como AVIF
                            .allowHardware(false)
                            .build(),
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop, // Para que llene la caja sin deformarse
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fila con Fuente y Categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Etiqueta de la Fuente
                Surface(
                    color = when(article.fuente) {
                        "Unión Rayo" -> Color(0xFFF59E0B) // Ámbar
                        "Matagigantes" -> Color(0xFF0369A1) // Azul oceánico
                        "Bukaneros" -> Color(0xFF1E293B)  // Oscuro
                        else -> MaterialTheme.colorScheme.primary // Rojo oficial
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = article.fuente.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Categoría original
                if (article.categories.isNotEmpty()) {
                    Text(
                        text = article.categories.first().uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            Text(
                text = article.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Resumen
            if (!article.summary.isNullOrEmpty()) {
                Text(
                    text = article.summary,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = formatNewsDate(article.published),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// FUNCIÓN HELPER PARA FORMATEAR FECHAS

fun formatNewsDate(dateStr: String): String {
    return try {
        // Analiza el formato del RSS: "Sat, 31 Oct 2026 21:30:00 +0000"
        val parsedDate = java.time.ZonedDateTime.parse(dateStr, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
        parsedDate.format(formatter)
    } catch (e: Exception) {
        // Si hay algún fallo, devuelve los primeros caracteres limpios
        dateStr.take(16)
    }
}