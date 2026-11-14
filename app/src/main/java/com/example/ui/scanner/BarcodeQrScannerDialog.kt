package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.pick
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun BarcodeQrScannerDialog(
    language: AppLanguage,
    titleBn: String = "বারকোড / QR কোড স্ক্যান",
    titleEn: String = "Scan Barcode / QR Code",
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualCode by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = language.pick(titleBn, titleEn),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("scanner_close_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close scanner")
                    }
                }

                if (hasPermission) {
                    CameraBarcodePreview(
                        torchEnabled = torchEnabled,
                        onToggleTorch = { torchEnabled = !torchEnabled },
                        onDetected = { code ->
                            onCodeScanned(code)
                            onDismiss()
                        }
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = language.pick(
                                    "স্ক্যান করতে ক্যামেরা পারমিশন দিন বা নিচে কোড লিখুন",
                                    "Grant camera permission to scan or enter code manually below"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("request_camera_permission_button")
                            ) {
                                Text(language.pick("ক্যামেরা চালু করুন", "Allow Camera"))
                            }
                        }
                    }
                }

                Text(
                    text = language.pick(
                        "অথবা বারকোড / QR কোড হাতে লিখুন:",
                        "Or enter Barcode / QR value manually:"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = { Text("e.g. 8901234567890 / FST:...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_manual_input")
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                onCodeScanned(manualCode.trim())
                                onDismiss()
                            }
                        },
                        enabled = manualCode.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("scanner_manual_submit")
                    ) {
                        Text(language.pick("যোগ", "Use"))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraBarcodePreview(
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var alreadyDelivered by remember { mutableStateOf(false) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    LaunchedEffect(torchEnabled) {
        try {
            cameraController.enableTorch(torchEnabled)
        } catch (_: Exception) {
        }
    }

    DisposableEffect(lifecycleOwner, cameraController) {
        cameraController.setImageAnalysisAnalyzer(
            mainExecutor,
            MlKitAnalyzer(
                listOf(barcodeScanner),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainExecutor
            ) { result: MlKitAnalyzer.Result? ->
                if (alreadyDelivered) return@MlKitAnalyzer
                val barcodes = result?.getValue(barcodeScanner)
                val firstValue = barcodes?.firstOrNull()?.rawValue?.trim()
                if (!firstValue.isNullOrBlank()) {
                    alreadyDelivered = true
                    onDetected(firstValue)
                }
            }
        )
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            barcodeScanner.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameW = size.width * 0.72f
            val frameH = size.height * 0.54f
            val left = (size.width - frameW) / 2f
            val top = (size.height - frameH) / 2f
            drawRoundRect(
                color = Color(0xFF66DBA9),
                topLeft = Offset(left, top),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 5f)
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Toggle flash",
                    tint = Color.White
                )
            }
        }
    }
}
