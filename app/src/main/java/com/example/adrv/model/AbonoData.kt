package com.example.adrv.model

data class AbonoData(
    val abonadoNo: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val puertas: String = "",
    val grada: String = "",
    val fila: String = "",
    val asiento: String = "",
    val categoria: String = "ADULTO",
    val codigoSocio: String = "",
    val temporada: String = "TEMPORADA 2026/2027",
    val barcodeNumber: String = "",
    val sponsorText: String = "DIGI",
    val frontPhotoPath: String? = null,
    val backPhotoPath: String? = null
) {
    val isConfigured: Boolean
        get() = abonadoNo.isNotBlank() || nombre.isNotBlank() || frontPhotoPath != null || backPhotoPath != null
}