package com.example.adrv.data

import com.example.adrv.model.RayoData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object RayoRepository {
    private const val GITHUB_JSON_URL = "https://raw.githubusercontent.com/Aaronnn17/ADRV/refs/heads/master/matches.json"

    suspend fun fetchRayoData(): RayoData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(GITHUB_JSON_URL).readText()
            Gson().fromJson(jsonString, RayoData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}