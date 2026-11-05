package com.example.adrv.data

object TeamLogoProvider {

    fun getLogoUrl(teamName: String): String {
        val lower = teamName.lowercase()
        return when {
            lower.contains("rayo") -> "https://www.rayovallecano.es/images/escudo.png"
            lower.contains("barcelona") || lower.contains("barça") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/barcelona.png"
            lower.contains("madrid") && !lower.contains("atletico") && !lower.contains("atlético") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/real-madrid.png"
            lower.contains("atletico") || lower.contains("atlético") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/atletico.png"
            lower.contains("valencia") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/valencia.png"
            lower.contains("betis") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/betis.png"
            lower.contains("sociedad") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/real-sociedad.png"
            lower.contains("celta") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/celta.png"
            lower.contains("sevilla") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/sevilla.png"
            lower.contains("athletic") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/athletic.png"
            lower.contains("villarreal") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/villarreal.png"
            lower.contains("osasuna") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/osasuna.png"
            lower.contains("alavés") || lower.contains("alaves") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/alaves.png"
            lower.contains("getafe") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/getafe.png"
            lower.contains("espanyol") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/espanyol.png"
            lower.contains("girona") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/girona.png"
            lower.contains("mallorca") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/mallorca.png"
            lower.contains("levante") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/levante.png"
            lower.contains("elche") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/elche.png"
            lower.contains("racing") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/racing.png"
            lower.contains("recreativo") -> "https://s.laliga.com/assets/multimedia/logos/v1/160x160/recreativo.png"
            else -> "https://www.rayovallecano.es/images/escudo.png"
        }
    }
}