package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToDrive
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.backup.DriveBackupManager
import com.example.data.local.BankAccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomBankEntity
import com.example.data.local.SavingsGoalEntity
import com.example.domain.JalaliCalendar
import com.example.security.BiometricAvailability
import com.example.ui.components.CategoryIcons
import com.example.ui.components.IranianBankCardTile
import com.example.ui.components.IranianBankLogo
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.AccountingUiState
import com.example.update.GitHubUpdateChecker

@Composable
fun SettingsMoreScreen(
    state: AccountingUiState,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onSaveBankAccount: (BankAccountEntity) -> Unit,
    onDeleteBankAccount: (BankAccountEntity) -> Unit,
    onSaveCustomBank: (CustomBankEntity) -> Unit,
    onDeleteCustomBank: (CustomBankEntity) -> Unit,
    onSaveSavingsGoal: (SavingsGoalEntity) -> Unit,
    onAddGoalContribution: (SavingsGoalEntity, Long) -> Unit,
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit,
    onSetShowInRial: (Boolean) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onSetAutoApproveSms: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onRequestAutoRestore: () -> Unit,
    onTriggerAutoBackupNow: () -> Unit,
    onRequestDriveBackup: () -> Unit,
    onRequestDriveRestore: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
    biometricAvailability: BiometricAvailability,
    onVerifyCurrentPin: (String) -> Boolean,
    onEnablePinLock: (pin: String, question: String, answer: String, enableBiometric: Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onUpdateSecurityQuestion: (String, String) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onDisablePinLock: () -> Unit,
    onLockAppNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showRial = state.preferences.showInRial

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    var showBankDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankAccountEntity?>(null) }
    var openScannerInBankDialog by remember { mutableStateOf(false) }

    var showCustomBankDialog by remember { mutableStateOf(false) }
    var editingCustomBank by remember { mutableStateOf<CustomBankEntity?>(null) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    var confirmRestoreDialog by remember { mutableStateOf(false) }

    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showUpdateQuestionDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "تنظیمات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Core App Settings (FIRST — تنظیمات اصلی)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    PreferenceToggleRow(
                        icon = Icons.Rounded.MonetizationOn,
                        title = "نمایش مبالغ به ریال",
                        subtitle = "در حالت پیش‌فرض مبالغ به تومان نمایش داده می‌شوند",
                        checked = state.preferences.showInRial,
                        onCheckedChange = onSetShowInRial
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    PreferenceToggleRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "حالت شب (تاریک)",
                        subtitle = "پوسته تیره زمردی برای استفاده راحت‌تر در شب",
                        checked = state.preferences.darkMode,
                        onCheckedChange = onSetDarkMode
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    PreferenceToggleRow(
                        icon = Icons.Rounded.Sms,
                        title = "خواندن خودکار پیامک‌های بانکی جدید",
                        subtitle = "در ورود اول فقط موجودی تراز ثبت می‌شود و پیامک‌های جدید برای تعیین جزئیات در صف قرار می‌گیرند",
                        checked = state.preferences.autoApproveSms,
                        onCheckedChange = onSetAutoApproveSms
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اعلان فوری تعیین شرح تراکنش",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (state.hasNotificationPermission) {
                                    "فعال است — هر پیامک بانکی بلافاصله در اعلانات نمایش داده می‌شود"
                                } else {
                                    "غیرفعال است — برای نمایش فوری پیامک‌های بانکی دسترسی اعلانات را فعال کنید"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (state.hasNotificationPermission) {
                            FilledTonalButton(
                                onClick = onSendTestNotification,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("settings_test_notification_button")
                            ) {
                                Text("تست اعلان", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            Button(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("settings_request_notification_button")
                            ) {
                                Text("فعال‌سازی", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // 2. Security, Optional PIN Lock, Security Question & Biometric Fingerprint Card
        item {
            val pinEnabled = state.preferences.pinLockEnabled
            val bioAvailable = biometricAvailability == BiometricAvailability.AVAILABLE
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "امنیت، رمز عبور و اثر انگشت",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (pinEnabled) {
                                    "قفل برنامه فعال است (با بازیابی سوال امنیتی)"
                                } else {
                                    "در صورت تمایل برای برنامه رمز ۴ رقمی و اثر انگشت تنظیم کنید"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pinEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable) {
                                    showSetupPinDialog = true
                                } else {
                                    showDisablePinDialog = true
                                }
                            },
                            modifier = Modifier.testTag("toggle_pin_lock_switch")
                        )
                    }

                    if (pinEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ورود سریع با اثر انگشت",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when (biometricAvailability) {
                                        BiometricAvailability.AVAILABLE -> "باز کردن قفل برنامه با حسگر اثر انگشت گوشی"
                                        BiometricAvailability.NOT_ENROLLED -> "ابتدا در تنظیمات اندروید اثر انگشت ثبت کنید"
                                        BiometricAvailability.UNAVAILABLE -> "حسگر بیومتریک روی این دستگاه در دسترس نیست"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.preferences.biometricEnabled && bioAvailable,
                                enabled = bioAvailable,
                                onCheckedChange = onSetBiometricEnabled,
                                modifier = Modifier.testTag("toggle_biometric_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showChangePinDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تغییر رمز", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = { showUpdateQuestionDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("سوال امنیتی", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        FilledTonalButton(
                            onClick = onLockAppNow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_app_now_button")
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("قفل فوری برنامه (تست ورود)")
                        }
                    }
                }
            }
        }

        // 3. Real-Time Auto-Backup ("من را حذف نکنید") + 1-Click Auto Restore + Manual Restore & Google Drive
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FolderCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پشتیبان‌گیری لحظه‌ای («${DriveBackupManager.AUTO_BACKUP_FOLDER_NAME}»)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "هر تغییری بلافاصله در پوشه Documents / ${DriveBackupManager.AUTO_BACKUP_FOLDER_NAME} ذخیره می‌شود و با حذف برنامه پاک نمی‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    if (state.preferences.lastAutoBackupMillis > 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "آخرین ذخیره لحظه‌ای: ${JalaliCalendar.fromEpochMillis(state.preferences.lastAutoBackupMillis).formatFull()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Primary restore buttons: 1-Click Auto Restore & Manual File Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRequestAutoRestore,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auto_folder_restore_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("بازگردانی خودکار", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { confirmRestoreDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drive_restore_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("بازگردانی دستی", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Secondary actions: Force instant save to folder & Export to Google Drive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onTriggerAutoBackupNow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auto_folder_backup_now_button")
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ذخیره فوری در پوشه", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(
                            onClick = onRequestDriveBackup,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drive_backup_button")
                        ) {
                            Icon(Icons.Rounded.AddToDrive, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("بک‌آپ گوگل درایو", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (state.preferences.lastBackupMillis > 0L) {
                        Text(
                            text = "آخرین بک‌آپ گوگل درایو: ${JalaliCalendar.fromEpochMillis(state.preferences.lastBackupMillis).formatFull()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // 4. App Version & GitHub Releases Update Checker Card
        item {
            val update = state.availableUpdate
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (update != null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (update != null) "نسخه جدید آماده نصب است!" else "نسخه و به‌روزرسانی برنامه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "نسخه فعلی: ${JalaliCalendar.toPersianDigits(BuildConfig.VERSION_NAME)} (بیلد ${JalaliCalendar.toPersianDigits(BuildConfig.VERSION_CODE)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (update != null) {
                        Text(
                            text = "نسخه جدید منتشرشده: ${update.title}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = { onOpenUrl(update.apkDownloadUrl ?: update.releasePageUrl) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_download_update_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("دانلود و نصب نسخه جدید")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCheckForUpdate,
                            enabled = !state.isCheckingUpdate,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_github_update_button")
                        ) {
                            if (state.isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("در حال بررسی...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Text("بررسی به‌روزرسانی", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        TextButton(
                            onClick = { onOpenUrl(GitHubUpdateChecker.RELEASES_PAGE_URL) },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("صفحه انتشارها", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 5. Bank Cards Management + Scanner + Built-in & Custom Banks Directory
        item {
            SectionHeader(
                title = "کارت‌های بانکی شما",
                actionLabel = "+ افزودن کارت",
                onAction = {
                    editingBank = null
                    openScannerInBankDialog = false
                    showBankDialog = true
                }
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        editingBank = null
                        openScannerInBankDialog = true
                        showBankDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_scan_card_button")
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اسکن کارت با دوربین", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = {
                        editingCustomBank = null
                        showCustomBankDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_add_custom_bank_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("افزودن بانک جدید", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(10.dp))

            if (state.bankAccounts.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.bankAccounts, key = { it.id }) { acc ->
                        IranianBankCardTile(
                            account = acc,
                            showInRial = showRial,
                            onClick = {
                                editingBank = acc
                                openScannerInBankDialog = false
                                showBankDialog = true
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Directory of all supported Iranian + user-defined custom banks
            val displayBanks = remember(state.allBanks) {
                state.allBanks.filter { it.code != "CASH" }
            }
            Text(
                text = "بانک‌های پشتیبانی‌شده (${JalaliCalendar.toPersianDigits(displayBanks.size)} بانک — برای ویرایش بانک‌های دلخواه روی آن‌ها ضربه بزنید)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "add_custom_bank_tile") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .width(82.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                editingCustomBank = null
                                showCustomBankDialog = true
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "افزودن بانک جدید",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "بانک جدید",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
                items(displayBanks, key = { it.code }) { bank ->
                    val customEntity = if (bank.isCustom) {
                        state.customBankEntities.firstOrNull { it.code == bank.code }
                    } else null
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .width(82.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (customEntity != null) {
                                    editingCustomBank = customEntity
                                    showCustomBankDialog = true
                                } else {
                                    editingBank = BankAccountEntity(bankCode = bank.code)
                                    openScannerInBankDialog = false
                                    showBankDialog = true
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IranianBankLogo(bank = bank, size = 38.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = bank.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 6. Savings Goals (قلک هوشمند)
        item {
            SectionHeader(
                title = "قلک و اهداف پس‌انداز",
                actionLabel = "+ هدف جدید",
                onAction = {
                    editingGoal = null
                    showGoalDialog = true
                }
            )
            Spacer(Modifier.height(8.dp))
            if (state.savingsGoals.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            editingGoal = null
                            showGoalDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "برای خریدها یا سفرهای آینده قلک بسازید و پیشرفت آن را دنبال کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.savingsGoals.forEach { goal ->
                        val ratio = if (goal.targetAmountToman > 0L) {
                            (goal.savedAmountToman.toFloat() / goal.targetAmountToman.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    editingGoal = goal
                                    showGoalDialog = true
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    FilledTonalButton(
                                        onClick = { onAddGoalContribution(goal, 500_000L) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("+ ۵۰۰ هزار", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = Color(goal.colorHex)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = JalaliCalendar.formatCurrency(goal.savedAmountToman, showRial),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "هدف: ${JalaliCalendar.formatCurrency(goal.targetAmountToman, showRial)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Customizable Categories Section (LAST — آخر تنظیمات)
        item {
            SectionHeader(
                title = "دسته‌بندی‌های قابل تنظیم (${JalaliCalendar.toPersianDigits(state.categories.size)})",
                actionLabel = "+ دسته جدید",
                onAction = {
                    editingCategory = null
                    showCategoryDialog = true
                }
            )
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.forEachIndexed { idx, cat ->
                        val catColor = Color(cat.colorHex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    editingCategory = cat
                                    showCategoryDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(catColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.resolve(cat.iconKey),
                                    contentDescription = cat.name,
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = buildString {
                                        append(if (cat.isIncome) "درآمد" else "هزینه")
                                        if (!cat.isIncome && cat.monthlyBudgetToman > 0L) {
                                            append(" • بودجه: ")
                                            append(JalaliCalendar.formatCurrency(cat.monthlyBudgetToman, showRial))
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "ویرایش",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (idx < state.categories.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showCategoryDialog) {
        CategoryEditorDialog(
            initial = editingCategory,
            showInRial = showRial,
            onDismiss = { showCategoryDialog = false },
            onSave = { cat ->
                onSaveCategory(cat)
                showCategoryDialog = false
            },
            onDelete = { cat ->
                onDeleteCategory(cat)
                showCategoryDialog = false
            }
        )
    }

    if (showBankDialog) {
        BankAccountEditorDialog(
            initial = editingBank,
            allBanks = state.allBanks,
            customBanks = state.customBanks,
            showInRial = showRial,
            openScannerImmediately = openScannerInBankDialog,
            onRequestAddCustomBank = {
                editingCustomBank = null
                showCustomBankDialog = true
            },
            onDismiss = {
                showBankDialog = false
                openScannerInBankDialog = false
            },
            onSave = { acc ->
                onSaveBankAccount(acc)
                showBankDialog = false
                openScannerInBankDialog = false
            },
            onDelete = if (editingBank != null && (editingBank?.id ?: 0L) != 0L) {
                { acc ->
                    onDeleteBankAccount(acc)
                    showBankDialog = false
                    openScannerInBankDialog = false
                }
            } else null
        )
    }

    if (showCustomBankDialog) {
        CustomBankEditorDialog(
            initial = editingCustomBank,
            onDismiss = { showCustomBankDialog = false },
            onSave = { customBank ->
                onSaveCustomBank(customBank)
                showCustomBankDialog = false
            },
            onDelete = if (editingCustomBank != null) {
                { customBank ->
                    onDeleteCustomBank(customBank)
                    showCustomBankDialog = false
                }
            } else null
        )
    }

    if (showGoalDialog) {
        SavingsGoalDialog(
            initial = editingGoal,
            showInRial = showRial,
            onDismiss = { showGoalDialog = false },
            onSave = { goal ->
                onSaveSavingsGoal(goal)
                showGoalDialog = false
            },
            onDelete = { goal ->
                onDeleteSavingsGoal(goal)
                showGoalDialog = false
            }
        )
    }

    if (confirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = { confirmRestoreDialog = false },
            title = { Text("بازگردانی دستی از فایل انتخابی") },
            text = {
                Text("با انتخاب فایل پشتیبان (JSON) از حافظه گوشی یا گوگل درایو، اطلاعات فعلی با فایل انتخابی جایگزین می‌شود. آیا ادامه می‌دهید؟")
            },
            confirmButton = {
                Button(onClick = {
                    confirmRestoreDialog = false
                    onRequestDriveRestore()
                }) {
                    Text("انتخاب فایل پشتیبان")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreDialog = false }) { Text("انصراف") }
            }
        )
    }

    if (showSetupPinDialog) {
        SetupPinLockDialog(
            biometricAvailable = biometricAvailability == BiometricAvailability.AVAILABLE,
            onDismiss = { showSetupPinDialog = false },
            onConfirm = { pin, question, answer, enableBio ->
                onEnablePinLock(pin, question, answer, enableBio)
                showSetupPinDialog = false
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onVerifyCurrentPin = onVerifyCurrentPin,
            onDismiss = { showChangePinDialog = false },
            onSaveNewPin = { newPin ->
                onChangePin(newPin)
                showChangePinDialog = false
            }
        )
    }

    if (showUpdateQuestionDialog) {
        UpdateSecurityQuestionDialog(
            currentQuestion = state.preferences.securityQuestion,
            onVerifyCurrentPin = onVerifyCurrentPin,
            onDismiss = { showUpdateQuestionDialog = false },
            onSave = { q, a ->
                onUpdateSecurityQuestion(q, a)
                showUpdateQuestionDialog = false
            }
        )
    }

    if (showDisablePinDialog) {
        DisablePinConfirmDialog(
            onVerifyCurrentPin = onVerifyCurrentPin,
            onDismiss = { showDisablePinDialog = false },
            onConfirmedDisable = {
                onDisablePinLock()
                showDisablePinDialog = false
            }
        )
    }
}

@Composable
private fun PreferenceToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
