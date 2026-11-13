package com.example.adrv

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.adrv.data.RayoRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdrvUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("adrv_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) return Result.success()

        val notifyNews = prefs.getBoolean("notify_news", true)
        val notifyMatches = prefs.getBoolean("notify_matches", true)

        // ¡Ya podemos descomentar esto y usarlo!
        val allowedSources = prefs.getStringSet("notify_sources", emptySet()) ?: emptySet()

        return try {
            // --- 1. COMPROBAR NOTICIAS ---
            if (notifyNews) {
                val newsData = RayoRepository.fetchNewsData()
                val latestNews = newsData?.news?.firstOrNull()
                val lastNewsTitle = prefs.getString("last_news_title", "")

                if (latestNews != null && latestNews.title != lastNewsTitle) {
                    // Usamos tu propiedad "fuente"
                    val fuenteNoticia = latestNews.fuente

                    // Si el set está vacío (todas permitidas) o si contiene la fuente actual:
                    if (allowedSources.isEmpty() || allowedSources.contains(fuenteNoticia)) {
                        showNotification(
                            id = 1924,
                            title = "Nueva noticia: $fuenteNoticia",
                            message = latestNews.title,
                            targetType = "news",
                            targetData = latestNews.link // Pasamos el link de la noticia
                        )
                    }

                    // Guardamos siempre el título para no volver a evaluar esta misma noticia en el futuro
                    prefs.edit().putString("last_news_title", latestNews.title).apply()
                }
            }

            // --- 2. COMPROBAR PARTIDOS (Horarios confirmados) ---
            if (notifyMatches) {
                val rayoData = RayoRepository.fetchRayoData()
                val nextMatch = rayoData?.partidos?.firstOrNull { it.status != "FINISHED" }
                val lastSavedMatchDate = prefs.getString("last_match_date", "")

                if (nextMatch != null) {
                    if (!lastSavedMatchDate.isNullOrEmpty() && nextMatch.dateText != lastSavedMatchDate) {
                        val horaFormateada = try {
                            val instant = Instant.parse(nextMatch.dateText)
                            instant.atZone(ZoneId.of("Europe/Madrid")).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                        } catch (e: Exception) { nextMatch.dateText }

                        showNotification(
                            id = 1925,
                            title = "Horario Confirmado / Modificado",
                            message = "${nextMatch.homeTeam} vs ${nextMatch.awayTeam} -> $horaFormateada",
                            targetType = "match",
                            targetData = nextMatch.dateText // Pasamos la fecha del partido
                        )
                    }
                    prefs.edit().putString("last_match_date", nextMatch.dateText).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }


    private fun showNotification(id: Int, title: String, message: String, targetType: String, targetData: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "adrv_updates_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Actualizaciones del Rayo", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_type", targetType)
            putExtra("target_data", targetData)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_adrv)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}