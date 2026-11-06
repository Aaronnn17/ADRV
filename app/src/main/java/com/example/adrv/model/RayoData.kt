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
    val statusApi: String? = null,
    val dateText: String,
    val competition: String,
    val round: String,
    val homeLogoUrl: String,
    val awayLogoUrl: String,
    val venue: String? = null,
    val referee: String? = null
)

data class ApiStandingsData(
    val metadata: StandingsMetadata,
    val standings: List<ApiStanding>
)

data class StandingsMetadata(
    val competition: String,
    val season: String,
    val matchday: Int
)

data class ApiStanding(
    val position: Int,
    val teamName: String,
    val shortName: String,
    val crest: String,
    val playedGames: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int
)

data class ApiSquadData(
    val club: ClubDetails,
    val squad: List<ApiPlayerDetail>
)

data class ClubDetails(
    val id: Int,
    val name: String,
    val shortName: String,
    val crest: String,
    val venue: String?,
    val coach: String?
)

data class ApiPlayerDetail(
    val id: Int,
    val name: String,
    val position: String,
    val nationality: String,
    val countryCode: String?,
    val shirtNumber: Int?
)