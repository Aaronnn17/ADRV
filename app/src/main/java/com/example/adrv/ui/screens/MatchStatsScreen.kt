package com.example.adrv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.adrv.model.ApiMatch
import com.example.adrv.model.Enfrentamiento
import com.example.adrv.model.EstadisticaTemporada
import com.example.adrv.model.MatchStatsResponse
import com.example.adrv.model.PartidoRacha
import com.example.adrv.ui.theme.RayoRed
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MatchStatsScreen(stats: MatchStatsResponse, matchInfo: ApiMatch) {
    var isAnimStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimStarted = true }

    val (fechaStr, horaStr) = remember(matchInfo.dateText) {
        try {
            val localDateTime = ZonedDateTime.parse(matchInfo.dateText)
                .withZoneSameInstant(ZoneId.of("Europe/Madrid"))
            val fFormat = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "ES"))
            val hFormat = DateTimeFormatter.ofPattern("HH:mm")
            Pair(localDateTime.format(fFormat), localDateTime.format(hFormat))
        } catch (e: Exception) {
            Pair(matchInfo.dateText, "--:--")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. CABECERA DEL PARTIDO ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = matchInfo.competition,
                    color = RayoRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (!matchInfo.round.isNullOrEmpty()) {
                    Text(
                        text = matchInfo.round,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamLogoHeader(matchInfo.homeLogoUrl, matchInfo.homeTeam, modifier = Modifier.weight(1f))
                    Text(
                        text = "VS",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TeamLogoHeader(matchInfo.awayLogoUrl, matchInfo.awayTeam, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Fecha", fechaStr)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        InfoRow("Hora local", horaStr)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        InfoRow("Estadio", matchInfo.venue ?: "Por confirmar")
                    }
                }
            }
        }

        // --- 2. CARA A CARA ---
        item {
            AnimatedVisibility(
                visible = isAnimStarted,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }
            ) {
                StatsCardContainer(title = "Cara a cara") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCircleBox(stats.cara_a_cara.victorias_local, "Victorias Rayo", RayoRed)
                        StatCircleBox(stats.cara_a_cara.empates, "Empates", MaterialTheme.colorScheme.onSurface)
                        StatCircleBox(stats.cara_a_cara.victorias_visitante, "Victorias Rival", MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // --- 3. ENFRENTAMIENTOS RECIENTES ---
        if (stats.enfrentamientos_recientes.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isAnimStarted,
                    enter = fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 4 }
                ) {
                    StatsCardContainer(title = "Enfrentamientos recientes") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            stats.enfrentamientos_recientes.forEach { item ->
                                RecentMatchRow(item)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. RACHA DE LOS ÚLTIMOS 5 PARTIDOS (DETALLADA) ---
        item {
            AnimatedVisibility(
                visible = isAnimStarted,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
            ) {
                StatsCardContainer(title = "Racha en los últimos partidos") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Bloque Local
                        TeamDetailedRachaSection(
                            logoUrl = matchInfo.homeLogoUrl,
                            teamName = matchInfo.homeTeam,
                            rachaList = stats.racha_ultimos_partidos.local
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Bloque Visitante
                        TeamDetailedRachaSection(
                            logoUrl = matchInfo.awayLogoUrl,
                            teamName = matchInfo.awayTeam,
                            rachaList = stats.racha_ultimos_partidos.visitante
                        )
                    }
                }
            }
        }

        // --- 5. ESTADÍSTICAS DE TEMPORADA ---
        if (stats.estadisticas_temporada.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isAnimStarted,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 4 }
                ) {
                    Text(
                        text = "Estadísticas de la temporada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                    )
                }
            }
            items(stats.estadisticas_temporada) { stat ->
                StatBarAnimated(stat = stat, isAnimStarted = isAnimStarted)
            }
        }
    }
}

// ==================== COMPONENTES VISUALES ====================

@Composable
fun TeamLogoHeader(logoUrl: String?, teamName: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (!logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = teamName,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = teamName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@Composable
fun StatsCardContainer(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun StatCircleBox(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                color = color,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentMatchRow(item: Enfrentamiento) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.fecha,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(75.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.local,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = item.resultado,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = item.visitante,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TeamDetailedRachaSection(logoUrl: String?, teamName: String, rachaList: List<PartidoRacha>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = teamName,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = teamName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (rachaList.isEmpty()) {
            Text(
                text = "No hay datos de racha disponibles",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rachaList.forEach { partido ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = partido.fecha,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(65.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = partido.local,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = " ${partido.resultado} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = partido.visitante,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ResultBadge(partido.estado)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultBadge(estado: String) {
    val color = when (estado.uppercase()) {
        "V" -> Color(0xFF4CAF50)
        "E" -> Color(0xFFFFB300)
        "D" -> Color(0xFFE53935)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
    ) {
        Text(
            text = estado,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatBarAnimated(stat: EstadisticaTemporada, isAnimStarted: Boolean) {
    val homeVal = stat.local.replace("%", "").replace(",", "").toFloatOrNull() ?: 0f
    val awayVal = stat.visitante.replace("%", "").replace(",", "").toFloatOrNull() ?: 0f
    val total = homeVal + awayVal
    val targetWeight = if (total > 0) homeVal / total else 0.5f

    val animatedWeight by animateFloatAsState(
        targetValue = if (isAnimStarted) targetWeight else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "BarAnimation"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stat.local, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RayoRed)
            Text(text = stat.estadistica, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stat.visitante, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (animatedWeight > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedWeight)
                        .background(RayoRed)
                )
            }
        }
    }
}