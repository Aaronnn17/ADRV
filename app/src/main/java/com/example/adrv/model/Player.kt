package com.example.adrv.model

enum class PlayerPosition(val label: String) {
    PORTERO("Portero"),
    DEFENSA("Defensa"),
    CENTROCAMPISTA("Centrocampista"),
    DELANTERO("Delantero"),
    TECNICO("Cuerpo Técnico")
}

data class Player(
    val id: String,
    val name: String,
    val number: Int?,
    val position: PlayerPosition,
    val nationality: String = "España"
)