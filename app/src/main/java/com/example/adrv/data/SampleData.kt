package com.example.adrv.data

import com.example.adrv.model.Match
import com.example.adrv.model.MatchEvent
import com.example.adrv.model.MatchStatus
import com.example.adrv.model.NewsItem
import com.example.adrv.model.Player
import com.example.adrv.model.PlayerPosition
import com.example.adrv.model.StandingItem

object SampleData {

    // Real latest match: Rayo Vallecano 3 - 0 RC Celta (LALIGA EA SPORTS)
    val currentMatch = Match(
        id = "m_latest",
        homeTeam = "Rayo Vallecano",
        awayTeam = "RC Celta",
        homeScore = 3,
        awayScore = 0,
        status = MatchStatus.FINISHED,
        dateText = "Jornada 11 · Estadio de Vallecas",
        competition = "LaLiga EA Sports",
        round = "Estadio de Vallecas",
        homeLogoUrl = TeamLogoProvider.getLogoUrl("Rayo Vallecano"),
        awayLogoUrl = TeamLogoProvider.getLogoUrl("RC Celta"),
        events = listOf(
            MatchEvent(
                minute = "82'",
                type = "Gol",
                title = "¡Golazo de Isi Palazón!",
                description = "Disparo con la zurda desde fuera del área tras gran combinación colectiva."
            ),
            MatchEvent(
                minute = "55'",
                type = "Gol",
                title = "Gol de Sergio Camello",
                description = "Remate preciso en el área chica superando al guardameta rival."
            ),
            MatchEvent(
                minute = "34'",
                type = "Gol",
                title = "Gol de De Frutos",
                description = "Cabezazo imponente al primer palo tras centro medido."
            )
        )
    )

    // Real upcoming matches from rayovallecano.es/partidos
    val upcomingMatches = listOf(
        Match(
            id = "m13",
            homeTeam = "Rayo Vallecano",
            awayTeam = "Valencia CF",
            status = MatchStatus.UPCOMING,
            dateText = "sáb, 21 nov - 16:15",
            round = "LALIGA EA SPORTS · J13",
            homeLogoUrl = TeamLogoProvider.getLogoUrl("Rayo Vallecano"),
            awayLogoUrl = TeamLogoProvider.getLogoUrl("Valencia CF")
        ),
        Match(
            id = "m14",
            homeTeam = "Real Betis",
            awayTeam = "Rayo Vallecano",
            status = MatchStatus.UPCOMING,
            dateText = "dom, 29 nov - 18:30",
            round = "LALIGA EA SPORTS · J14",
            homeLogoUrl = TeamLogoProvider.getLogoUrl("Real Betis"),
            awayLogoUrl = TeamLogoProvider.getLogoUrl("Rayo Vallecano")
        ),
        Match(
            id = "m15",
            homeTeam = "Rayo Vallecano",
            awayTeam = "Levante UD",
            status = MatchStatus.UPCOMING,
            dateText = "dom, 6 dic - 14:00",
            round = "LALIGA EA SPORTS · J15",
            homeLogoUrl = TeamLogoProvider.getLogoUrl("Rayo Vallecano"),
            awayLogoUrl = TeamLogoProvider.getLogoUrl("Levante UD")
        )
    )

    val newsList = listOf(
        NewsItem(
            id = "n1",
            title = "Beñat San José: \"Muy felices por la victoria. Hicimos un partido muy completo\"",
            summary = "El técnico rayista valoró positivamente el rendimiento del equipo tras el holgado triunfo frente a la afición vallecana.",
            date = "Hace 4 días",
            sourceName = "rayovallecano.es",
            imageUrl = "https://www.rayovallecano.es/images/escudo.png",
            articleUrl = "https://www.rayovallecano.es/noticias",
            isFeatured = true
        ),
        NewsItem(
            id = "n2",
            title = "Rayo Vallecano 3 - RC Celta 0 Crónica",
            summary = "Gran actuación colectiva del conjunto franjirrojo para sumar tres puntos fundamentales en el Estadio de Vallecas.",
            date = "Hace 5 días",
            sourceName = "rayovallecano.es",
            imageUrl = "https://www.rayovallecano.es/images/escudo.png",
            articleUrl = "https://www.rayovallecano.es/noticias",
            isFeatured = false
        ),
        NewsItem(
            id = "n3",
            title = "El Rayo Vallecano decreta tres días de luto oficial",
            summary = "Comunicado institucional del club rayista en memoria de los fallecidos.",
            date = "Reciente",
            sourceName = "rayovallecano.es",
            imageUrl = "https://www.rayovallecano.es/images/escudo.png",
            articleUrl = "https://www.rayovallecano.es/noticias",
            isFeatured = false
        )
    )

    val standings = listOf(
        StandingItem(1, "FC Barcelona", 11, 10, 0, 1, 32, 10, 30),
        StandingItem(2, "Atlético de Madrid", 11, 9, 1, 1, 24, 8, 28),
        StandingItem(3, "Real Madrid", 11, 9, 0, 2, 25, 9, 27),
        StandingItem(4, "Real Betis", 11, 7, 2, 2, 20, 12, 23),
        StandingItem(5, "Real Sociedad", 11, 6, 1, 4, 17, 14, 19),
        StandingItem(6, "Sevilla FC", 11, 5, 2, 4, 16, 15, 17),
        StandingItem(7, "Athletic Club", 10, 4, 3, 3, 14, 11, 15),
        StandingItem(8, "CA Osasuna", 11, 4, 2, 5, 13, 16, 14),
        StandingItem(9, "Villarreal CF", 11, 3, 3, 5, 14, 18, 12),
        StandingItem(10, "Celta", 11, 2, 6, 3, 12, 16, 12),
        StandingItem(11, "Deportivo Alavés", 11, 3, 3, 5, 11, 15, 12),
        StandingItem(12, "Rayo Vallecano", 10, 3, 3, 4, 12, 14, 12, isRayo = true),
        StandingItem(13, "Getafe CF", 11, 3, 3, 5, 10, 15, 12),
        StandingItem(14, "RC Deportivo", 11, 2, 5, 4, 9, 13, 11),
        StandingItem(15, "RCD Espanyol", 11, 3, 2, 6, 10, 17, 11)
    )

    // REAL squad parsed directly from rayovallecano.es/jugadores
    val players = listOf(
        Player("1", "Cárdenas", 1, PlayerPosition.PORTERO),
        Player("13", "A. Batalla", 13, PlayerPosition.PORTERO),
        Player("25", "Audero", 25, PlayerPosition.PORTERO),
        Player("2", "Andrei", 2, PlayerPosition.DEFENSA),
        Player("3", "Kumbulla", 3, PlayerPosition.DEFENSA),
        Player("5", "Luiz Felipe", 5, PlayerPosition.DEFENSA),
        Player("15", "Mujaid", 15, PlayerPosition.DEFENSA),
        Player("17", "Pedrosa", 17, PlayerPosition.DEFENSA),
        Player("20", "Balliu", 20, PlayerPosition.DEFENSA),
        Player("22", "Pelayo", 22, PlayerPosition.DEFENSA),
        Player("24", "Lejeune", 24, PlayerPosition.DEFENSA),
        Player("33", "Jozhua V.", 33, PlayerPosition.DEFENSA),
        Player("4", "Pedro Díaz", 4, PlayerPosition.CENTROCAMPISTA),
        Player("6", "Pathé I. Ciss", 6, PlayerPosition.CENTROCAMPISTA),
        Player("7", "Isi", 7, PlayerPosition.CENTROCAMPISTA),
        Player("8", "Unai López", 8, PlayerPosition.CENTROCAMPISTA),
        Player("14", "Tsitaishvili", 14, PlayerPosition.CENTROCAMPISTA),
        Player("18", "Álvaro", 18, PlayerPosition.CENTROCAMPISTA),
        Player("23", "Óscar V.", 23, PlayerPosition.CENTROCAMPISTA),
        Player("36", "G.Bouare", 36, PlayerPosition.CENTROCAMPISTA),
        Player("9", "Alemão", 9, PlayerPosition.DELANTERO),
        Player("10", "Camello", 10, PlayerPosition.DELANTERO),
        Player("11", "Nteka", 11, PlayerPosition.DELANTERO),
        Player("19", "De Frutos", 19, PlayerPosition.DELANTERO),
        Player("21", "Fran Pérez", 21, PlayerPosition.DELANTERO),
        Player("37", "Rayane", 37, PlayerPosition.DELANTERO)
    )
}