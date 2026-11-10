package com.example.adrv.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adrv.data.AbonoStorage
import com.example.adrv.data.BarcodeAnalyzer
import com.example.adrv.model.AbonoData
import com.example.adrv.ui.components.BarcodeImage
import com.example.adrv.ui.components.bounceClick
import com.example.adrv.ui.theme.RayoRed
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AbonoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storage = remember { AbonoStorage(context) }
    var abonoData by remember { mutableStateOf(storage.loadAbonoData()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFullScreenAbono by remember { mutableStateOf(false) }

    val frontPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val path = storage.savePhotoFromUri(it, isFront = true)
            if (path != null) {
                abonoData = abonoData.copy(frontPhotoPath = path)

                Toast.makeText(context, "Escaneando datos del abono...", Toast.LENGTH_SHORT).show()

                BarcodeAnalyzer.analyzeImage(
                    context = context,
                    imageUri = it,
                    currentData = abonoData,
                    onResult = { newData ->
                        abonoData = newData
                        storage.saveAbonoData(abonoData)
                        Toast.makeText(context, "¡Datos completados con éxito!", Toast.LENGTH_LONG).show()
                    },
                    onFailure = {
                        Toast.makeText(context, "Error al escanear. Rellena los datos a mano.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Abono Digital",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Carnet Socio Rayo Vallecano · Acceso a Torno",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar Abono",
                    tint = RayoRed
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Digital Pass
        OfficialPhysicalCardPass2D(
            data = abonoData,
            onClick = {
                // Solo se amplía si hay un código de barras válido guardado
                if (abonoData.barcodeNumber.isNotBlank()) {
                    showFullScreenAbono = true
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Uploader Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Genera tu pase desde el carnet físico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sube la foto de tu carnet. La app escaneará tu código de barras y datos automáticamente para generar tu Pase 2D.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        frontPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { },
                    colors = ButtonDefaults.buttonColors(containerColor = RayoRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (abonoData.barcodeNumber.isNotBlank()) "Actualizar Abono" else "Escanear Abono",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showFullScreenAbono) {
        FullScreenAbonoDialog(
            data = abonoData,
            onDismiss = { showFullScreenAbono = false }
        )
    }

    if (showEditDialog) {
        EditAbonoDialog(
            data = abonoData,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                abonoData = updated
                storage.saveAbonoData(updated)
                showEditDialog = false
            }
        )
    }
}

// Replica estilizada del carnet (Estilo Premium 3D / Wallet) fiel al físico
@Composable
fun OfficialPhysicalCardPass2D(data: AbonoData, onClick: () -> Unit = {}) {
    if (data.barcodeNumber.isBlank()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.60f)
                .clickable { onClick() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            EmptyPasePlaceholder()
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.60f)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        // Gris claro para simular el plástico y destacar el blanco del código de barras
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        // En lugar de un padding global, usamos padding por secciones para que el rojo llegue al borde
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. CABECERA (LOGO 26/27) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "26", fontWeight = FontWeight.Black, fontSize = 42.sp, color = Color(0xFF1E293B))
                    Text(text = "⚡", fontSize = 42.sp, color = RayoRed)
                    Text(text = "27", fontWeight = FontWeight.Black, fontSize = 42.sp, color = Color(0xFF1E293B))
                }
                Text(
                    text = "ABONO RAYO VALLECANO DE MADRID",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.Gray
                )
            }

            // --- 2. DATOS DEL ABONADO (Alineados a la izquierda como el original) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "ABONADO Nº : ${data.abonadoNo.ifBlank { "----" }}",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.nombre.uppercase().ifBlank { "NOMBRE" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = data.apellidos.uppercase().ifBlank { "APELLIDOS" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PUERTAS : ${data.puertas.ifBlank { "--" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = data.grada.uppercase().ifBlank { "SECTOR" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Fila de Fila y Asiento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "FILA: ${data.fila.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = "ASIENTO: ${data.asiento.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Fila de Categoría y Código de socio (Adulto / U03CCA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = data.categoria.uppercase().ifBlank { "ADULTO" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = data.codigoSocio.uppercase().ifBlank { "------" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                }
            }

            // --- 3. FRANJA ROJA (Ocupa el 100% del ancho) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RayoRed)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.temporada.uppercase().ifBlank { "TEMPORADA 2026/2027" },
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // --- 4. CÓDIGO DE BARRAS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BarcodeImage(
                    text = data.barcodeNumber,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(64.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = data.sponsorText.uppercase().ifBlank { "DIGI" },
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    color = Color(0xFF0F265C)
                )
            }
        }
    }
}

@Composable
fun EmptyPasePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tu Pase 2D no está listo",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pulsa el botón de abajo y sube la foto delantera de tu carnet. La app extraerá tus datos al instante.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EditAbonoDialog(
    data: AbonoData,
    onDismiss: () -> Unit,
    onSave: (AbonoData) -> Unit
) {
    var abonadoNo by remember { mutableStateOf(data.abonadoNo) }
    var nombre by remember { mutableStateOf(data.nombre) }
    var apellidos by remember { mutableStateOf(data.apellidos) }
    var puertas by remember { mutableStateOf(data.puertas) }
    var grada by remember { mutableStateOf(data.grada) }
    var fila by remember { mutableStateOf(data.fila) }
    var asiento by remember { mutableStateOf(data.asiento) }
    var categoria by remember { mutableStateOf(data.categoria) }
    var codigoSocio by remember { mutableStateOf(data.codigoSocio) }
    var temporada by remember { mutableStateOf(data.temporada) }
    var barcodeNumber by remember { mutableStateOf(data.barcodeNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Editar Datos", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = abonadoNo, onValueChange = { abonadoNo = it }, label = { Text("Abonado Nº") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = apellidos, onValueChange = { apellidos = it }, label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = puertas, onValueChange = { puertas = it }, label = { Text("Puertas") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = grada, onValueChange = { grada = it }, label = { Text("Grada") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = fila, onValueChange = { fila = it }, label = { Text("Fila") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = asiento, onValueChange = { asiento = it }, label = { Text("Asiento") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = codigoSocio, onValueChange = { codigoSocio = it }, label = { Text("Código Socio") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = temporada, onValueChange = { temporada = it }, label = { Text("Temporada") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = barcodeNumber, onValueChange = { barcodeNumber = it }, label = { Text("Número de Código de Barras") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        data.copy(
                            abonadoNo = abonadoNo, nombre = nombre, apellidos = apellidos,
                            puertas = puertas, grada = grada, fila = fila, asiento = asiento,
                            categoria = categoria, codigoSocio = codigoSocio, temporada = temporada,
                            barcodeNumber = barcodeNumber
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RayoRed)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun FullScreenAbonoDialog(
    data: AbonoData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Este efecto sube el brillo al máximo al abrirse y lo restaura al cerrarse
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness

        window?.attributes = window?.attributes?.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }

        onDispose {
            window?.attributes = window?.attributes?.apply {
                screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Permite ocupar toda la pantalla
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.90f)) // Fondo oscuro casi opaco
                .clickable { onDismiss() }, // Si tocas fuera o dentro, se cierra
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Reutilizamos tu diseño exacto, pero solo para mostrar
                OfficialPhysicalCardPass2D(data = data, onClick = onDismiss)
            }
        }
    }
}