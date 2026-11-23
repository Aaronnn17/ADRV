package com.example.adrv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adrv.model.WeatherHourData
import com.example.adrv.model.WeatherResponse
import com.example.adrv.ui.theme.RayoRed
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MatchWeatherWidget(
    weather: WeatherResponse,
    modifier: Modifier = Modifier
) {
    if (!weather.disponible || weather.horaPartido == null) return

    val timeline = weather.evolucionHoraria
    val defaultIndex = remember(timeline) {
        timeline.indexOfFirst { it.etiqueta.equals("Inicio", ignoreCase = true) }
            .takeIf { it >= 0 } ?: 0
    }

    var selectedIndex by remember(timeline) { mutableIntStateOf(defaultIndex) }
    var isExpanded by remember { mutableStateOf(false) }

    val activeHour: WeatherHourData = timeline.getOrNull(selectedIndex) ?: weather.horaPartido

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "weatherChevron"
    )

    // Misma tarjeta sutil y alineada que NextMatchCard
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- BARRA COMPACTA (ÚNICA PARTE VISIBLE POR DEFECTO) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedWeatherIcon(
                        iconKey = activeHour.icono,
                        isDay = activeHour.esDia,
                        cutoutColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Temperatura con Odómetro
                    Row(verticalAlignment = Alignment.Top) {
                        OdometerText(
                            text = "${activeHour.temperatura?.roundToInt() ?: "--"}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "°",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Descripción y ciudad/hora
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeHour.descripcion,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${weather.partido?.ciudad ?: ""} · ${activeHour.hora}h",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Indicador sutil de lluvia + flecha desplegable
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val rainProb = activeHour.probabilidadLluvia ?: 0
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Lluvia",
                        tint = if (rainProb >= 30) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$rainProb%",
                        color = if (rainProb >= 30) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Ver detalles del tiempo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }

            // --- PANEL DESPLEGABLE (SOLO SI EL USUARIO PULSA) ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Selector de horas interactivo (Previa -> Inicio -> 2ª Parte -> Salida)
                    if (timeline.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeline.forEachIndexed { index, hourItem ->
                                val isSelected = index == selectedIndex
                                val pillBg by animateColorAsState(
                                    targetValue = if (isSelected) RayoRed else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    label = "hourBg_$index"
                                )
                                val mainColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                val subColor = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pillBg)
                                        .jellyClick { selectedIndex = index }
                                        .padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = hourItem.etiqueta ?: hourItem.hora,
                                        color = subColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = hourItem.hora,
                                        color = subColor,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${hourItem.temperatura?.roundToInt() ?: "--"}°",
                                        color = mainColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 2. Fila de métricas clave compactas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactMetricTile(
                            icon = Icons.Default.Thermostat,
                            label = "Sensación",
                            value = "${activeHour.sensacionTermica?.roundToInt() ?: "--"}°",
                            modifier = Modifier.weight(1f)
                        )
                        CompactWindTile(
                            windKmh = activeHour.vientoKmh ?: 0.0,
                            degrees = activeHour.direccionVientoGrados ?: 0,
                            cardinal = activeHour.direccionViento,
                            modifier = Modifier.weight(1.2f)
                        )
                        CompactMetricTile(
                            icon = Icons.Default.WaterDrop,
                            label = "Humedad",
                            value = "${activeHour.humedad ?: "--"}%",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Segunda fila: Nubes, UV y Máx/Mín
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactMetricTile(
                            icon = Icons.Default.Cloud,
                            label = "Nubes",
                            value = "${activeHour.nubosidad ?: "--"}%",
                            modifier = Modifier.weight(1f)
                        )
                        CompactMetricTile(
                            icon = Icons.Default.WbSunny,
                            label = "Índice UV",
                            value = "${activeHour.indiceUv ?: "--"}",
                            modifier = Modifier.weight(1f)
                        )
                        weather.resumenDia?.let { day ->
                            CompactMetricTile(
                                icon = Icons.Default.Thermostat,
                                label = "Máx / Mín",
                                value = "${day.tempMax?.roundToInt() ?: "-"}°/${day.tempMin?.roundToInt() ?: "-"}°",
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }

                    // 4. Consejo de grada + amanecer/atardecer en una línea discreta
                    if (!weather.consejoGrada.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = weather.consejoGrada,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ICONO ANIMADO COMPACTO (SOL GIRATORIO, NUBE FLOTANTE, GOTAS DE LLUVIA)
// =========================================================================
@Composable
fun AnimatedWeatherIcon(
    iconKey: String,
    isDay: Boolean,
    cutoutColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "weatherIconAnim")

    val sunRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "sunRot"
    )
    val cloudDrift by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "cloudDrift"
    )
    val rainFall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "rainFall"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val showSunOrMoon = iconKey in listOf("clear", "mainly_clear", "partly_cloudy")
        val showCloud = iconKey in listOf("mainly_clear", "partly_cloudy", "cloudy", "fog", "drizzle", "rain", "heavy_rain", "storm", "snow")
        val showRain = iconKey in listOf("drizzle", "rain", "heavy_rain", "storm")

        if (showSunOrMoon) {
            val sunCenter = if (showCloud) Offset(w * 0.38f, h * 0.36f) else center
            val radius = if (showCloud) w * 0.18f else w * 0.22f

            if (isDay) {
                val rayCount = 8
                for (i in 0 until rayCount) {
                    val angleRad = Math.toRadians((sunRotation + i * (360f / rayCount)).toDouble())
                    val start = Offset(
                        sunCenter.x + (radius * 1.25f * cos(angleRad)).toFloat(),
                        sunCenter.y + (radius * 1.25f * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        sunCenter.x + (radius * 1.65f * cos(angleRad)).toFloat(),
                        sunCenter.y + (radius * 1.65f * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFFFACC15),
                        start = start,
                        end = end,
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    color = Color(0xFFFACC15),
                    radius = radius,
                    center = sunCenter
                )
            } else {
                drawCircle(
                    color = Color(0xFFCBD5E1),
                    radius = radius,
                    center = sunCenter
                )
                drawCircle(
                    color = cutoutColor,
                    radius = radius * 0.8f,
                    center = Offset(sunCenter.x + radius * 0.45f, sunCenter.y - radius * 0.2f)
                )
            }
        }

        if (showRain) {
            val dropOffsets = listOf(0.32f, 0.50f, 0.68f)
            dropOffsets.forEachIndexed { idx, xFrac ->
                val phase = (rainFall + idx * 0.33f) % 1f
                val startY = h * 0.65f + phase * (h * 0.25f)
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 1f - phase),
                    start = Offset(w * xFrac, startY),
                    end = Offset(w * xFrac - 2f, startY + h * 0.11f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        if (showCloud) {
            val cloudY = if (showRain) h * 0.52f else h * 0.58f
            val dx = cloudDrift
            val cloudColor = if (iconKey in listOf("heavy_rain", "storm")) Color(0xFF94A3B8) else Color(0xFFE2E8F0)

            drawCircle(cloudColor, radius = w * 0.17f, center = Offset(w * 0.36f + dx, cloudY))
            drawCircle(cloudColor, radius = w * 0.21f, center = Offset(w * 0.54f + dx, cloudY - h * 0.06f))
            drawCircle(cloudColor, radius = w * 0.15f, center = Offset(w * 0.70f + dx, cloudY + h * 0.02f))
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.24f + dx, cloudY),
                size = Size(w * 0.54f, h * 0.17f),
                cornerRadius = CornerRadius(14f, 14f)
            )
        }
    }
}

// =========================================================================
// SUBCOMPONENTES COMPACTOS Y NATIVOS
// =========================================================================
@Composable
private fun CompactMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactWindTile(
    windKmh: Double,
    degrees: Int,
    cardinal: String,
    modifier: Modifier = Modifier
) {
    val animatedAngle by animateFloatAsState(
        targetValue = degrees.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "windDir"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Viento $cardinal",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = RayoRed,
                modifier = Modifier
                    .size(11.dp)
                    .graphicsLayer { rotationZ = animatedAngle }
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "${windKmh.roundToInt()} km/h",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}