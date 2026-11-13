package com.example.adrv.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.layout.ContentScale
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
import com.example.adrv.model.ApiSquadData
import com.example.adrv.model.ApiStanding
import com.example.adrv.model.ApiStandingsData
import com.example.adrv.model.RayoData
import com.example.adrv.model.ApiPlayerDetail
import com.example.adrv.ui.components.bounceClick
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EquipoScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calendario", "Clasificación", "Plantilla")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ----------------------------------------------------
        // PESTAÑAS SUPERIORES CON TRANSICIÓN FLUIDA Y SUAVE
        // ----------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTabIndex == index

                    val backgroundColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tabBg"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "tabText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            .clip(RoundedCornerShape(50.dp))
                            .background(backgroundColor)
                            .bounceClick { selectedTabIndex = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // CONTENIDO SEGÚN PESTAÑA CON ANIMACIÓN DE TRANSICIÓN
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> CalendarioView()
                    1 -> ClasificacionView()
                    2 -> PlantillaView()
                }
            }
        }
    }
}

// ----------------------------------------------------
// VISTA 1: CALENDARIO
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioView() {
    var rayoData by remember { mutableStateOf<RayoData?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now(ZoneId.of("Europe/Madrid"))) }

    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMatchForModal by remember { mutableStateOf<ApiMatch?>(null) }

    LaunchedEffect(Unit) {
        rayoData = RayoRepository.fetchRayoData()
    }

    val matchesByDate = remember(rayoData) {
        rayoData?.partidos?.associateBy { match ->
            try {
                Instant.parse(match.dateText).atZone(ZoneId.of("Europe/Madrid")).toLocalDate()
            } catch (e: Exception) {
                try {
                    LocalDate.parse(match.dateText.substring(0, 10))
                } catch (ex: Exception) {
                    LocalDate.MIN
                }
            }
        } ?: emptyMap()
    }

    val today = LocalDate.now(ZoneId.of("Europe/Madrid"))
    var selectedDate by remember { mutableStateOf(today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (rayoData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // CABECERA MES CON NAVEGACIÓN ANIMADA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }

                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "MonthAnimation"
                ) { month ->
                    val mName = month.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                    Text(
                        text = "${mName.replaceFirstChar { it.uppercase() }} ${month.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                                val isSelected = (date == selectedDate)

                                DayCell(
                                    dayNumber = currentDay,
                                    match = matchOnThisDay,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedDate = date
                                        if (matchOnThisDay != null) {
                                            selectedMatchForModal = matchOnThisDay
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

            // LEYENDA DEL CALENDARIO (AÑADIDO)
            Spacer(modifier = Modifier.height(16.dp))
            CalendarLegend()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // MODAL BOTTOM SHEET PARA EL DETALLE DEL PARTIDO
    selectedMatchForModal?.let { match ->
        ModalBottomSheet(
            onDismissRequest = { selectedMatchForModal = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MatchDetailsPanelContent(match = match)
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
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (match != null) {
            val isHome = match.homeTeam.contains("Rayo", ignoreCase = true)
            val rivalLogo = if (isHome) match.awayLogoUrl else match.homeLogoUrl

            val matchTime = formatSpanishTimeOnly(match.dateText)
            val isTBD = matchTime == "00:00" || matchTime == "01:00"

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(model = rivalLogo, contentDescription = "Partido", modifier = Modifier.size(32.dp))

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isTBD) Color.Gray.copy(alpha = 0.8f) else Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHome) Icons.Default.Home else Icons.Default.Flight,
                        contentDescription = null,
                        tint = if (isTBD) Color.White.copy(alpha = 0.6f) else Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        } else {
            Text(
                text = dayNumber.toString(),
                fontWeight = FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp
            )
        }
    }
}

// COMPONENTES LEYENDA CALENDARIO (AÑADIDOS)
@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalendarLegendItem(icon = Icons.Default.Home, bgColor = Color(0xFF0F172A), iconTint = Color.White, text = "Local")
        CalendarLegendItem(icon = Icons.Default.Flight, bgColor = Color(0xFF0F172A), iconTint = Color.White, text = "Visitante")
        // Utilizamos el icono de casa genérico pero en gris para los "Sin confirmar", como en tus DayCells
        CalendarLegendItem(icon = Icons.Default.Home, bgColor = Color.Gray.copy(alpha = 0.8f), iconTint = Color.White.copy(alpha = 0.6f), text = "Sin confirmar")
    }
}

@Composable
fun CalendarLegendItem(icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, iconTint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(10.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun MatchDetailsPanelContent(match: ApiMatch) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = match.competition, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = match.round, fontWeight = FontWeight.Black, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.35f)) {
                AsyncImage(model = match.homeLogoUrl, contentDescription = match.homeTeam, modifier = Modifier.size(65.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = match.homeTeam, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Box(modifier = Modifier.weight(0.3f), contentAlignment = Alignment.Center) {
                if (match.status == "FINISHED" && match.homeScore != null && match.awayScore != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}", // CORREGIDO
                            fontSize = 32.sp, // Un poco más pequeño para que quepa bien en una línea
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(text = "Final", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text(text = "VS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.5f))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.35f)) {
                AsyncImage(model = match.awayLogoUrl, contentDescription = match.awayTeam, modifier = Modifier.size(65.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = match.awayTeam, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MatchInfoRow(label = "Fecha", value = formatSpanishDateOnly(match.dateText))
                MatchInfoRow(label = "Hora local", value = formatSpanishTimeOnly(match.dateText))

                if (!match.venue.isNullOrEmpty() && match.venue != "Estadio Visitante") {
                    MatchInfoRow(label = "Estadio", value = match.venue!!)
                }
                if (!match.referee.isNullOrEmpty() && match.referee != "Por designar") {
                    MatchInfoRow(label = "Árbitro", value = match.referee!!)
                }

                val estadoTraducido = when (match.statusApi) {
                    "FINISHED", "AWARDED" -> "Finalizado"
                    "IN_PLAY", "PAUSED" -> "En juego"
                    "POSTPONED", "SUSPENDED" -> "Aplazado"
                    "CANCELLED" -> "Cancelado"
                    else -> "Por disputar"
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                MatchInfoRow(label = "Estado", value = estadoTraducido)
            }
        }
    }
}

@Composable
fun MatchInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End)
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

// ----------------------------------------------------
// VISTA 2: CLASIFICACIÓN (DISEÑO UEFA / DESCENSO)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClasificacionView() {
    var standingsData by remember { mutableStateOf<ApiStandingsData?>(null) }
    var selectedTeam by remember { mutableStateOf<ApiStanding?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        standingsData = RayoRepository.fetchStandingsData()
    }

    if (standingsData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // CABECERA TABLA
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.12f))
                        Text("Club", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.38f))
                        Text("Pts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(0.11f))
                        Text("PJ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(0.08f))
                        Text("V", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(0.08f))
                        Text("E", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(0.08f))
                        Text("D", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(0.08f))
                        Text("DG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(0.11f))
                    }
                }

                // FILAS DE EQUIPOS
                items(standingsData!!.standings) { team ->
                    val isRayo = team.teamName.contains("Rayo", ignoreCase = true)
                    val rowBgColor = if (isRayo) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    val zoneColor = getStandingsZoneColor(team.position)
                    val goalDiffStr = if (team.goalDifference > 0) "+${team.goalDifference}" else "${team.goalDifference}"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { selectedTeam = team },
                        colors = CardDefaults.cardColors(containerColor = rowBgColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(zoneColor))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${team.position}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.12f))

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.38f)) {
                                    AsyncImage(model = team.crest, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 6.dp))
                                    Text(
                                        text = team.shortName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isRayo) FontWeight.ExtraBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(text = "${team.points}", fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(0.11f))
                                Text(
                                    text = "${team.playedGames}",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.08f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // <- ¡Cambiado!
                                )
                                Text(
                                    text = "${team.won}",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.08f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // <- ¡Cambiado!
                                )
                                Text(
                                    text = "${team.draw}",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.08f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // <- ¡Cambiado!
                                )
                                Text(
                                    text = "${team.lost}",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.08f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // <- ¡Cambiado!
                                )
                                Text(text = goalDiffStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.11f))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    StandingsLegendFooter()
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // MODAL BOTTOM SHEET (ESTÉTICO Y FLUIDO)
            selectedTeam?.let { team ->
                ModalBottomSheet(
                    onDismissRequest = { selectedTeam = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(model = team.crest, contentDescription = null, modifier = Modifier.size(70.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = team.teamName, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(text = "${team.position}º Clasificado", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TeamDetailRow(label = "Puntos", value = "${team.points}")
                                TeamDetailRow(label = "Partidos Jugados", value = "${team.playedGames}")
                                TeamDetailRow(label = "Victorias", value = "${team.won}")
                                TeamDetailRow(label = "Empates", value = "${team.draw}")
                                TeamDetailRow(label = "Derrotas", value = "${team.lost}")
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                TeamDetailRow(label = "Goles a Favor", value = "${team.goalsFor}")
                                TeamDetailRow(label = "Goles en Contra", value = "${team.goalsAgainst}")
                                TeamDetailRow(label = "Diferencia de Goles", value = if (team.goalDifference > 0) "+${team.goalDifference}" else "${team.goalDifference}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getStandingsZoneColor(position: Int): Color = when (position) {
    in 1..4 -> Color(0xFF1E3A8A)   // Champions League (1º a 4º - Azul oscuro)
    5 -> Color(0xFF1E3A8A)         // Champions League (5º puesto - Mismo tono azul oscuro)
    6 -> Color(0xFF0284C7)         // Europa League (6º puesto - Azul claro)
    7 -> Color(0xFFEA580C)         // Conference League (7º puesto - Naranja)
    in 18..20 -> Color(0xFFDC2626) // Descenso (Rojo)
    else -> Color.Transparent
}

@Composable
fun StandingsLegendFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = Color(0xFF1E3A8A), label = "Champions")
            LegendItem(color = Color(0xFF0284C7), label = "Europa League")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = Color(0xFFEA580C), label = "Conference")
            LegendItem(color = Color(0xFFDC2626), label = "Descenso")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(140.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, shape = CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TeamDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ----------------------------------------------------
// VISTA 3: PLANTILLA (DISEÑO CROMOS INTERACTIVOS)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantillaView() {
    var squadData by remember { mutableStateOf<ApiSquadData?>(null) }
    var selectedPlayer by remember { mutableStateOf<ApiPlayerDetail?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        squadData = RayoRepository.fetchSquadData()
    }

    if (squadData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Agrupamos directamente usando "positionEs" de tu JSON en mayúsculas
        val playersByPosition = squadData!!.squad.groupBy {
            it.positionEs?.uppercase() ?: ""
        }

        // Claves exactas tal cual vienen en tu JSON
        val sortedPositions = listOf("PORTERO", "DEFENSA", "CENTROCAMPISTA", "DELANTERO")

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // CABECERA ENTRENADOR
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = squadData!!.club.crest,
                                contentDescription = "Escudo Rayo",
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = squadData!!.club.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!squadData!!.club.coach.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Entrenador: ${squadData!!.club.coach}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // GRID DE CROMOS ORDENADO POR POSITION_ES
                sortedPositions.forEach { posKey ->
                    val playersInPos = playersByPosition[posKey] ?: emptyList()
                    if (playersInPos.isNotEmpty()) {
                        item {
                            // Pluralizamos el título de la categoría
                            val headerTitle = when (posKey) {
                                "PORTERO" -> "Porteros"
                                "DEFENSA" -> "Defensas"
                                "CENTROCAMPISTA" -> "Centrocampistas"
                                "DELANTERO" -> "Delanteros"
                                else -> posKey
                            }

                            Text(
                                text = headerTitle,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                            )
                        }

                        val rows = playersInPos.chunked(2)
                        items(rows) { rowPlayers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (player in rowPlayers) {
                                    PlayerCromoCard(
                                        player = player,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedPlayer = player }
                                    )
                                }
                                if (rowPlayers.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // MODAL BOTTOM SHEET JUGADOR MINIMALISTA CON TRADUCCIÓN (AÑADIDO)
            selectedPlayer?.let { player ->
                ModalBottomSheet(
                    onDismissRequest = { selectedPlayer = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!player.photo.isNullOrEmpty()) {
                            AsyncImage(
                                model = player.photo,
                                contentDescription = player.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            Box(modifier = Modifier.size(140.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text(text = player.shirtNumber?.toString() ?: "-", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = player.displayName ?: player.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(text = player.positionEs ?: translatePosition(player.position).dropLast(1), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TeamDetailRow("Nombre completo", player.name)
                                TeamDetailRow("Dorsal", player.shirtNumber?.toString() ?: "-")
                                // APLICADA LA FUNCIÓN DE TRADUCCIÓN DE PAÍS
                                TeamDetailRow("Nacionalidad", "${getFlagEmoji(player.countryCode)} ${getSpanishCountryName(player.countryCode, player.nationality)}")
                                if (!player.dateOfBirth.isNullOrEmpty()) {
                                    TeamDetailRow("Edad", "${calculateAge(player.dateOfBirth)} años")
                                    TeamDetailRow("Nacimiento", formatSpanishDateOnly(player.dateOfBirth))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CROMO INDIVIDUAL OPTIMIZADO PARA SCROLL FLUIDO
@Composable
fun PlayerCromoCard(player: ApiPlayerDetail, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var hasAnimated by rememberSaveable { mutableStateOf(false) }

    val alpha = remember { Animatable(if (hasAnimated) 1f else 0f) }
    val offsetY = remember { Animatable(if (hasAnimated) 0f else 100f) }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            launch { alpha.animateTo(1f, animationSpec = tween(400)) }
            launch { offsetY.animateTo(0f, animationSpec = tween(400)) }
            hasAnimated = true
        }
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = offsetY.value
            }
            .aspectRatio(0.85f)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!player.photo.isNullOrEmpty()) {
                AsyncImage(
                    model = player.photo,
                    contentDescription = player.name,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.material.icons.Icons.Default.Home, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.95f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = player.shirtNumber?.toString() ?: "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = player.displayName ?: player.name.split(" ").last(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = player.positionEs ?: translatePosition(player.position).dropLast(1),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

// ==========================================
// UTILIDADES DE LA PLANTILLA
// ==========================================

fun getFlagEmoji(countryCode: String?): String {
    if (countryCode.isNullOrEmpty() || countryCode.length != 2) return "🏴"
    val code = countryCode.uppercase()
    val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

fun calculateAge(dateOfBirth: String?): Int? {
    if (dateOfBirth.isNullOrBlank()) return null
    return try {
        val parts = dateOfBirth.split("-")
        val birthYear = parts[0].toInt()
        val birthMonth = parts[1].toInt()
        val birthDay = parts[2].toInt()

        val today = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Madrid"))
        var age = today.get(java.util.Calendar.YEAR) - birthYear
        if (today.get(java.util.Calendar.MONTH) + 1 < birthMonth ||
            (today.get(java.util.Calendar.MONTH) + 1 == birthMonth && today.get(java.util.Calendar.DAY_OF_MONTH) < birthDay)
        ) {
            age--
        }
        age
    } catch (e: Exception) {
        null
    }
}

fun translatePosition(position: String): String {
    return when (position.lowercase()) {
        "goalkeeper" -> "Porteros"
        "defence" -> "Defensas"
        "midfield" -> "Centrocampistas"
        "offence" -> "Delanteros"
        else -> position
    }
}

// FUNCIÓN DE TRADUCCIÓN DE PAÍS (AÑADIDA)
fun getSpanishCountryName(countryCode: String?, defaultName: String): String {
    if (countryCode.isNullOrEmpty()) return defaultName
    return try {
        val locale = java.util.Locale("", countryCode)
        locale.getDisplayCountry(java.util.Locale("es", "ES")).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        defaultName
    }
}