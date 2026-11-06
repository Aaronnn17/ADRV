package com.example.adrv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.example.adrv.data.RayoRepository
import com.example.adrv.model.ApiMatch
import com.example.adrv.model.RayoData
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarioScreen() {
    var rayoData by remember { mutableStateOf<RayoData?>(null) }
    var selectedMatch by remember { mutableStateOf<ApiMatch?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now(ZoneId.of("Europe/Madrid"))) }

    // AÑADIMOS EL ESTADO DE SCROLL
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        rayoData = RayoRepository.fetchRayoData()
    }

    val matchesByDate = remember(rayoData) {
        rayoData?.partidos?.associateBy { match ->
            try {
                Instant.parse(match.dateText).atZone(ZoneId.of("Europe/Madrid")).toLocalDate()
            } catch (e: Exception) {
                LocalDate.MIN
            }
        } ?: emptyMap()
    }

    // AÑADIMOS .verticalScroll(scrollState) A LA COLUMNA PRINCIPAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (rayoData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // CABECERA MES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }

                val nombreMes = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                Text(
                    text = "${nombreMes.replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mes siguiente")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DÍAS SEMANA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                    Text(
                        text = day,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CALENDARIO
            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value
            val daysInMonth = currentMonth.lengthOfMonth()
            var currentDay = 1

            Column(modifier = Modifier.fillMaxWidth()) {
                for (week in 0..5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dayOfWeek in 1..7) {
                            if ((week == 0 && dayOfWeek < firstDayOfWeek) || currentDay > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val date = currentMonth.atDay(currentDay)
                                val matchOnThisDay = matchesByDate[date]
                                val isSelected = selectedMatch != null && matchOnThisDay?.id == selectedMatch?.id

                                DayCell(
                                    dayNumber = currentDay,
                                    match = matchOnThisDay,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (matchOnThisDay != null) {
                                            selectedMatch = matchOnThisDay
                                        }
                                    }
                                )
                                currentDay++
                            }
                        }
                    }
                    if (currentDay > daysInMonth) break
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // PANEL DETALLES (Se expandirá hacia abajo gracias al scroll)
            selectedMatch?.let { match ->
                MatchDetailsPanel(match)
                // Espacio extra al final para asegurar que se pueda hacer scroll cómodamente hasta abajo
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DayCell(
    dayNumber: Int,
    match: ApiMatch?,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(enabled = match != null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontWeight = if (match != null) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp
            )

            if (match != null) {
                Spacer(modifier = Modifier.height(2.dp))
                val rivalLogo = if (match.homeTeam.contains("Rayo")) match.awayLogoUrl else match.homeLogoUrl
                AsyncImage(
                    model = rivalLogo,
                    contentDescription = "Partido",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MatchDetailsPanel(match: ApiMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera
            Text(text = match.competition, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(text = match.round, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Equipos y Marcador con proporciones fijas para que no se pisen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Local (30%)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.3f)
                ) {
                    AsyncImage(model = match.homeLogoUrl, contentDescription = match.homeTeam, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.homeTeam,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        minLines = 2, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Centro (40%) - Marcador ancho
                Box(
                    modifier = Modifier.weight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    if (match.status == "FINISHED" && match.homeScore != null && match.awayScore != null) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "VS",
                            color = Color.Gray,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Visitante (30%)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.3f)
                ) {
                    AsyncImage(model = match.awayLogoUrl, contentDescription = match.awayTeam, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.awayTeam,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        minLines = 2, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // LISTA DE DATOS DETALLADOS
            MatchInfoRow(label = "Competición", value = match.competition)
            MatchInfoRow(label = "Jornada", value = match.round)
            MatchInfoRow(label = "Fecha", value = formatSpanishDateOnly(match.dateText))
            MatchInfoRow(label = "Hora local", value = formatSpanishTimeOnly(match.dateText))

            if (!match.venue.isNullOrEmpty()) {
                MatchInfoRow(label = "Estadio", value = match.venue)
            }

            val estadoTraducido = if (match.status == "FINISHED") "Finalizado" else "Por disputar"
            MatchInfoRow(label = "Estado", value = estadoTraducido)
        }
    }
}

@Composable
fun MatchInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}

fun formatSpanishDateOnly(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val zonedDateTime = instant.atZone(ZoneId.of("Europe/Madrid"))
        zonedDateTime.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "ES")))
    } catch (e: Exception) {
        dateString
    }
}

fun formatSpanishTimeOnly(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val zonedDateTime = instant.atZone(ZoneId.of("Europe/Madrid"))
        zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        "--:--"
    }
}