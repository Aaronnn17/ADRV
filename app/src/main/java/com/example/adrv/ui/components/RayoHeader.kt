package com.example.adrv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adrv.R
import kotlinx.coroutines.launch

@Composable
fun RayoHeader(
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    unreadCount: Int = 0
) {
    val scope = rememberCoroutineScope()
    val settingsRotation = remember { Animatable(0f) }
    val bellRotation = remember { Animatable(0f) }

    // Repica la campana cuando cambia el número de notificaciones sin leer
    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            bellRotation.snapTo(-22f)
            bellRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.25f,
                    stiffness = 450f
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // BOTÓN DE AJUSTES CON GIRO ELÁSTICO
        IconButton(
            onClick = {
                scope.launch {
                    settingsRotation.animateTo(
                        targetValue = settingsRotation.value + 90f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                onSettingsClick()
            },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    rotationZ = settingsRotation.value
                }
            )
        }

        // ESCUDO Y TEXTOS CENTRADOS
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_adrv),
                contentDescription = "Logo Rayo Vallecano",
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "RAYO VALLECANO",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "ADRV 1924",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "by @Aaronnn17",
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal
            )
        }

        // BOTÓN DE NOTIFICACIONES CON CAMPANA VIVA Y BADGE ELÁSTICO
        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BadgedBox(
                badge = {
                    AnimatedVisibility(
                        visible = unreadCount > 0,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                        exit = scaleOut()
                    ) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            OdometerText(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            bellRotation.snapTo(-20f)
                            bellRotation.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.25f,
                                    stiffness = 450f
                                )
                            )
                        }
                        onNotificationClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = bellRotation.value
                            transformOrigin = TransformOrigin(0.5f, 0.15f)
                        }
                    )
                }
            }
        }
    }
}