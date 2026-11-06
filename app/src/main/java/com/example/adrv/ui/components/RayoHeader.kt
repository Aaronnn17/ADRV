package com.example.adrv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adrv.R
import com.example.adrv.ui.theme.RayoRed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun RayoHeader(
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    unreadCount: Int = 0 // <--- Añadimos el contador de no leídas
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Tu área",
                tint = Color.White
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                color = Color.White,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "ADRV 1924",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Botón de campana con indicador condicional
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(text = unreadCount.toString(), color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        ) {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Color.White
                )
            }
        }
    }
}