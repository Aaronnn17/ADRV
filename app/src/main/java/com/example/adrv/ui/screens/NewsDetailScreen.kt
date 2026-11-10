package com.example.adrv.ui.screens

import android.text.Html
import android.webkit.WebView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.adrv.model.News

@Composable
fun NewsDetailScreen(
    article: News
) {
    val scrollState = rememberScrollState()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Categoría
        if (article.categories.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = article.categories.first().uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Título principal
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fecha elegante
        Text(
            text = formatNewsDate(article.published),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Imagen destacada
        if (!article.image.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = article.image,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Cuerpo de la noticia con espaciado y formato mejorado
        if (!article.content.isNullOrEmpty()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = false
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    // Le inyectamos CSS automático para que las imágenes nunca se salgan de la pantalla del móvil
                    val htmlData = """
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; padding: 0; margin: 0; color: #333; line-height: 1.6; }
                    img { max-width: 100%; height: auto; border-radius: 8px; margin-top: 10px; margin-bottom: 10px; }
                    iframe { max-width: 100%; }
                </style>
            </head>
            <body>
                ${article.content}
            </body>
            </html>
        """.trimIndent()

                    webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                }
            )
        } else if (!article.summary.isNullOrEmpty()) {
            Text(
                text = article.summary,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}