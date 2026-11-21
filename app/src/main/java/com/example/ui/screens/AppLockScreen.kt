package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.JalaliCalendar
import com.example.ui.theme.AmberBright
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    securityQuestion: String,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onVerifyPin: (String) -> Boolean,
    onTriggerBiometric: () -> Unit,
    onRecoverWithSecurityAnswer: (answer: String, newPin: String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockedUntilMillis by remember { mutableLongStateOf(0L) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    val canUseBiometric = biometricEnabled && biometricAvailable

    // Auto-launch biometric prompt once when Lock Screen appears
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            delay(250)
            onTriggerBiometric()
        }
    }

    // Countdown timer if user hit 5 wrong attempts
    LaunchedEffect(lockedUntilMillis) {
        while (lockedUntilMillis > System.currentTimeMillis()) {
            cooldownSeconds = ((lockedUntilMillis - System.currentTimeMillis()) / 1000L).toInt() + 1
            delay(500)
        }
        cooldownSeconds = 0
    }

    fun appendDigit(d: String) {
        if (cooldownSeconds > 0 || enteredPin.length >= 4) return
        errorMessage = null
        val updated = enteredPin + d
        enteredPin = updated
        if (updated.length == 4) {
            if (onVerifyPin(updated)) {
                enteredPin = ""
                failedAttempts = 0
            } else {
                failedAttempts++
                enteredPin = ""
                if (failedAttempts >= 5) {
                    lockedUntilMillis = System.currentTimeMillis() + 30_000L
                    failedAttempts = 0
                    errorMessage = "تلاش ناموفق بیش از حد مجاز. لطفاً ۳۰ ثانیه صبر کنید."
                } else {
                    errorMessage = "رمز عبور نادرست است. دوباره تلاش کنید."
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EmeraldDeep,
                        Color(0xFF062C29),
                        Color(0xFF041C1A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Minimal Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = EmeraldDeep,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(76.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_minimal_1795251116863),
                        contentDescription = "لوگوی حسابدار من",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "حسابدار من",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (cooldownSeconds > 0) {
                        "قفل موقت (${JalaliCalendar.toPersianDigits(cooldownSeconds)} ثانیه)"
                    } else {
                        "رمز ۴ رقمی خود را وارد کنید"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )

                Spacer(Modifier.height(24.dp))

                // 4 Animated PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val filled = index < enteredPin.length
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                errorMessage != null -> Color(0xFFF87171)
                                filled -> AmberBright
                                else -> Color.White.copy(alpha = 0.28f)
                            },
                            label = "pin_dot_color"
                        )
                        val dotSize by animateDpAsState(
                            targetValue = if (filled) 18.dp else 14.dp,
                            label = "pin_dot_size"
                        )
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = errorMessage ?: " ",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFCA5A5),
                    textAlign = TextAlign.Center
                )
            }

            // Custom Luxury Numeric Keypad (RTL-safe standard phone layout 1-2-3 / 4-5-6 / 7-8-9)
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val rows = listOf(
                    listOf("3", "2", "1"),
                    listOf("6", "5", "4"),
                    listOf("9", "8", "7")
                )
                rows.forEach { rowDigits ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowDigits.forEach { digit ->
                            KeypadDigitButton(
                                digit = digit,
                                enabled = cooldownSeconds == 0,
                                onClick = { appendDigit(digit) }
                            )
                        }
                    }
                }

                // Bottom row: Backspace | 0 | Fingerprint
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Backspace button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                errorMessage = null
                            }
                            .testTag("pin_keypad_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "پاک کردن رقم",
                            tint = Color.White.copy(alpha = if (enteredPin.isNotEmpty()) 0.9f else 0.35f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    KeypadDigitButton(
                        digit = "0",
                        enabled = cooldownSeconds == 0,
                        onClick = { appendDigit("0") }
                    )

                    // Fingerprint button (or spacer if not enabled)
                    if (canUseBiometric) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(EmeraldBright.copy(alpha = 0.22f))
                                .border(1.dp, EmeraldBright.copy(alpha = 0.55f), CircleShape)
                                .clickable(onClick = onTriggerBiometric)
                                .testTag("pin_keypad_fingerprint"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = "ورود با اثر انگشت",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(72.dp))
                    }
                }
            }

            // Forgot PIN -> Security Question Recovery Button
            TextButton(
                onClick = { showRecoveryDialog = true },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("forgot_pin_recovery_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.LockReset,
                    contentDescription = null,
                    tint = AmberBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "فراموشی رمز؟ بازیابی با سوال امنیتی",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberBright
                )
            }
        }
    }

    if (showRecoveryDialog) {
        SecurityQuestionRecoveryDialog(
            securityQuestion = securityQuestion,
            onDismiss = { showRecoveryDialog = false },
            onRecover = { answer, newPin ->
                val ok = onRecoverWithSecurityAnswer(answer, newPin)
                if (ok) {
                    showRecoveryDialog = false
                }
                ok
            }
        )
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.11f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("pin_key_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = JalaliCalendar.toPersianDigits(digit),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SecurityQuestionRecoveryDialog(
    securityQuestion: String,
    onDismiss: () -> Unit,
    onRecover: (answer: String, newPin: String) -> Boolean
) {
    var answer by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بازیابی رمز با سوال امنیتی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = securityQuestion.ifBlank { "سوال امنیتی ثبت شده شما" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        error = null
                    },
                    label = { Text("پاسخ سوال امنیتی") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_answer_input")
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { raw ->
                        newPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز ۴ رقمی جدید") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_new_pin_input")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { raw ->
                        confirmPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("تکرار رمز جدید") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        answer.isBlank() -> error = "پاسخ سوال امنیتی را وارد کنید"
                        newPin.length != 4 -> error = "رمز جدید باید دقیقاً ۴ رقم باشد"
                        newPin != confirmPin -> error = "تکرار رمز جدید مطابقت ندارد"
                        else -> {
                            val ok = onRecover(answer, newPin)
                            if (!ok) {
                                error = "پاسخ سوال امنیتی صحیح نیست"
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("recovery_submit_button")
            ) {
                Text("تغییر رمز و ورود")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
