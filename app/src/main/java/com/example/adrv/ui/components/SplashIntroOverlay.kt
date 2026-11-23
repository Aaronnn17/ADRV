package com.example.adrv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.adrv.R
import com.example.adrv.ui.theme.RayoRed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun AppIntroContainer(
    splashBackgroundColor: Color = Color(0xFF121212),
    content: @Composable () -> Unit
) {
    var isFinished by remember { mutableStateOf(false) }

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.88f) }
    val stripeProgress = remember { Animatable(0f) }
    val splitProgress = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.96f) }

    // Curvas sedosas tipo Apple / Material Emphasized
    val smoothOutEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val curtainEasing = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)

    LaunchedEffect(Unit) {
        // FASE 1: Aparece el escudo con suavidad mientras la franja roja cruza la pantalla en diagonal
        coroutineScope {
            launch {
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = smoothOutEasing)
                )
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 440, easing = smoothOutEasing)
                )
            }
            launch {
                delay(70)
                stripeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 480, easing = CubicBezierEasing(0.42f, 0f, 0.18f, 1f))
                )
            }
        }

        // Breve pausa de respiro para lucir el escudo sobre la franja
        delay(170)

        // FASE 2: El escudo se difumina suavemente y la franja roja se abre en dos revelando la app
        coroutineScope {
            launch {
                logoScale.animateTo(
                    targetValue = 1.12f,
                    animationSpec = tween(durationMillis = 360, easing = smoothOutEasing)
                )
            }
            launch {
                logoAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 280, easing = smoothOutEasing)
                )
            }
            launch {
                delay(60)
                splitProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 720, easing = curtainEasing)
                )
            }
            launch {
                delay(120)
                contentScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 660, easing = smoothOutEasing)
                )
            }
        }

        isFinished = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tu app ya cargada detrás, asentándose con un micro-zoom muy sutil
        Box(
            modifier = if (!isFinished) {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = contentScale.value
                        scaleY = contentScale.value
                    }
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            content()
        }

        // Telón diagonal franjirrojo + escudo
        if (!isFinished) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val diagLen = hypot(w, h)
                    if (diagLen == 0f) return@Canvas

                    // Diagonal arriba-derecha -> abajo-izquierda (misma inclinación que el rayo del escudo)
                    val dx = -w / diagLen
                    val dy = h / diagLen

                    // Normal apuntando hacia la esquina superior-izquierda
                    val nx = -h / diagLen
                    val ny = -w / diagLen

                    val bleed = diagLen * 0.25f
                    val startPt = Offset(w - dx * bleed, -dy * bleed)
                    val endPt = Offset(dx * bleed, h + dy * bleed)

                    val currentStripeEnd = Offset(
                        x = startPt.x + (endPt.x - startPt.x) * stripeProgress.value,
                        y = startPt.y + (endPt.y - startPt.y) * stripeProgress.value
                    )

                    val cornerDist = (w * h) / diagLen
                    val panelDepth = cornerDist + diagLen * 0.25f
                    val stripeHalfWidth = 22.dp.toPx()

                    val maxTravel = cornerDist + stripeHalfWidth + 48.dp.toPx()
                    val travel = maxTravel * splitProgress.value

                    // Solape invisible de 2px solo al principio para evitar líneas finas en la unión
                    val seamOverlap = if (splitProgress.value < 0.005f) 2f else 0f

                    // --- TELÓN SUPERIOR-IZQUIERDO ---
                    withTransform({
                        translate(left = nx * travel, top = ny * travel)
                    }) {
                        val bgPathTL = Path().apply {
                            moveTo(startPt.x - nx * seamOverlap, startPt.y - ny * seamOverlap)
                            lineTo(endPt.x - nx * seamOverlap, endPt.y - ny * seamOverlap)
                            lineTo(endPt.x + nx * panelDepth, endPt.y + ny * panelDepth)
                            lineTo(startPt.x + nx * panelDepth, startPt.y + ny * panelDepth)
                            close()
                        }
                        drawPath(path = bgPathTL, color = splashBackgroundColor)

                        if (stripeProgress.value > 0f) {
                            val stripePathTL = Path().apply {
                                moveTo(startPt.x - nx * seamOverlap, startPt.y - ny * seamOverlap)
                                lineTo(currentStripeEnd.x - nx * seamOverlap, currentStripeEnd.y - ny * seamOverlap)
                                lineTo(currentStripeEnd.x + nx * stripeHalfWidth, currentStripeEnd.y + ny * stripeHalfWidth)
                                lineTo(startPt.x + nx * stripeHalfWidth, startPt.y + ny * stripeHalfWidth)
                                close()
                            }
                            drawPath(path = stripePathTL, color = RayoRed)
                        }
                    }

                    // --- TELÓN INFERIOR-DERECHO ---
                    withTransform({
                        translate(left = -nx * travel, top = -ny * travel)
                    }) {
                        val bgPathBR = Path().apply {
                            moveTo(startPt.x + nx * seamOverlap, startPt.y + ny * seamOverlap)
                            lineTo(endPt.x + nx * seamOverlap, endPt.y + ny * seamOverlap)
                            lineTo(endPt.x - nx * panelDepth, endPt.y - ny * panelDepth)
                            lineTo(startPt.x - nx * panelDepth, startPt.y - ny * panelDepth)
                            close()
                        }
                        drawPath(path = bgPathBR, color = splashBackgroundColor)

                        if (stripeProgress.value > 0f) {
                            val stripePathBR = Path().apply {
                                moveTo(startPt.x + nx * seamOverlap, startPt.y + ny * seamOverlap)
                                lineTo(currentStripeEnd.x + nx * seamOverlap, currentStripeEnd.y + ny * seamOverlap)
                                lineTo(currentStripeEnd.x - nx * stripeHalfWidth, currentStripeEnd.y - ny * stripeHalfWidth)
                                lineTo(startPt.x - nx * stripeHalfWidth, startPt.y - ny * stripeHalfWidth)
                                close()
                            }
                            drawPath(path = stripePathBR, color = RayoRed)
                        }
                    }
                }

                // Sutil halo oscuro para dar profundidad al escudo sobre la franja roja
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value * 0.7f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Escudo oficial centrado
                Image(
                    painter = painterResource(id = R.drawable.logo_adrv),
                    contentDescription = "Escudo ADRV",
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                )
            }
        }
    }
}