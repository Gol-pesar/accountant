package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.JalaliCalendar
import com.example.security.SecurityCrypto

@Composable
fun SetupPinLockDialog(
    biometricAvailable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (pin: String, question: String, answer: String, enableBiometric: Boolean) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedQuestion by remember { mutableStateOf(SecurityCrypto.PRESET_SECURITY_QUESTIONS.first()) }
    var useCustomQuestion by remember { mutableStateOf(false) }
    var customQuestion by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var enableBio by remember { mutableStateOf(biometricAvailable) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم رمز عبور و سوال امنیتی") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { raw ->
                        pin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز ۴ رقمی") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { raw ->
                        confirmPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("تکرار رمز ۴ رقمی") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_confirm_pin_input")
                )

                Text(
                    text = "سوال امنیتی (برای بازیابی در صورت فراموشی رمز)",
                    style = MaterialTheme.typography.labelMedium
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SecurityCrypto.PRESET_SECURITY_QUESTIONS) { q ->
                        FilterChip(
                            selected = !useCustomQuestion && selectedQuestion == q,
                            onClick = {
                                useCustomQuestion = false
                                selectedQuestion = q
                            },
                            label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = useCustomQuestion,
                            onClick = { useCustomQuestion = true },
                            label = { Text("سوال دلخواه...", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (useCustomQuestion) {
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = {
                            customQuestion = it
                            error = null
                        },
                        label = { Text("متن سوال امنیتی دلخواه") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                        .testTag("setup_security_answer_input")
                )

                if (biometricAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فعال‌سازی ورود با اثر انگشت",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = enableBio, onCheckedChange = { enableBio = it })
                    }
                }

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
                    val finalQuestion = if (useCustomQuestion) customQuestion.trim() else selectedQuestion
                    when {
                        pin.length != 4 -> error = "رمز عبور باید دقیقاً ۴ رقم باشد"
                        pin != confirmPin -> error = "رمز عبور و تکرار آن یکسان نیستند"
                        finalQuestion.length < 4 -> error = "لطفاً یک سوال امنیتی معتبر انتخاب یا وارد کنید"
                        answer.trim().length < 2 -> error = "پاسخ سوال امنیتی باید حداقل ۲ حرف باشد"
                        else -> onConfirm(pin, finalQuestion, answer.trim(), enableBio && biometricAvailable)
                    }
                },
                modifier = Modifier.testTag("save_pin_lock_button")
            ) {
                Text("فعال‌سازی قفل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun ChangePinDialog(
    onVerifyCurrentPin: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSaveNewPin: (String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر رمز ۴ رقمی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { raw ->
                        currentPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز فعلی") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { raw ->
                        newPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز جدید (۴ رقم)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    Text(error!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        !onVerifyCurrentPin(currentPin) -> error = "رمز فعلی اشتباه است"
                        newPin.length != 4 -> error = "رمز جدید باید ۴ رقم باشد"
                        newPin != confirmPin -> error = "تکرار رمز جدید مطابقت ندارد"
                        else -> onSaveNewPin(newPin)
                    }
                }
            ) {
                Text("ذخیره رمز جدید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun DisablePinConfirmDialog(
    onVerifyCurrentPin: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirmedDisable: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("غیرفعال کردن قفل برنامه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("برای حذف رمز عبور و اثر انگشت، رمز ۴ رقمی فعلی را وارد کنید:")
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { raw ->
                        currentPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز فعلی") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerifyCurrentPin(currentPin)) {
                        onConfirmedDisable()
                    } else {
                        error = "رمز وارد شده نادرست است"
                    }
                }
            ) {
                Text("حذف قفل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun UpdateSecurityQuestionDialog(
    currentQuestion: String,
    onVerifyCurrentPin: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (question: String, answer: String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(currentQuestion.ifBlank { SecurityCrypto.PRESET_SECURITY_QUESTIONS.first() }) }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر سوال امنیتی") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { raw ->
                        currentPin = JalaliCalendar.toLatinDigits(raw).filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز فعلی (برای تأیید)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SecurityCrypto.PRESET_SECURITY_QUESTIONS) { q ->
                        FilterChip(
                            selected = question == q,
                            onClick = { question = q },
                            label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        question = it
                        error = null
                    },
                    label = { Text("متن سوال امنیتی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        error = null
                    },
                    label = { Text("پاسخ جدید") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        !onVerifyCurrentPin(currentPin) -> error = "رمز فعلی اشتباه است"
                        question.trim().length < 4 -> error = "سوال امنیتی معتبر وارد کنید"
                        answer.trim().length < 2 -> error = "پاسخ سوال امنیتی باید حداقل ۲ حرف باشد"
                        else -> onSave(question.trim(), answer.trim())
                    }
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
