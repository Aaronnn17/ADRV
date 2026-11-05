package com.example.adrv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.adrv.ui.theme.RayoRed

@Composable
fun RayoFranjaBackground(
    modifier: Modifier = Modifier,
    franjaColor: Color = RayoRed,
    franjaWidthRatio: Float = 0.28f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stripWidth = width * franjaWidthRatio

        val path = Path().apply {
            moveTo(width + stripWidth * 0.5f, 0f)
            lineTo(width - stripWidth * 0.5f, 0f)
            lineTo(-stripWidth * 0.5f, height)
            lineTo(stripWidth * 0.5f, height)
            close()
        }

        clipRect(0f, 0f, width, height) {
            drawPath(
                path = path,
                color = franjaColor.copy(alpha = 0.95f)
            )
        }
    }
}