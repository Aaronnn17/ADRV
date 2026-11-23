package com.example.adrv.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

@Composable
fun BarcodeImage(
    text: String,
    format: String = "CODE_128",
    modifier: Modifier = Modifier,
    width: Int = 800,
    height: Int = 220
) {
    val bitmap = remember(text, format) {
        generateBarcode(text, format, width, height)
    }

    // Caja blanca pura con margen (Quiet Zone) obligatoria para lectores láser de estadios
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Código de barras de acceso ($format)",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None // Evita difuminar los bordes de las barras
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(androidx.compose.ui.graphics.Color.White)
            )
        }
    }
}

private fun generateBarcode(content: String, formatName: String, width: Int, height: Int): Bitmap? {
    val zxingFormat = when (formatName.uppercase()) {
        "CODE_39" -> BarcodeFormat.CODE_39
        "CODE_93" -> BarcodeFormat.CODE_93
        "CODABAR" -> BarcodeFormat.CODABAR
        "ITF" -> BarcodeFormat.ITF
        "EAN_13" -> BarcodeFormat.EAN_13
        "EAN_8" -> BarcodeFormat.EAN_8
        "UPC_A" -> BarcodeFormat.UPC_A
        "UPC_E" -> BarcodeFormat.UPC_E
        "QR_CODE" -> BarcodeFormat.QR_CODE
        "PDF_417" -> BarcodeFormat.PDF_417
        "AZTEC" -> BarcodeFormat.AZTEC
        else -> BarcodeFormat.CODE_128
    }

    val hints = mapOf(EncodeHintType.MARGIN to 0)

    return try {
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(content, zxingFormat, width, height, hints)
        createBitmapFromMatrix(bitMatrix)
    } catch (e: Exception) {
        // Fallback a CODE_128 si por algún motivo el formato original falla al codificar
        try {
            val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, width, height, hints)
            createBitmapFromMatrix(bitMatrix)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}

private fun createBitmapFromMatrix(bitMatrix: com.google.zxing.common.BitMatrix): Bitmap {
    val matrixWidth = bitMatrix.width
    val matrixHeight = bitMatrix.height
    val pixels = IntArray(matrixWidth * matrixHeight)
    for (y in 0 until matrixHeight) {
        val offset = y * matrixWidth
        for (x in 0 until matrixWidth) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
    return bitmap
}