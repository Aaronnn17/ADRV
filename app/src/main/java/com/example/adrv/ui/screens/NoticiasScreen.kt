package com.example.adrv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adrv.R
import com.example.adrv.data.RayoRepository
import com.example.adrv.model.News
import com.example.adrv.ui.components.bounceClick

@Composable
fun NoticiasScreen() {
    var allNews by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSource by remember { mutableStateOf("Todos") }

    var selectedNewsUrl by remember { mutableStateOf<String?>(null) }
    var selectedNewsTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val response = RayoRepository.fetchNewsData()
            if (response != null) {
                allNews = response.news.map { news ->
                    val sourceClean = if (news.fuente.equals("Oficial", ignoreCase = true)) "Rayo Vallecano" else news.fuente
                    news.copy(fuente = sourceClean)
                }
            }
        } catch (e: Exception) {
            allNews = emptyList()
        } finally {
            isLoading = false
        }
    }

    val filteredNews = if (selectedSource == "Todos") {
        allNews
    } else {
        allNews.filter { it.fuente.equals(selectedSource, ignoreCase = true) }
    }

    val sources = listOf("Todos", "Rayo Vallecano", "Unión Rayo", "Bukaneros", "Matagigantes")

    if (selectedNewsUrl != null) {
        WebViewScreen(
            url = selectedNewsUrl!!,
            title = selectedNewsTitle,
            onBackClick = {
                selectedNewsUrl = null
                selectedNewsTitle = ""
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Filtros horizontales
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sources.forEach { source ->
                                val isSelected = (selectedSource == source)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSource = source },
                                    label = { Text(text = source, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(50.dp)
                                )
                            }
                        }
                    }

                    if (filteredNews.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "No hay noticias para este medio", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(filteredNews) { item ->
                            ModernNewsCard(
                                item = item,
                                onClick = {
                                    if (item.link.isNotEmpty()) {
                                        selectedNewsUrl = item.link
                                        selectedNewsTitle = item.title
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun ModernNewsCard(item: News, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val logoRes = when (item.fuente.lowercase()) {
                        "rayo vallecano", "oficial" -> R.drawable.logo_oficial
                        "unión rayo", "union rayo" -> R.drawable.logo_unionrayo
                        "bukaneros" -> R.drawable.logo_bukaneros
                        "matagigantes" -> R.drawable.logo_matagigantes
                        else -> R.drawable.logo_oficial
                    }

                    val fuenteLower = item.fuente.lowercase()
                    val esLogoAncho = fuenteLower.contains("union") || fuenteLower.contains("unión") || fuenteLower.contains("matagigantes")

                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .widthIn(min = 22.dp, max = if (esLogoAncho) 90.dp else 26.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = item.fuente,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    if (!fuenteLower.contains("union") && !fuenteLower.contains("unión") && !fuenteLower.contains("matagigantes")) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.fuente,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = getSourceColor(item.fuente)
                        )
                    }
                }

                // Fecha traducida infaliblemente al castellano
                Text(
                    text = formatSpanishNewsDate(item.published),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!item.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.summary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Formateador robusto corregido para extraer correctamente el año
fun formatSpanishNewsDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    return try {
        val cleanStr = dateString.lowercase()

        val mesesMap = mapOf(
            "jan" to "enero", "feb" to "febrero", "mar" to "marzo",
            "apr" to "abril", "may" to "mayo", "jun" to "junio",
            "jul" to "julio", "aug" to "agosto", "sep" to "septiembre",
            "oct" to "octubre", "nov" to "noviembre", "dec" to "diciembre"
        )

        var dia = ""
        var mesText = ""
        var anio = "2026" // Año por defecto seguro

        // 1. Buscar patrón numérico estándar YYYY-MM-DD
        val numMatch = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(cleanStr)
        if (numMatch != null) {
            val (y, m, d) = numMatch.destructured
            val mesesArr = arrayOf("", "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
            val mesNombre = mesesArr.getOrNull(m.toInt()) ?: m
            return "${d.toInt()} de $mesNombre, $y"
        }

        // 2. Buscar meses en texto (inglés)
        for ((eng, esp) in mesesMap) {
            if (cleanStr.contains(eng)) {
                mesText = esp
                break
            }
        }

        // 3. Extraer números para encontrar el día y el año de 4 cifras
        val numbers = Regex("\\d+").findAll(cleanStr).map { it.value }.toList()
        if (numbers.isNotEmpty()) {
            for (num in numbers) {
                if (num.length == 4 && num.startsWith("20")) {
                    anio = num
                } else if (num.toInt() in 1..31 && dia.isEmpty()) {
                    dia = num
                }
            }
        }

        if (dia.isNotEmpty() && mesText.isNotEmpty()) {
            "$dia de $mesText, $anio"
        } else {
            dateString.take(10)
        }
    } catch (e: Exception) {
        dateString.take(10)
    }
}

@Composable
fun getSourceColor(fuente: String): Color {
    return when (fuente.lowercase()) {
        "rayo vallecano" -> Color(0xFFD32F2F)
        "bukaneros" -> Color(0xFF424242)
        else -> Color(0xFFE53935)
    }
}