package com.example.adrv.model

enum class MatchStatus {
    LIVE,
    FINISHED,
    UPCOMING
}

data class MatchEvent(
    val minute: String,
    val type: String,
    val title: String,
    val description: String,
    val isRayoAction: Boolean = true
)

data class Match(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: MatchStatus,
    val dateText: String,
    val competition: String = "LaLiga EA Sports",
    val round: String = "Estadio de Vallecas",
    val stadium: String = "Estadio de Vallecas",
    val category: String = "Fútbol masculino · Primer equipo",
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null,
    val homeLogoEmoji: String = "⚡",
    val awayLogoEmoji: String = "🛡️",
    val matchUrl: String? = null, // <-- Añadido para la URL específica del partido
    val events: List<MatchEvent> = emptyList()
)