package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar
import com.example.scanner.BankCardTextParser
import com.example.scanner.ScannedBankCard
import com.example.ui.components.IranianBankLogo
import com.example.ui.theme.AmberBright
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalGetImage::class)
@Composable
fun BankCardScannerDialog(
    customBanks: List<IranianBank>,
    onDismiss: () -> Unit,
    onCardScanned: (ScannedBankCard) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }
    var detectedCard by remember { mutableStateOf<ScannedBankCard?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            permissionDeniedOnce = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            runCatching {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }.onFailure {
                permissionDeniedOnce = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF071A18))
        ) {
            if (!hasCameraPermission) {
                // Camera Permission Rationale / Fallback UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AmberBright.copy(alpha = 0.16f),
                        modifier = Modifier.size(74.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = AmberBright,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "اسکن هوشمند کارت بانکی",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "برای خواندن خودکار شماره ۱۶ رقمی کارت و تشخیص نام بانک به صورت کاملاً آفلاین، دسترسی دوربین لازم است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            runCatching {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }.onFailure {
                                permissionDeniedOnce = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("grant_camera_permission_button")
                    ) {
                        Text("اعطای دسترسی دوربین")
                    }
                    if (permissionDeniedOnce) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("باز کردن تنظیمات دوربین", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("بستن", color = Color.White.copy(alpha = 0.85f))
                    }
                }
            } else {
                // Live CameraX Preview + On-Device ML Kit OCR Analyzer
                val previewView = remember {
                    PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }

                DisposableEffect(lifecycleOwner, customBanks) {
                    val mainExecutor = ContextCompat.getMainExecutor(context)
                    val analyzerExecutor = Executors.newSingleThreadExecutor()
                    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val lastAnalyzedMs = AtomicLong(0L)
                    var cameraProviderRef: ProcessCameraProvider? = null
                    var disposed = false

                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        if (disposed) return@addListener
                        try {
                            val cameraProvider = providerFuture.get()
                            cameraProviderRef = cameraProvider

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                val now = System.currentTimeMillis()
                                val mediaImage = imageProxy.image
                                if (mediaImage == null || now - lastAnalyzedMs.get() < 320L) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                lastAnalyzedMs.set(now)

                                try {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val parsed = BankCardTextParser.parse(
                                                rawText = visionText.text,
                                                customBanks = customBanks
                                            )
                                            if (parsed != null) {
                                                detectedCard = BankCardTextParser.merge(
                                                    previous = detectedCard,
                                                    incoming = parsed,
                                                    customBanks = customBanks
                                                )
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } catch (t: Throwable) {
                                    Log.w("BankCardScanner", "Frame analysis skipped", t)
                                    imageProxy.close()
                                }
                            }

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            boundCamera = camera
                            cameraError = null
                        } catch (t: Throwable) {
                            Log.e("BankCardScanner", "Camera binding failed", t)
                            cameraError = "امکان راه‌اندازی دوربین وجود نداشت. می‌توانید شماره کارت و شبا را دستی وارد کنید."
                        }
                    }, mainExecutor)

                    onDispose {
                        disposed = true
                        runCatching { boundCamera?.cameraControl?.enableTorch(false) }
                        runCatching { cameraProviderRef?.unbindAll() }
                        runCatching { textRecognizer.close() }
                        runCatching { analyzerExecutor.shutdown() }
                        boundCamera = null
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Golden bank-card framing overlay (spacious enough for horizontal or vertical 4-line cards + Sheba)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameW = size.width * 0.88f
                    val frameH = (frameW / 1.48f).coerceAtMost(size.height * 0.44f)
                    val left = (size.width - frameW) / 2f
                    val top = (size.height - frameH) / 2.45f

                    drawRect(
                        color = Color.Black.copy(alpha = 0.54f),
                        topLeft = Offset.Zero,
                        size = Size(size.width, top)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.54f),
                        topLeft = Offset(0f, top + frameH),
                        size = Size(size.width, size.height - (top + frameH))
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.54f),
                        topLeft = Offset(0f, top),
                        size = Size(left, frameH)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.54f),
                        topLeft = Offset(left + frameW, top),
                        size = Size(size.width - (left + frameW), frameH)
                    )

                    val hasBoth = detectedCard?.cardNumber != null && detectedCard?.shebaNumber != null
                    val borderColor = when {
                        hasBoth -> Color(0xFF10B981)
                        detectedCard != null -> Color(0xFF34D399)
                        else -> AmberBright
                    }
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(left, top),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }

                // Top Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_card_scanner_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن اسکنر", tint = Color.White)
                        }
                    }

                    Text(
                        text = "اسکن شماره کارت و شبا",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        IconButton(
                            onClick = {
                                val next = !torchEnabled
                                torchEnabled = next
                                runCatching { boundCamera?.cameraControl?.enableTorch(next) }
                            },
                            modifier = Modifier.testTag("toggle_scanner_torch_button")
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = "فلش دوربین",
                                tint = if (torchEnabled) AmberBright else Color.White
                            )
                        }
                    }
                }

                // Bottom Result & Confirmation Panel
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFF0A2421).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, AmberBright.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live Dual Capture Status Chips (Card + Sheba)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScannerStatusBadge(
                                label = if (detectedCard?.cardNumber != null) "شماره کارت ✓" else "در انتظار شماره کارت",
                                captured = detectedCard?.cardNumber != null,
                                modifier = Modifier.weight(1f)
                            )
                            ScannerStatusBadge(
                                label = if (detectedCard?.shebaNumber != null) "شماره شبا ✓" else "در انتظار شماره شبا",
                                captured = detectedCard?.shebaNumber != null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (cameraError != null) {
                            Text(
                                text = cameraError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFCA5A5),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (detectedCard != null) {
                            val card = detectedCard!!
                            val bank = card.detectedBank ?: IranianBanks.findByCode("CASH")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IranianBankLogo(bank = bank, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (card.detectedBank != null) bank.persianName else "اطلاعات کارت شناسایی شد",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (card.formattedCardNumber != null) {
                                        Text(
                                            text = "کارت: ${JalaliCalendar.toPersianDigits(card.formattedCardNumber)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberBright
                                        )
                                    }
                                    if (card.formattedShebaNumber != null) {
                                        Text(
                                            text = "شبا: ${card.formattedShebaNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF6EE7B7)
                                        )
                                    }
                                    if (card.expiryDate != null) {
                                        Text(
                                            text = "انقضا: ${JalaliCalendar.toPersianDigits(card.expiryDate)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            if (card.cardNumber == null || card.shebaNumber == null) {
                                Text(
                                    text = if (card.shebaNumber == null) {
                                        "می‌توانید همین الان تأیید کنید یا برای خواندن شماره شبا (IR...) دوربین را کمی نگه دارید."
                                    } else {
                                        "شماره شبا خوانده شد؛ می‌توانید همین الان تأیید کنید یا روی شماره کارت بگیرید."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { detectedCard = null },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("اسکن مجدد", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = { onCardScanned(card) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("confirm_scanned_card_button")
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    val btnLabel = when {
                                        card.cardNumber != null && card.shebaNumber != null -> "تأیید کارت و شبا"
                                        card.cardNumber != null -> "تأیید شماره کارت"
                                        else -> "تأیید شماره شبا"
                                    }
                                    Text(btnLabel, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = AmberBright,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "در حال جستجوی شماره کارت (افقی یا ۴ رقمی عمودی) و شماره شبا...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            Text(
                                text = "پشتیبانی از کارت‌های افقی، کارت‌های ۴ رقم در هر خط و شماره شبا (IR...) کاملاً آفلاین.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.62f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerStatusBadge(
    label: String,
    captured: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (captured) Color(0xFF10B981).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = if (captured) Color(0xFF34D399) else Color.White.copy(alpha = 0.18f)
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (captured) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
