package com.example.adrv.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EquipoScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
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

                    // Transición fluida de color para el fondo
                    val backgroundColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tabBg"
                    )

                    // Transición fluida de color para el texto
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
                            .clickable { selectedTabIndex = index }
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

        // CONTENIDO SEGÚN PESTAÑA
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> CalendarioView()
                1 -> ClasificacionView()
                2 -> PlantillaView()
            }
        }
    }
}

// ----------------------------------------------------
// VISTA 1: CALENDARIO
// ----------------------------------------------------
@Composable
fun CalendarioView() {
    var rayoData by remember { mutableStateOf<RayoData?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now(ZoneId.of("Europe/Madrid"))) }

    val scrollState = rememberScrollState()

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

    // 1. Fecha de hoy por defecto para marcar visualmente la celda
    val today = LocalDate.now(ZoneId.of("Europe/Madrid"))
    var selectedDate by remember { mutableStateOf(today) }

    // 2. Partido explícito seleccionado para el panel de detalles inferior
    var userSelectedMatch by remember { mutableStateOf<ApiMatch?>(null) }

    // Si el usuario no ha pinchado ningún partido, podemos mostrar por defecto el de hoy si lo hay, o dejarlo vacío
    val activeMatch = userSelectedMatch ?: matchesByDate[today]

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

                                // 3. Se marca en rojo si coincide con la fecha seleccionada (por defecto hoy)
                                val isSelected = (date == selectedDate)

                                DayCell(
                                    dayNumber = currentDay,
                                    match = matchOnThisDay,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        // Actualizamos siempre el día visual seleccionado
                                        selectedDate = date

                                        // Y si ese día tiene partido, actualizamos el panel de detalles inferior
                                        if (matchOnThisDay != null) {
                                            userSelectedMatch = matchOnThisDay
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

            // PANEL DETALLES: Solo se muestra si hay un partido asociado al día seleccionado o el de hoy
            activeMatch?.let { match ->
                MatchDetailsPanel(match)
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
            Text(text = match.competition, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(text = match.round, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            MatchInfoRow(label = "Competición", value = match.competition)
            MatchInfoRow(label = "Jornada", value = match.round)
            MatchInfoRow(label = "Fecha", value = formatSpanishDateOnly(match.dateText))
            MatchInfoRow(label = "Hora local", value = formatSpanishTimeOnly(match.dateText))

            if (!match.venue.isNullOrEmpty() && match.venue != "Estadio Visitante") {
                MatchInfoRow(label = "Estadio", value = match.venue)
            }
            if (!match.referee.isNullOrEmpty() && match.referee != "Por designar") {
                MatchInfoRow(label = "Árbitro", value = match.referee)
            }

            val estadoTraducido = when (match.statusApi) {
                "FINISHED", "AWARDED" -> "Finalizado"
                "IN_PLAY", "PAUSED" -> "En juego"
                "POSTPONED", "SUSPENDED" -> "Aplazado"
                "CANCELLED" -> "Cancelado"
                else -> "Por disputar"
            }
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

// ----------------------------------------------------
// VISTA 2: CLASIFICACIÓN (Actualizada con detalle al pulsar)
// ----------------------------------------------------
@Composable
fun ClasificacionView() {
    var standingsData by remember { mutableStateOf<ApiStandingsData?>(null) }
    // Estado para saber qué equipo se ha seleccionado para ver sus detalles
    var selectedTeam by remember { mutableStateOf<ApiStanding?>(null) }

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cabecera de la tabla
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pos", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f))
                        Text("Equipo", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                        Text("PJ", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f))
                        Text("PTS", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f))
                    }
                }

                // Filas de los equipos (ahora clicables)
                items(standingsData!!.standings) { team ->
                    val isRayo = team.teamName.contains("Rayo")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTeam = team }, // <--- Al hacer clic guardamos el equipo seleccionado
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRayo) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${team.position}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.5f)) {
                                AsyncImage(model = team.crest, contentDescription = null, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                                Text(text = team.shortName, fontWeight = if (isRayo) FontWeight.Bold else FontWeight.Normal)
                            }

                            Text(text = "${team.playedGames}", modifier = Modifier.weight(0.1f))
                            Text(text = "${team.points}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f))
                        }
                    }
                }
            }

            // DIÁLOGO / TARJETA FLOTANTE CON LOS DATOS DETALLADOS AL PULSAR UN EQUIPO
            selectedTeam?.let { team ->
                AlertDialog(
                    onDismissRequest = { selectedTeam = null },
                    confirmButton = {
                        TextButton(onClick = { selectedTeam = null }) {
                            Text("Cerrar")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = team.crest, contentDescription = null, modifier = Modifier.size(32.dp).padding(end = 12.dp))
                            Text(text = team.teamName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider()
                            TeamDetailRow(label = "Posición en la tabla", value = "${team.position}º")
                            TeamDetailRow(label = "Puntos", value = "${team.points}")
                            TeamDetailRow(label = "Partidos Jugados", value = "${team.playedGames}")
                            TeamDetailRow(label = "Partidos Ganados", value = "${team.won}")
                            TeamDetailRow(label = "Partidos Empatados", value = "${team.draw}")
                            TeamDetailRow(label = "Partidos Perdidos", value = "${team.lost}")
                            TeamDetailRow(label = "Goles a Favor", value = "${team.goalsFor}")
                            TeamDetailRow(label = "Goles en Contra", value = "${team.goalsAgainst}")
                            TeamDetailRow(label = "Diferencia de Goles", value = if (team.goalDifference > 0) "+${team.goalDifference}" else "${team.goalDifference}")
                        }
                    }
                )
            }
        }
    }
}

// Componente auxiliar para las filas del diálogo de detalles
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
// VISTA 3: PLANTILLA (Actualizada con posición y banderas)
// ----------------------------------------------------
@Composable
fun PlantillaView() {
    var squadData by remember { mutableStateOf<ApiSquadData?>(null) }

    LaunchedEffect(Unit) {
        squadData = RayoRepository.fetchSquadData()
    }

    if (squadData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val playersByPosition = squadData!!.squad.groupBy { it.position }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera del Club y Entrenador (Ya centrada y corregida)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                            modifier = Modifier.size(60.dp)
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Entrenador: ${squadData!!.club.coach}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Recorremos las posiciones
            playersByPosition.forEach { (position, players) ->
                item {
                    Text(
                        text = translatePosition(position),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(players) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // --- CAJITA DE POSICIÓN (Sustituye al dorsal) ---
                                val (posShort, posColor) = getPositionBadgeInfo(player.position)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(posColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = posShort,
                                        fontWeight = FontWeight.Bold,
                                        color = posColor,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Nombre del jugador
                                Text(
                                    text = player.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // --- BANDERA VISUAL SEGÚN NACIONALIDAD ---
                            Text(
                                text = getFlagEmoji(player.countryCode),
                                fontSize = 22.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// FUNCIONES AUXILIARES PARA POSICIONES Y BANDERAS
// ----------------------------------------------------

// Devuelve la abreviatura y el color correspondiente a cada demarcación
fun getPositionBadgeInfo(position: String): Pair<String, Color> {
    return when (position.lowercase()) {
        "goalkeeper" -> Pair("PT", Color(0xFFFFB300)) // Amarillo
        "defence"    -> Pair("DF", Color(0xFF1E88E5)) // Azul
        "midfield"   -> Pair("MC", Color(0xFF43A047)) // Verde
        "offence"    -> Pair("DL", Color(0xFFE53935)) // Rojo
        else         -> Pair("S/P", Color.Gray)
    }
}

// Convierte cualquier código ISO de 2 letras (ej: "ID", "ES") en su emoji de bandera matemático
fun getFlagEmoji(countryCode: String?): String {
    if (countryCode.isNullOrEmpty() || countryCode.length != 2) return "🌍"

    val code = countryCode.uppercase()
    val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6

    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
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