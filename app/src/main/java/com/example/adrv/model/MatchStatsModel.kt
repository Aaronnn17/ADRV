package com.example.adrv.model

import kotlinx.serialization.Serializable

@Serializable
data class MatchStatsResponse(
    val match_center_url: String = "",
    val cara_a_cara: CaraACara = CaraACara(),
    val enfrentamientos_recientes: List<Enfrentamiento> = emptyList(),
    val racha_ultimos_partidos: RachaPartidos = RachaPartidos(),
    val estadisticas_temporada: List<EstadisticaTemporada> = emptyList(),
    val estadisticas_jugadores: List<EstadisticaJugador> = emptyList() // Aunque esté vacío por ahora, lo preparamos
)

@Serializable
data class CaraACara(
    val victorias_local: String = "0",
    val empates: String = "0",
    val victorias_visitante: String = "0"
)

@Serializable
data class Enfrentamiento(
    val fecha: String,
    val local: String,
    val resultado: String,
    val visitante: String,
    val competicion: String = ""
)

@Serializable
data class RachaPartidos(
    val local: List<PartidoRacha> = emptyList(),
    val visitante: List<PartidoRacha> = emptyList()
)

@Serializable
data class PartidoRacha(
    val fecha: String,
    val local: String,
    val resultado: String,
    val visitante: String,
    val estado: String
)

@Serializable
data class EstadisticaTemporada(
    val estadistica: String,
    val media_local: String? = null,
    val local: String,
    val visitante: String,
    val media_visitante: String? = null
)

@Serializable
data class EstadisticaJugador(
    val estadistica: String,
    val jugador_local: String,
    val valor_local: String,
    val foto_local: String,
    val jugador_visitante: String,
    val valor_visitante: String,
    val foto_visitante: String
)