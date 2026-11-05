package com.example.adrv.data

import android.content.Context
import android.net.Uri
import com.example.adrv.model.AbonoData
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object BarcodeAnalyzer {

    fun analyzeImage(
        context: Context,
        imageUri: Uri,
        currentData: AbonoData,
        onResult: (AbonoData) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)

            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
            )
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // 1. Extraemos el código de barras
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcodeValue = barcodes.firstOrNull()?.rawValue ?: currentData.barcodeNumber

                    // 2. Extraemos el texto
                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val newData = parseAbonoText(visionText.text, currentData.copy(barcodeNumber = barcodeValue))
                            onResult(newData)
                        }
                        .addOnFailureListener { e -> onFailure(e) }
                }
                .addOnFailureListener { e -> onFailure(e) }

        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun parseAbonoText(fullText: String, baseData: AbonoData): AbonoData {
        var data = baseData
        val upperText = fullText.uppercase()

        // Convertimos todo a una sola línea plana para que las separaciones de columnas del OCR no rompan la lectura
        val flatText = upperText.replace("\n", " ")
        val lines = upperText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Extracción con Regex (Infalible sin importar saltos de línea o huecos)
        Regex("ABONADO\\s*(?:Nº|N°|NO|N)?\\s*[:\\-]?\\s*(\\d+)").find(flatText)?.let {
            data = data.copy(abonadoNo = it.groupValues[1])
        }
        Regex("PUERTAS\\s*[:\\-]?\\s*([\\d,\\s]+)").find(flatText)?.let {
            data = data.copy(puertas = it.groupValues[1].replace(" ", ""))
        }
        Regex("FILA\\s*[:\\-]?\\s*(\\d+)").find(flatText)?.let {
            data = data.copy(fila = it.groupValues[1])
        }
        Regex("ASIENTO\\s*[:\\-]?\\s*(\\d+)").find(flatText)?.let {
            data = data.copy(asiento = it.groupValues[1])
        }
        Regex("(TEMPORADA\\s*\\d{4}/\\d{4})").find(flatText)?.let {
            data = data.copy(temporada = it.groupValues[1])
        }
        Regex("(ADULTO|INFANTIL|JUBILADO|JOVEN)").find(flatText)?.let {
            data = data.copy(categoria = it.groupValues[1])
        }

        // El código de socio (U03CCA) suele ser de 6 caracteres alfanuméricos. Lo buscamos aislándolo.
        Regex("\\b([A-Z0-9]{6})\\b").findAll(flatText).toList().let { matches ->
            val code = matches.lastOrNull { it.groupValues[1].matches(Regex(".*[A-Z].*")) }?.groupValues?.get(1)
            if (code != null) data = data.copy(codigoSocio = code)
        }

        // 2. Extracción de Nombre, Apellidos y Grada (Dependemos de su posición porque no tienen etiquetas como "Nombre:")
        for (i in lines.indices) {
            val line = lines[i]

            if (line.contains("ABONADO")) {
                val possibleName = if (i + 1 < lines.size) lines[i + 1] else ""
                val possibleSurname = if (i + 2 < lines.size) lines[i + 2] else ""

                // Si las siguientes líneas no son la de "PUERTAS", asumimos que son el nombre y apellido
                if (possibleName.isNotBlank() && !possibleName.contains("PUERTAS")) {
                    data = data.copy(nombre = possibleName)
                }
                if (possibleSurname.isNotBlank() && !possibleSurname.contains("PUERTAS")) {
                    data = data.copy(apellidos = possibleSurname)
                }
            }

            if (line.contains("PUERTAS")) {
                val possibleGrada = if (i + 1 < lines.size) lines[i + 1] else ""
                // Si la siguiente línea no es la de "FILA", asumimos que es el texto de la Grada
                if (possibleGrada.isNotBlank() && !possibleGrada.contains("FILA")) {
                    data = data.copy(grada = possibleGrada)
                }
            }
        }

        return data
    }
}