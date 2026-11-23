package com.example.adrv.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("actualizado_en") val actualizadoEn: String = "",
    val disponible: Boolean = false,
    val motivo: String? = null,
    val partido: WeatherMatchInfo? = null,
    @SerializedName("hora_partido") val horaPartido: WeatherHourData? = null,
    @SerializedName("resumen_dia") val resumenDia: WeatherDailySummary? = null,
    @SerializedName("evolucion_horaria") val evolucionHoraria: List<WeatherHourData> = emptyList(),
    @SerializedName("consejo_grada") val consejoGrada: String? = null
)

data class WeatherMatchInfo(
    val id: String = "",
    val homeTeam: String = "",
    val awayTeam: String = "",
    val dateText: String = "",
    @SerializedName("fecha_local") val fechaLocal: String = "",
    @SerializedName("hora_local") val horaLocal: String = "",
    val estadio: String = "",
    val ciudad: String = ""
)

data class WeatherHourData(
    val hora: String = "",
    val temperatura: Double? = null,
    @SerializedName("sensacion_termica") val sensacionTermica: Double? = null,
    val humedad: Int? = null,
    @SerializedName("probabilidad_lluvia") val probabilidadLluvia: Int? = null,
    @SerializedName("precipitacion_mm") val precipitacionMm: Double? = null,
    val nubosidad: Int? = null,
    @SerializedName("viento_kmh") val vientoKmh: Double? = null,
    @SerializedName("rachas_viento_kmh") val rachasVientoKmh: Double? = null,
    @SerializedName("direccion_viento_grados") val direccionVientoGrados: Int? = null,
    @SerializedName("direccion_viento") val direccionViento: String = "",
    @SerializedName("indice_uv") val indiceUv: Double? = null,
    @SerializedName("visibilidad_km") val visibilidadKm: Double? = null,
    @SerializedName("es_dia") val esDia: Boolean = true,
    val descripcion: String = "",
    val icono: String = "partly_cloudy",
    val etiqueta: String? = null
)

data class WeatherDailySummary(
    @SerializedName("temp_max") val tempMax: Double? = null,
    @SerializedName("temp_min") val tempMin: Double? = null,
    @SerializedName("prob_lluvia_max") val probLluviaMax: Int? = null,
    @SerializedName("viento_max_kmh") val vientoMaxKmh: Double? = null,
    @SerializedName("rachas_max_kmh") val rachasMaxKmh: Double? = null,
    @SerializedName("uv_max") val uvMax: Double? = null,
    val amanecer: String = "",
    val atardecer: String = "",
    val descripcion: String = ""
)