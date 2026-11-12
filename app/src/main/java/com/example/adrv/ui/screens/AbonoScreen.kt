package com.example.adrv.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.adrv.data.AbonoStorage
import com.example.adrv.data.BarcodeAnalyzer
import com.example.adrv.model.AbonoData
import com.example.adrv.ui.components.BarcodeImage
import com.example.adrv.ui.components.bounceClick
import com.example.adrv.ui.theme.RayoRed
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbonoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storage = remember { AbonoStorage(context) }
    var abonoData by remember { mutableStateOf(storage.loadAbonoData()) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showFullScreenAbono by remember { mutableStateOf(false) }
    var showSourceSelector by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }

    fun processImageUri(uri: Uri) {
        val path = storage.savePhotoFromUri(uri, isFront = true)
        if (path != null) {
            abonoData = abonoData.copy(frontPhotoPath = path)
            Toast.makeText(context, "Escaneando datos del abono...", Toast.LENGTH_SHORT).show()

            BarcodeAnalyzer.analyzeImage(
                context = context,
                imageUri = uri,
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { processImageUri(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraScanner = true
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara para escanear el abono", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (abonoData.barcodeNumber.isBlank()) {
            Spacer(modifier = Modifier.height(32.dp))
            AbonoEmptyState(
                onScanClick = { showSourceSelector = true }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showSourceSelector = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Repetir Foto", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Datos", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            OfficialPhysicalCardPass2D(
                data = abonoData,
                onClick = { showFullScreenAbono = true }
            )
        }
    }

    if (showSourceSelector) {
        ModalBottomSheet(onDismissRequest = { showSourceSelector = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("¿Cómo quieres subir tu abono?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showSourceSelector = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    showCameraScanner = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = RayoRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Usar Cámara", fontWeight = FontWeight.Medium)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showSourceSelector = false
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Abrir Galería", fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCameraScanner) {
        CameraScannerDialog(
            onImageCaptured = { uri ->
                showCameraScanner = false
                processImageUri(uri)
            },
            onDismiss = { showCameraScanner = false }
        )
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

@Composable
fun AbonoEmptyState(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.60f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tu Pase 2D no está listo",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pulsa el botón de abajo y encuadra la foto delantera de tu carnet. La app extraerá tus datos al instante.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = RayoRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
                    .bounceClick {}
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escanear Abono", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
            text = "Pulsa el botón de abajo y sube la foto delantera de tu carnet.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

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

    var isPressed by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0.5f) }
    var touchY by remember { mutableFloatStateOf(0.5f) }

    val rotationX by animateFloatAsState(
        targetValue = if (isPressed) (touchY - 0.5f) * -25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotX"
    )
    val rotationY by animateFloatAsState(
        targetValue = if (isPressed) (touchX - 0.5f) * 25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotY"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val glareAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "glare"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.60f)
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                this.cameraDistance = 16f * density
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    var moved = false
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()

                    touchX = (down.position.x / width).coerceIn(0f, 1f)
                    touchY = (down.position.y / height).coerceIn(0f, 1f)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        if (change.positionChange().getDistance() > 3f) moved = true

                        touchX = (change.position.x / width).coerceIn(0f, 1f)
                        touchY = (change.position.y / height).coerceIn(0f, 1f)

                    } while (event.changes.any { it.pressed })

                    isPressed = false
                    if (!moved) {
                        onClick()
                    }
                }
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(text = "ABONADO Nº : ${data.abonadoNo.ifBlank { "----" }}", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = data.nombre.uppercase().ifBlank { "NOMBRE" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = data.apellidos.uppercase().ifBlank { "APELLIDOS" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "PUERTAS : ${data.puertas.ifBlank { "--" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = data.grada.uppercase().ifBlank { "SECTOR" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "FILA: ${data.fila.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        Text(text = "ASIENTO: ${data.asiento.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = data.categoria.uppercase().ifBlank { "ADULTO" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        Text(text = data.codigoSocio.uppercase().ifBlank { "------" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    }
                }

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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BarcodeImage(
                        text = data.barcodeNumber,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(64.dp)
                    )
                }
            }

            if (glareAlpha > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gradientX = touchX * size.width
                    val gradientY = touchY * size.height

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = glareAlpha), Color.Transparent),
                            center = Offset(gradientX, gradientY),
                            radius = size.width * 0.9f
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CameraScannerDialog(onImageCaptured: (Uri) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("Camera", "Error al iniciar la cámara", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val cardHeight = canvasHeight * 0.65f
                val cardWidth = cardHeight * 0.63f
                val left = (canvasWidth - cardWidth) / 2
                val top = (canvasHeight - cardHeight) / 3

                val path = Path().apply {
                    addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, left + cardWidth, top + cardHeight),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    )
                    fillType = PathFillType.EvenOdd
                }

                drawPath(path, Color.Black.copy(alpha = 0.75f))
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            IconButton(onClick = onDismiss, modifier = Modifier.padding(32.dp).align(Alignment.TopStart)) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Text(
                text = "Encuadra la parte frontal del abono\ndentro del marco",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(top = 220.dp)
            )

            Button(
                onClick = {
                    val photoFile = File.createTempFile("abono_scan", ".jpg", context.cacheDir)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = Uri.fromFile(photoFile)
                                ContextCompat.getMainExecutor(context).execute {
                                    onImageCaptured(savedUri)
                                }
                            }
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Camera", "Error al guardar foto: ${exc.message}", exc)
                            }
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp).size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = RayoRed)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Hacer Foto", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
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

// Versión estática y totalmente rígida del carnet, perfecta para el lector del torno
@Composable
fun StaticPhysicalCardPass2D(data: AbonoData, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.60f)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(text = "ABONADO Nº : ${data.abonadoNo.ifBlank { "----" }}", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = data.nombre.uppercase().ifBlank { "NOMBRE" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(text = data.apellidos.uppercase().ifBlank { "APELLIDOS" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "PUERTAS : ${data.puertas.ifBlank { "--" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(text = data.grada.uppercase().ifBlank { "SECTOR" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "FILA: ${data.fila.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = "ASIENTO: ${data.asiento.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = data.categoria.uppercase().ifBlank { "ADULTO" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(text = data.codigoSocio.uppercase().ifBlank { "------" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                }
            }

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BarcodeImage(
                    text = data.barcodeNumber,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(64.dp)
                )
            }
        }
    }
}
@Composable
fun FullScreenAbonoDialog(data: AbonoData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

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
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.90f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                // AQUÍ ESTÁ EL CAMBIO: Usamos la versión estática y rígida
                StaticPhysicalCardPass2D(data = data, onClick = onDismiss)
            }
        }
    }
}