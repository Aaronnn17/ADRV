package com.example.adrv.ui.screens

import androidx.activity.compose.BackHandler // <--- 1. Importa el BackHandler de Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

    // 2. Interceptamos el botón atrás nativo si estamos viendo una noticia en detalle
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
        NewsDetailScreen(
            article = selectedArticle!!
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (newsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay noticias disponibles en este momento",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(newsList) { article ->
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
            .clickable { onClick() }, // <--- Hacemos la tarjeta interactiva
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        model = article.image,
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Categoría
            if (article.categories.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = article.categories.first().uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

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

            // Fecha elegante y limpia en castellano
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