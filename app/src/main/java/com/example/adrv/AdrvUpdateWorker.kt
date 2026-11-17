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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
        val allowedSources = prefs.getStringSet("notify_sources", emptySet()) ?: emptySet()

        return try {
            // --- 1. COMPROBAR TODAS LAS NOTICIAS NUEVAS ---
            if (notifyNews) {
                val newsData = RayoRepository.fetchNewsData()
                val recentNews = newsData?.news?.take(10) ?: emptyList()

                // Recuperamos el historial de IDs de noticias que YA hemos notificado
                val notifiedNewsIds = prefs.getStringSet("notified_news_ids", null)?.toMutableSet()

                if (notifiedNewsIds == null) {
                    // Primera vez que se ejecuta con este sistema: guardamos las actuales sin spamear 10 notificaciones de golpe
                    val initialIds = recentNews.map { it.id }.toSet()
                    prefs.edit().putStringSet("notified_news_ids", initialIds).apply()
                } else {
                    // Recorremos de la más antigua a la más reciente entre las nuevas para que salten en orden
                    recentNews.reversed().forEach { newsItem ->
                        if (!notifiedNewsIds.contains(newsItem.id)) {
                            val fuenteNoticia = if (newsItem.fuente.equals("Oficial", ignoreCase = true)) {
                                "Rayo Vallecano"
                            } else {
                                newsItem.fuente
                            }

                            // Comprobamos si la fuente está permitida en los ajustes del usuario
                            val isAllowed = allowedSources.isEmpty() || allowedSources.any {
                                it.equals(fuenteNoticia, ignoreCase = true) || it.equals(newsItem.fuente, ignoreCase = true)
                            }

                            if (isAllowed) {
                                showNotification(
                                    id = newsItem.id.hashCode(), // ID único para que NO se pisen entre sí
                                    title = "🗞️ Nueva noticia: $fuenteNoticia",
                                    message = newsItem.title,
                                    targetType = "news",
                                    targetData = newsItem.link
                                )
                            }

                            // Marcamos este ID como ya procesado
                            notifiedNewsIds.add(newsItem.id)
                        }
                    }

                    // Guardamos la lista actualizada (limitada a los últimos 100 IDs para que no crezca infinito)
                    val trimmedIds = notifiedNewsIds.toList().takeLast(100).toSet()
                    prefs.edit().putStringSet("notified_news_ids", trimmedIds).apply()
                }
            }

            // --- 2. COMPROBAR PRÓXIMOS 5 PARTIDOS (Horarios confirmados o modificados) ---
            if (notifyMatches) {
                val rayoData = RayoRepository.fetchRayoData()
                val today = LocalDate.now(ZoneId.of("Europe/Madrid"))

                val upcomingMatches = rayoData?.partidos?.filter { match ->
                    try {
                        !ZonedDateTime.parse(match.dateText)
                            .withZoneSameInstant(ZoneId.of("Europe/Madrid"))
                            .toLocalDate()
                            .isBefore(today)
                    } catch (e: Exception) { false }
                }?.take(5) ?: emptyList()

                upcomingMatches.forEachIndexed { index, match ->
                    val prefKey = "worker_match_date_${match.id}"
                    val lastSavedDate = prefs.getString(prefKey, "") ?: ""

                    if (lastSavedDate.isNotEmpty() && lastSavedDate != match.dateText) {
                        val horaFormateada = try {
                            val instant = Instant.parse(match.dateText)
                            instant.atZone(ZoneId.of("Europe/Madrid")).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                        } catch (e: Exception) { match.dateText }

                        showNotification(
                            id = 1925 + index, // ID único por partido para que no se pisen entre sí
                            title = "⚽ ¡Horario confirmado / Modificado!",
                            message = "${match.homeTeam} vs ${match.awayTeam} -> $horaFormateada",
                            targetType = "match",
                            targetData = match.dateText
                        )
                    }
                    prefs.edit().putString(prefKey, match.dateText).apply()
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
            val channel = NotificationChannel(channelId, "Actualizaciones del Rayo", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_${targetType}_$id" // Evita que Android mezcle el PendingIntent de noticia y partido
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}