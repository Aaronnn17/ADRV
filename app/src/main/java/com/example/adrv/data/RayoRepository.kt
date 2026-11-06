package com.example.adrv.data

import com.example.adrv.model.ApiSquadData
import com.example.adrv.model.ApiStandingsData
import com.example.adrv.model.RayoData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object RayoRepository {
    // URL del Calendario
    private const val GITHUB_MATCHES_URL = "https://raw.githubusercontent.com/Aaronnn17/ADRV/refs/heads/master/matches.json"
    // URL de la Clasificación
    private const val GITHUB_STANDINGS_URL = "https://raw.githubusercontent.com/Aaronnn17/ADRV/refs/heads/master/standings.json"
    // URL de la Plantilla
    private const val GITHUB_SQUAD_URL = "https://raw.githubusercontent.com/Aaronnn17/ADRV/refs/heads/master/squad.json"


    suspend fun fetchRayoData(): RayoData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(GITHUB_MATCHES_URL).readText()
            Gson().fromJson(jsonString, RayoData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchStandingsData(): ApiStandingsData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(GITHUB_STANDINGS_URL).readText()
            Gson().fromJson(jsonString, ApiStandingsData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchSquadData(): ApiSquadData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(GITHUB_SQUAD_URL).readText()
            Gson().fromJson(jsonString, ApiSquadData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}