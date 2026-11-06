package com.example.adrv.model

import com.google.gson.annotations.SerializedName

data class RayoData(
    val metadata: Metadata,
    val club: Club,
    val partidos: List<ApiMatch>
)

data class Metadata(
    val temporada: String,
    @SerializedName("actualizado_en") val actualizadoEn: String
)

data class Club(
    val id: Int,
    val nombre: String,
    val estadio: String?,
    val entrenador: String?,
    val escudo: String?,
    val plantilla: List<ApiPlayer>
)

data class ApiPlayer(
    val id: Int,
    val name: String,
    val position: String,
    val nationality: String,
    val shirtNumber: Int?
)

data class ApiMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String,
    val dateText: String,
    val competition: String,
    val round: String,
    val homeLogoUrl: String,
    val awayLogoUrl: String,
    val venue: String? = null
)