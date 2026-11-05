package com.example.adrv.data

import android.content.Context
import android.net.Uri
import com.example.adrv.model.AbonoData
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AbonoStorage(private val context: Context) {

    private val prefs = context.getSharedPreferences("abono_prefs_v2", Context.MODE_PRIVATE)

    fun loadAbonoData(): AbonoData {
        return AbonoData(
            abonadoNo = prefs.getString("abonadoNo", "") ?: "",
            nombre = prefs.getString("nombre", "") ?: "",
            apellidos = prefs.getString("apellidos", "") ?: "",
            puertas = prefs.getString("puertas", "") ?: "",
            grada = prefs.getString("grada", "") ?: "",
            fila = prefs.getString("fila", "") ?: "",
            asiento = prefs.getString("asiento", "") ?: "",
            categoria = prefs.getString("categoria", "") ?: "",
            codigoSocio = prefs.getString("codigoSocio", "") ?: "",
            temporada = prefs.getString("temporada", "") ?: "",
            barcodeNumber = prefs.getString("barcodeNumber", "") ?: "",
            sponsorText = prefs.getString("sponsorText", "") ?: "",
            frontPhotoPath = prefs.getString("frontPhotoPath", null),
            backPhotoPath = prefs.getString("backPhotoPath", null)
        )
    }

    fun saveAbonoData(data: AbonoData) {
        prefs.edit()
            .putString("abonadoNo", data.abonadoNo)
            .putString("nombre", data.nombre)
            .putString("apellidos", data.apellidos)
            .putString("puertas", data.puertas)
            .putString("grada", data.grada)
            .putString("fila", data.fila)
            .putString("asiento", data.asiento)
            .putString("categoria", data.categoria)
            .putString("codigoSocio", data.codigoSocio)
            .putString("temporada", data.temporada)
            .putString("barcodeNumber", data.barcodeNumber)
            .putString("sponsorText", data.sponsorText)
            .putString("frontPhotoPath", data.frontPhotoPath)
            .putString("backPhotoPath", data.backPhotoPath)
            .apply()
    }

    fun savePhotoFromUri(uri: Uri, isFront: Boolean): String? {
        return try {
            val fileName = if (isFront) "abono_front.jpg" else "abono_back.jpg"
            val file = File(context.filesDir, fileName)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}