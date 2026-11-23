package com.example.adrv.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adrv.R
import com.example.adrv.data.RayoRepository
import com.example.adrv.model.News
import com.example.adrv.ui.components.bounceClick
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.example.adrv.ui.components.OdometerText
import com.example.adrv.ui.components.jellyClick

@Composable
fun NoticiasScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("adrv_prefs", Context.MODE_PRIVATE) }

    var allNews by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSource by remember { mutableStateOf("Todos") }

    var selectedNewsUrl by remember { mutableStateOf<String?>(null) }
    var selectedNewsTitle by remember { mutableStateOf("") }

    // Estado reactivo para que los rayos rojos desaparezcan al instante
    var readNewsIds by remember {
        mutableStateOf(prefs.getStringSet("read_news_ids", emptySet()) ?: emptySet())
    }

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

    val unreadCount = allNews.count { !readNewsIds.contains(it.id) }
    val sources = listOf("Todos", "Rayo Vallecano", "Unión Rayo", "Bukaneros", "Matagigantes")

    AnimatedContent(
        targetState = selectedNewsUrl,
        transitionSpec = {
            if (targetState != null) {
                slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn() togetherWith fadeOut()
            } else {
                fadeIn() togetherWith slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
            }
        },
        label = "TransicionNoticias"
    ) { currentUrl ->
        if (currentUrl != null) {
            WebViewScreen(
                url = currentUrl,
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
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // Filtros horizontales con efecto gelatina (Jelly Pad)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sources.forEach { source ->
                                    val isSelected = (selectedSource == source)
                                    val bgColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        label = "chipBg_$source"
                                    )
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        label = "chipText_$source"
                                    )

                                    Surface(
                                        color = bgColor,
                                        shape = RoundedCornerShape(50.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .jellyClick { selectedSource = source }
                                    ) {
                                        Text(
                                            text = source,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Botón para marcar todas como leídas si hay alguna sin leer
                        if (unreadCount > 0) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OdometerText(
                                        text = unreadCount.toString(),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " sin leer",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextButton(
                                        onClick = {
                                            val updated = readNewsIds.toMutableSet().apply {
                                                allNews.forEach { add(it.id) }
                                            }
                                            readNewsIds = updated
                                            prefs.edit().putStringSet("read_news_ids", updated).apply()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Marcar todas como leídas",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
                                val isUnread = !readNewsIds.contains(item.id)
                                ModernNewsCard(
                                    item = item,
                                    isUnread = isUnread,
                                    onClick = {
                                        val updated = readNewsIds.toMutableSet().apply { add(item.id) }
                                        readNewsIds = updated
                                        prefs.edit().putStringSet("read_news_ids", updated).apply()

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
}

@Composable
fun ModernNewsCard(item: News, isUnread: Boolean, onClick: () -> Unit) {
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
                    if (isUnread) {
                        val pulseTransition = rememberInfiniteTransition(label = "UnreadBolt")
                        val boltScale by pulseTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(750),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "BoltScale"
                        )
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "No leída",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(16.dp)
                                .scale(boltScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

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
        var anio = "2026"

        val numMatch = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(cleanStr)
        if (numMatch != null) {
            val (y, m, d) = numMatch.destructured
            val mesesArr = arrayOf("", "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
            val mesNombre = mesesArr.getOrNull(m.toInt()) ?: m
            return "${d.toInt()} de $mesNombre, $y"
        }

        for ((eng, esp) in mesesMap) {
            if (cleanStr.contains(eng)) {
                mesText = esp
                break
            }
        }

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