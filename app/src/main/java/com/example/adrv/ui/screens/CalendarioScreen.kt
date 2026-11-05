package com.example.adrv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.adrv.data.RayoScraper
import com.example.adrv.data.SampleData
import com.example.adrv.model.Match
import com.example.adrv.model.MatchStatus
import com.example.adrv.model.Player
import com.example.adrv.model.PlayerPosition
import com.example.adrv.ui.theme.RayoRed
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarioScreen(
    modifier: Modifier = Modifier
) {
    var selectedTopTab by remember { mutableIntStateOf(0) }
    val topTabs = listOf("Calendario", "Clasificación", "Plantilla")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Minimalist Pill Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            topTabs.forEachIndexed { index, title ->
                val isSelected = selectedTopTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) RayoRed else Color.Transparent)
                        .clickable { selectedTopTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }

        when (selectedTopTab) {
            0 -> CalendarioSubscreen()
            1 -> ClasificacionSubscreen()
            2 -> PlantillaSubscreen()
        }
    }
}

// 1. Parser mejorado a prueba de fallos
fun parseDateTextToLocalDate(dateText: String, currentYear: Int): LocalDate? {
    // Si viene en formato DD/MM/YYYY
    Regex("(\\d{1,2})[/\\-](\\d{1,2})").find(dateText)?.let { match ->
        return try { LocalDate.of(currentYear, match.groupValues[2].toInt(), match.groupValues[1].toInt()) } catch(e: Exception){ null }
    }

    // Si viene en formato "13 dic" o "dom, 13 dic"
    val regex = Regex("(\\d{1,2})\\s*(ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)", RegexOption.IGNORE_CASE)
    val match = regex.find(dateText) ?: return null

    val day = match.groupValues[1].toInt()
    val month = when (match.groupValues[2].lowercase()) {
        "ene" -> 1; "feb" -> 2; "mar" -> 3; "abr" -> 4; "may" -> 5; "jun" -> 6
        "jul" -> 7; "ago" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; "dic" -> 12
        else -> return null
    }
    return try { LocalDate.of(currentYear, month, day) } catch (e: Exception) { null }
}

@Composable
fun CalendarioSubscreen() {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var matchesList by remember { mutableStateOf(SampleData.upcomingMatches) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val scraped = RayoScraper.fetchMatches()
            if (scraped.isNotEmpty()) matchesList = scraped
        }
    }

    val matchesByDate = remember(matchesList, currentMonth) {
        val map = mutableMapOf<LocalDate, Match>()
        matchesList.forEach { match ->
            val parsedDate = parseDateTextToLocalDate(match.dateText, currentMonth.year)
            if (parsedDate != null && parsedDate.month == currentMonth.month) {
                map[parsedDate] = match
            }
        }
        map
    }

    val selectedMatch = matchesByDate[selectedDate]

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(8.dp))

        // Selector de Meses
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = RayoRed)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = RayoRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cabecera Días de la semana
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                Text(
                    text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid del Calendario
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value
        val totalCells = firstDayOfWeek - 1 + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayOffset = cellIndex - (firstDayOfWeek - 1)

                        if (dayOffset in 0 until daysInMonth) {
                            val date = currentMonth.atDay(dayOffset + 1)
                            val isSelected = date == selectedDate
                            val hasMatch = matchesByDate.containsKey(date)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) RayoRed else Color.Transparent)
                                    .clickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                // Siempre mostramos el número
                                Text(
                                    text = "${date.dayOfMonth}",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (isSelected || hasMatch) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )

                                // Si hay partido, ponemos un puntito debajo
                                if (hasMatch) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 6.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else RayoRed)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedMatch != null) {
            MatchDetailsCard(selectedMatch)
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay eventos programados para este día.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MatchDetailsCard(match: Match) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera superior
            Text(text = match.competition.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = match.dateText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(16.dp))

            // Equipos y Marcador
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    if (!match.homeLogoUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(model = match.homeLogoUrl, contentDescription = match.homeTeam, modifier = Modifier.size(48.dp))
                    } else {
                        Text(text = match.homeLogoEmoji, fontSize = 36.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = match.homeTeam, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (match.homeScore != null) "${match.homeScore} - ${match.awayScore}" else match.dateText.substringAfter("-").trim().ifEmpty { "VS" },
                        fontSize = 32.sp, fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (match.status == MatchStatus.FINISHED) Color(0xFF1E293B) else RayoRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when(match.status) {
                                MatchStatus.FINISHED -> "Finalizado"
                                MatchStatus.LIVE -> "En Directo"
                                MatchStatus.UPCOMING -> "Próximo"
                            },
                            fontSize = 10.sp, color = if (match.status == MatchStatus.FINISHED) Color.White else RayoRed, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    if (!match.awayLogoUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(model = match.awayLogoUrl, contentDescription = match.awayTeam, modifier = Modifier.size(48.dp))
                    } else {
                        Text(text = match.awayLogoEmoji, fontSize = 36.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = match.awayTeam, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Falsas pestañas de navegación interna
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Previa", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Box(modifier = Modifier.height(2.dp).width(40.dp).background(RayoRed))
                }
                Text("Alineaciones", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Estadísticas", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(1.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

            // Clasificación comparativa (Simulada para diseño)
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(16.dp)) {
                Text("Clasificación", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Club", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Text("PTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text("PJ", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("GF", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("GC", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Fila Equipo Local
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("1.", fontSize = 11.sp, modifier = Modifier.width(16.dp))
                    Text(match.homeTeam, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text("-", fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("-", fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("-", fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Fila Equipo Visitante
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("2.", fontSize = 11.sp, modifier = Modifier.width(16.dp))
                    Text(match.awayTeam, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text("-", fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("-", fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("-", fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ... [El resto de tu código para ClasificacionSubscreen y PlantillaSubscreen permanece exactamente igual]

@Composable
fun ClasificacionSubscreen() {
    var standingsList by remember { mutableStateOf(SampleData.standings) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val scraped = RayoScraper.fetchStandings()
        if (scraped.isNotEmpty()) {
            standingsList = scraped
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Clasificación LaLiga EA Sports",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RayoRed)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "#", modifier = Modifier.width(28.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "Equipo", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "PJ", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(text = "PG", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(text = "PE", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(text = "PP", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(text = "PTS", modifier = Modifier.width(34.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = RayoRed)
            }

            standingsList.forEach { item ->
                val isRayo = item.isRayo || item.teamName.contains("Rayo", true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRayo) RayoRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isRayo) androidx.compose.foundation.BorderStroke(1.dp, RayoRed) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.position}",
                            modifier = Modifier.width(28.dp),
                            fontWeight = if (isRayo) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRayo) RayoRed else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isRayo) "⚡ " else "⚽ ", fontSize = 13.sp)
                            Text(
                                text = item.teamName,
                                fontWeight = if (isRayo) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRayo) RayoRed else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }

                        Text(text = "${item.played}", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text(text = "${item.won}", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text(text = "${item.drawn}", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text(text = "${item.lost}", modifier = Modifier.width(30.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text(
                            text = "${item.points}",
                            modifier = Modifier.width(34.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isRayo) RayoRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantillaSubscreen() {
    var playersList by remember { mutableStateOf<List<Player>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val scraped = RayoScraper.fetchPlayers()
        if (scraped.isNotEmpty()) {
            playersList = scraped
        } else {
            playersList = realOfficialSquad()
        }
        isLoading = false
    }

    val positions = listOf(
        PlayerPosition.PORTERO,
        PlayerPosition.DEFENSA,
        PlayerPosition.CENTROCAMPISTA,
        PlayerPosition.DELANTERO
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Plantilla Oficial Rayo Vallecano",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RayoRed)
            }
        } else {
            positions.forEach { pos ->
                val posPlayers = playersList.filter { it.position == pos }
                if (posPlayers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = pos.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = RayoRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column {
                        posPlayers.chunked(2).forEach { rowPlayers ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPlayers.forEach { player ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(105.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (player.number != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(RayoRed),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${player.number}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.size(26.dp))
                                                }
                                                Text(text = "⚡", fontSize = 18.sp)
                                            }

                                            Column {
                                                Text(
                                                    text = player.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = player.position.label,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                if (rowPlayers.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun realOfficialSquad(): List<Player> {
    return listOf(
        Player("1", "Cárdenas", 1, PlayerPosition.PORTERO),
        Player("2", "A. Batalla", 13, PlayerPosition.PORTERO),
        Player("3", "Audero", 25, PlayerPosition.PORTERO),
        Player("4", "Andrei", 2, PlayerPosition.DEFENSA),
        Player("5", "Kumbulla", 3, PlayerPosition.DEFENSA),
        Player("6", "Luiz Felipe", 5, PlayerPosition.DEFENSA),
        Player("7", "Mujaid", 15, PlayerPosition.DEFENSA),
        Player("8", "Pedrosa", 17, PlayerPosition.DEFENSA),
        Player("9", "Balliu", 20, PlayerPosition.DEFENSA),
        Player("10", "Pelayo", 22, PlayerPosition.DEFENSA),
        Player("11", "Lejeune", 24, PlayerPosition.DEFENSA),
        Player("12", "Jozhua V.", 33, PlayerPosition.DEFENSA),
        Player("13", "Pedro Díaz", 4, PlayerPosition.CENTROCAMPISTA),
        Player("14", "Pathé I. Ciss", 6, PlayerPosition.CENTROCAMPISTA),
        Player("15", "Isi", 7, PlayerPosition.CENTROCAMPISTA),
        Player("16", "Unai López", 8, PlayerPosition.CENTROCAMPISTA),
        Player("17", "Tsitaishvili", 14, PlayerPosition.CENTROCAMPISTA),
        Player("18", "Álvaro", 18, PlayerPosition.CENTROCAMPISTA),
        Player("19", "Óscar V.", 23, PlayerPosition.CENTROCAMPISTA),
        Player("20", "G.Bouare", 36, PlayerPosition.CENTROCAMPISTA),
        Player("21", "Alemão", 9, PlayerPosition.DELANTERO),
        Player("22", "Camello", 10, PlayerPosition.DELANTERO),
        Player("23", "Nteka", 11, PlayerPosition.DELANTERO),
        Player("24", "De Frutos", 19, PlayerPosition.DELANTERO),
        Player("25", "Fran Pérez", 21, PlayerPosition.DELANTERO),
        Player("26", "Rayane", 37, PlayerPosition.DELANTERO)
    )
}