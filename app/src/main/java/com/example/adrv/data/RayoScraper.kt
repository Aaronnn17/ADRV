package com.example.adrv.data

import com.example.adrv.model.Match
import com.example.adrv.model.MatchStatus
import com.example.adrv.model.NewsItem
import com.example.adrv.model.Player
import com.example.adrv.model.PlayerPosition
import com.example.adrv.model.StandingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object RayoScraper {

    private const val BASE_URL = "https://www.rayovallecano.es"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // 1. Fetch News from https://www.rayovallecano.es/noticias
    suspend fun fetchNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("$BASE_URL/noticias")
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            val newsItems = mutableListOf<NewsItem>()
            val articles = doc.select("article, .news-card, .card, .grid-item, div.noticia, div.post")

            if (articles.isNotEmpty()) {
                articles.forEachIndexed { index, el ->
                    val title = el.select("h2, h3, h4, .title, .card-title, a").firstOrNull { it.text().length > 10 }?.text().orEmpty()
                    val link = el.select("a").attr("abs:href")
                    
                    // Extract image src or data-src or srcset
                    val imgEl = el.select("img").firstOrNull()
                    var imgSrc = imgEl?.attr("abs:data-src")?.ifEmpty { null }
                        ?: imgEl?.attr("abs:src")?.ifEmpty { null }
                        ?: imgEl?.attr("src")?.ifEmpty { null }

                    if (imgSrc != null && !imgSrc.startsWith("http")) {
                        imgSrc = "$BASE_URL$imgSrc"
                    }

                    val summary = el.select("p, .description, .excerpt").text()
                    val dateStr = el.select("time, .date, .fecha, span.time").text().ifEmpty { "Prensa Oficial" }

                    if (title.isNotBlank()) {
                        newsItems.add(
                            NewsItem(
                                id = "news_$index",
                                title = title,
                                summary = summary.ifEmpty { "Información de la actualidad franjirroja." },
                                date = dateStr,
                                sourceName = "Rayovallecano.es",
                                imageUrl = imgSrc,
                                articleUrl = link.ifEmpty { "$BASE_URL/noticias" },
                                isFeatured = index == 0
                            )
                        )
                    }
                }
            }

            if (newsItems.isNotEmpty()) {
                return@withContext newsItems
            } else {
                return@withContext parseHeadlineFallback(doc)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext SampleData.newsList
        }
    }

    private fun parseHeadlineFallback(doc: Document): List<NewsItem> {
        val news = mutableListOf<NewsItem>()
        // Parse all H2 / H3 headers and links on page
        val links = doc.select("a[href*=/noticia/], a[href*=/noticias/], a.title, h2 a, h3 a")
        links.forEachIndexed { index, a ->
            val title = a.text()
            val href = a.attr("abs:href")
            val parentImg = a.parent()?.select("img")?.attr("abs:src")
            if (title.length > 12 && news.none { it.title == title }) {
                news.add(
                    NewsItem(
                        id = "fall_$index",
                        title = title,
                        summary = "Noticia oficial del Rayo Vallecano.",
                        date = "Hace unos momentos",
                        sourceName = "Web Oficial Rayo",
                        imageUrl = parentImg?.ifEmpty { "https://www.rayovallecano.es/images/escudo.png" },
                        articleUrl = href.ifEmpty { "$BASE_URL/noticias" },
                        isFeatured = index == 0
                    )
                )
            }
        }
        return news.ifEmpty { SampleData.newsList }
    }

    // Fetch single article body for in-app reader
    suspend fun fetchArticleContent(url: String): String = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(8000).get()
            val paragraphs = doc.select("article p, .entry-content p, .news-detail p, main p")
            if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n") { it.text() }
            } else {
                "Contenido de la noticia disponible en el portal oficial del Rayo Vallecano de Madrid."
            }
        } catch (e: Exception) {
            "No se pudo cargar el contenido completo de la noticia. Comprueba tu conexión a internet."
        }
    }

    // 2. Fetch Matches using official MkFootballMatchCard structure
    suspend fun fetchMatches(): List<Match> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("$BASE_URL/partidos")
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            val matches = mutableListOf<Match>()

            // Buscamos todas las tarjetas oficiales de partidos
            val matchElements = doc.select("article.MkFootballMatchCard")

            matchElements.forEachIndexed { index, el ->
                // Nombres de los equipos usando las clases oficiales exactas
                val home = el.select(".MkFootballMatchCard--homeTeam .MkFootballMatchCard__teamName").text()
                val away = el.select(".MkFootballMatchCard--awayTeam .MkFootballMatchCard__teamName").text()

                // Logos / Escudos oficiales de los equipos
                val homeLogo = el.select(".MkFootballMatchCard--homeTeam img.MkFootballMatchCard__teamShield").firstOrNull()?.let {
                    it.attr("abs:src").ifEmpty { it.attr("src") }
                } ?: ""

                val awayLogo = el.select(".MkFootballMatchCard--awayTeam img.MkFootballMatchCard__teamShield").firstOrNull()?.let {
                    it.attr("abs:src").ifEmpty { it.attr("src") }
                } ?: ""

                // Competición y Jornada
                val competition = el.select(".MkFootballMatchCard__competition").text().ifEmpty { "LALIGA EA SPORTS" }
                val roundText = el.select(".MkFootballMatchCard__matchWeek").text()
                val round = if (roundText.isNotBlank()) "Jornada ${roundText.replace("J", "").trim()}" else "Estadio de Vallecas"

                // Enlace al detalle (Match Center)
                val matchLink = el.select("a.MkNavigate").attr("abs:href")
                    .ifEmpty { el.select("a[href*=/partidos/]").attr("abs:href") }

                // Estados especiales (como "Suspendido") o avisos
                val warningText = el.select("[class*='MkFootballMatchWarning__warning']").text()
                val isSuspended = warningText.uppercase().contains("SUSPENDIDO") || el.text().uppercase().contains("SUSPENDIDO")

                // Extracción de la fecha o marcador si está disponible en el texto de la tarjeta
                val fullText = el.text()
                val isFinished = fullText.contains("-") && !fullText.contains(":") && !isSuspended

                val status = when {
                    isSuspended -> MatchStatus.UPCOMING
                    isFinished -> MatchStatus.FINISHED
                    else -> MatchStatus.UPCOMING
                }

                val dateText = when {
                    isSuspended -> "SUSPENDIDO"
                    warningText.isNotBlank() -> warningText
                    else -> "Próximo partido"
                }

                if (home.isNotBlank() && away.isNotBlank()) {
                    matches.add(
                        Match(
                            id = "mk_match_${index}_${home.hashCode()}",
                            homeTeam = home,
                            awayTeam = away,
                            homeScore = null,
                            awayScore = null,
                            status = status,
                            dateText = dateText,
                            competition = competition,
                            round = round,
                            stadium = "Estadio de Vallecas",
                            homeLogoUrl = homeLogo.ifEmpty { null },
                            awayLogoUrl = awayLogo.ifEmpty { null },
                            homeLogoEmoji = if (home.contains("Rayo", true)) "⚡" else "🛡️",
                            awayLogoEmoji = if (away.contains("Rayo", true)) "⚡" else "🛡️",
                            matchUrl = matchLink.ifEmpty { null }
                        )
                    )
                }
            }

            if (matches.isNotEmpty()) matches else SampleData.upcomingMatches
        } catch (e: Exception) {
            e.printStackTrace()
            SampleData.upcomingMatches
        }
    }

    // 3. Fetch Squad / Players from https://www.rayovallecano.es/jugadores
    suspend fun fetchPlayers(): List<Player> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("$BASE_URL/jugadores")
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            val players = mutableListOf<Player>()
            val items = doc.select(".player-card, .jugador-item, article.player, .member-card, tr.player")

            items.forEachIndexed { index, el ->
                val name = el.select(".name, .nombre, .player-name").text()
                val numberStr = el.select(".number, .dorsal").text().replace("#", "").trim()
                val posStr = el.select(".position, .posicion").text()

                val position = when {
                    posStr.contains("Portero", true) || posStr.contains("Goalkeeper", true) -> PlayerPosition.PORTERO
                    posStr.contains("Defensa", true) || posStr.contains("Defender", true) -> PlayerPosition.DEFENSA
                    posStr.contains("Centrocampista", true) || posStr.contains("Midfielder", true) -> PlayerPosition.CENTROCAMPISTA
                    else -> PlayerPosition.DELANTERO
                }

                if (name.isNotBlank()) {
                    players.add(
                        Player(
                            id = "pl_$index",
                            name = name,
                            number = numberStr.toIntOrNull(),
                            position = position
                        )
                    )
                }
            }

            if (players.isNotEmpty()) players else SampleData.players
        } catch (e: Exception) {
            e.printStackTrace()
            SampleData.players
        }
    }

    // 4. Fetch Standings from https://www.rayovallecano.es/clasificacion
    suspend fun fetchStandings(): List<StandingItem> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("$BASE_URL/clasificacion")
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            val standings = mutableListOf<StandingItem>()
            val rows = doc.select("table tbody tr, .row-standing, .clasificacion-row")

            rows.forEachIndexed { index, row ->
                val cols = row.select("td, .col")
                if (cols.size >= 7) {
                    val pos = cols[0].text().toIntOrNull() ?: (index + 1)
                    val team = cols[1].text()
                    val pts = cols[2].text().toIntOrNull() ?: 0
                    val pj = cols[3].text().toIntOrNull() ?: 0
                    val pg = cols[4].text().toIntOrNull() ?: 0
                    val pe = cols[5].text().toIntOrNull() ?: 0
                    val pp = cols[6].text().toIntOrNull() ?: 0

                    if (team.isNotBlank()) {
                        standings.add(
                            StandingItem(
                                position = pos,
                                teamName = team,
                                played = pj,
                                won = pg,
                                drawn = pe,
                                lost = pp,
                                goalsFor = 0,
                                goalsAgainst = 0,
                                points = pts,
                                isRayo = team.contains("Rayo", true)
                            )
                        )
                    }
                }
            }

            if (standings.isNotEmpty()) standings else SampleData.standings
        } catch (e: Exception) {
            e.printStackTrace()
            SampleData.standings
        }
    }
}