package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendar
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PeopleScreen
import com.example.ui.screens.SettingsMoreScreen
import com.example.ui.screens.SmsAnalyzerScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.HesabdarTheme
import com.example.ui.viewmodel.AccountingViewModel

enum class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "خانه", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    TRANSACTIONS("transactions", "تراکنش", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong),
    ANALYTICS("analytics", "نمودار", Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    SMS("sms", "پیامک", Icons.Rounded.Sms, Icons.Outlined.Sms),
    PEOPLE("people", "اشخاص", Icons.Rounded.People, Icons.Outlined.People),
    SETTINGS("settings", "تنظیمات", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AccountingViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isSecurityReady by viewModel.isSecurityReady.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
            val biometricAvailability = remember { BiometricHelper.checkAvailability(this) }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HesabdarTheme(darkTheme = state.preferences.darkMode) {
                    if (!isSecurityReady) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else if (isAppLocked) {
                        AppLockScreen(
                            securityQuestion = state.preferences.securityQuestion,
                            biometricEnabled = state.preferences.biometricEnabled,
                            biometricAvailable = biometricAvailability == BiometricAvailability.AVAILABLE,
                            onVerifyPin = viewModel::unlockWithPin,
                            onTriggerBiometric = {
                                BiometricHelper.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = { viewModel.unlockWithBiometric() },
                                    onFallbackOrError = { /* User stays on PIN keypad */ }
                                )
                            },
                            onRecoverWithSecurityAnswer = viewModel::recoverWithSecurityAnswer
                        )
                    } else {
                        HesabdarAppShell(
                            viewModel = viewModel,
                            biometricAvailability = biometricAvailability
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HesabdarAppShell(
    viewModel: AccountingViewModel,
    biometricAvailability: BiometricAvailability
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val manualSmsText by viewModel.manualSmsText.collectAsStateWithLifecycle()
    val manualParsedResult by viewModel.manualParsedResult.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showTxSheet by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var defaultNewTxIncome by remember { mutableStateOf(false) }
    var smsPermissionDenied by remember { mutableStateOf(false) }
    var hideUpdateDialogThisSession by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Re-check SMS permission cleanly whenever user returns from Android App Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hadBefore = state.hasSmsPermission
                viewModel.refreshSmsPermission()
                if (!hadBefore && viewModel.uiState.value.hasSmsPermission) {
                    smsPermissionDenied = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle Android Back button cleanly across non-home destinations
    BackHandler(enabled = currentDestination != AppDestination.DASHBOARD) {
        currentDestination = AppDestination.DASHBOARD
    }

    // Runtime SMS permission launcher (READ_SMS + RECEIVE_SMS) with safe error handling
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        runCatching {
            viewModel.refreshSmsPermission()
            val readGranted = grants[Manifest.permission.READ_SMS] == true
            if (readGranted) {
                smsPermissionDenied = false
                viewModel.scanDeviceSmsInbox()
            } else {
                smsPermissionDenied = true
            }
        }
    }

    fun openAppDetailsSettings() {
        runCatching {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // User-consented Google Drive / SAF Export Backup launcher
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri)
        }
    }

    // User-consented Google Drive / SAF Restore Backup launcher
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(uri)
        }
    }

    fun launchDriveBackup() {
        val today = JalaliCalendar.today()
        val fileName = "hesabdar_backup_${today.year}_${today.month}_${today.day}.json"
        runCatching { createBackupLauncher.launch(fileName) }
    }

    fun launchDriveRestore() {
        runCatching { openBackupLauncher.launch(arrayOf("application/json", "*/*")) }
    }

    fun shareMonthlyReport() {
        runCatching {
            val text = viewModel.buildMonthlyReportText()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "گزارش مالی حسابدار من")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری گزارش ماهانه"))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentDestination == AppDestination.DASHBOARD || currentDestination == AppDestination.TRANSACTIONS) {
                    FloatingActionButton(
                        onClick = {
                            editingTx = null
                            defaultNewTxIncome = false
                            showTxSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "ثبت تراکنش جدید")
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    DestinationIcon(
                                        dest = dest,
                                        selected = selected,
                                        pendingSmsCount = state.pendingSms.size
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    DestinationIcon(
                                        dest = dest,
                                        selected = selected,
                                        pendingSmsCount = state.pendingSms.size
                                    )
                                },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (currentDestination) {
                            AppDestination.DASHBOARD -> DashboardScreen(
                                state = state,
                                onAddExpense = {
                                    editingTx = null
                                    defaultNewTxIncome = false
                                    showTxSheet = true
                                },
                                onAddIncome = {
                                    editingTx = null
                                    defaultNewTxIncome = true
                                    showTxSheet = true
                                },
                                onOpenSms = { currentDestination = AppDestination.SMS },
                                onOpenPeople = { currentDestination = AppDestination.PEOPLE },
                                onOpenTransactions = { currentDestination = AppDestination.TRANSACTIONS },
                                onOpenSettings = { currentDestination = AppDestination.SETTINGS },
                                onRequestDriveBackup = ::launchDriveBackup,
                                onEditTransaction = { tx ->
                                    editingTx = tx
                                    showTxSheet = true
                                },
                                onToggleCurrency = {
                                    viewModel.setShowInRial(!state.preferences.showInRial)
                                }
                            )

                            AppDestination.TRANSACTIONS -> TransactionsScreen(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToday = viewModel::jumpToCurrentMonth,
                                onAddTransaction = {
                                    editingTx = null
                                    defaultNewTxIncome = false
                                    showTxSheet = true
                                },
                                onEditTransaction = { tx ->
                                    editingTx = tx
                                    showTxSheet = true
                                }
                            )

                            AppDestination.ANALYTICS -> AnalyticsScreen(
                                state = state,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onResetToday = viewModel::jumpToCurrentMonth,
                                onShareMonthlyReport = ::shareMonthlyReport,
                                onManageCategories = { currentDestination = AppDestination.SETTINGS }
                            )

                            AppDestination.SMS -> SmsAnalyzerScreen(
                                state = state,
                                manualSmsText = manualSmsText,
                                manualParsedResult = manualParsedResult,
                                smsPermissionDenied = smsPermissionDenied,
                                onRequestSmsPermission = {
                                    runCatching {
                                        smsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_SMS,
                                                Manifest.permission.RECEIVE_SMS
                                            )
                                        )
                                    }.onFailure {
                                        smsPermissionDenied = true
                                        openAppDetailsSettings()
                                    }
                                },
                                onOpenAppSettings = ::openAppDetailsSettings,
                                onScanInbox = viewModel::scanDeviceSmsInbox,
                                onApprovePendingSms = viewModel::approvePendingSms,
                                onDismissPendingSms = viewModel::dismissPendingSms,
                                onManualSmsTextChange = viewModel::updateManualSmsInput,
                                onCommitManualSms = viewModel::commitManualParsedSms
                            )

                            AppDestination.PEOPLE -> PeopleScreen(
                                state = state,
                                onSavePerson = viewModel::savePerson,
                                onDeletePerson = viewModel::deletePerson,
                                onSettlePerson = viewModel::settlePersonBalance,
                                onEditTransaction = { tx ->
                                    editingTx = tx
                                    showTxSheet = true
                                }
                            )

                            AppDestination.SETTINGS -> SettingsMoreScreen(
                                state = state,
                                onSaveCategory = viewModel::saveCategory,
                                onDeleteCategory = viewModel::deleteCategory,
                                onSaveBankAccount = viewModel::saveBankAccount,
                                onDeleteBankAccount = viewModel::deleteBankAccount,
                                onSaveCustomBank = viewModel::saveCustomBank,
                                onDeleteCustomBank = viewModel::deleteCustomBank,
                                onSaveSavingsGoal = viewModel::saveSavingsGoal,
                                onAddGoalContribution = viewModel::addContributionToGoal,
                                onDeleteSavingsGoal = viewModel::deleteSavingsGoal,
                                onSetShowInRial = viewModel::setShowInRial,
                                onSetDarkMode = viewModel::setDarkMode,
                                onSetAutoApproveSms = viewModel::setAutoApproveSms,
                                onRequestDriveBackup = ::launchDriveBackup,
                                onRequestDriveRestore = ::launchDriveRestore,
                                onCheckForUpdate = {
                                    hideUpdateDialogThisSession = false
                                    viewModel.checkForAppUpdate(userInitiated = true)
                                },
                                onOpenUrl = ::openExternalUrl,
                                biometricAvailability = biometricAvailability,
                                onVerifyCurrentPin = viewModel::verifyCurrentPin,
                                onEnablePinLock = viewModel::enablePinLock,
                                onChangePin = viewModel::changePin,
                                onUpdateSecurityQuestion = viewModel::updateSecurityQuestion,
                                onSetBiometricEnabled = viewModel::setBiometricEnabled,
                                onDisablePinLock = viewModel::disablePinLock,
                                onLockAppNow = viewModel::lockAppNow
                            )
                        }
                    }
                }
            }
        }
    }

    // GitHub Releases Update Notification Dialog
    val updateRelease = state.availableUpdate
    if (updateRelease != null && !hideUpdateDialogThisSession) {
        AlertDialog(
            onDismissRequest = { hideUpdateDialogThisSession = true },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "نسخه جدید حسابدار من منتشر شد",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "نسخه جدید: ${updateRelease.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "می‌توانید فایل APK نسخه جدید را مستقیماً از گیت‌هاب دانلود کرده و بدون حذف نسخه قبلی نصب کنید (اطلاعات شما حفظ می‌شود).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hideUpdateDialogThisSession = true
                        openExternalUrl(updateRelease.apkDownloadUrl ?: updateRelease.releasePageUrl)
                    },
                    modifier = Modifier.testTag("dialog_download_update_button")
                ) {
                    Text("دانلود نسخه جدید")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        hideUpdateDialogThisSession = true
                        viewModel.dismissUpdateBanner(updateRelease.tagName)
                    }) {
                        Text("نادیده گرفتن")
                    }
                    TextButton(onClick = { hideUpdateDialogThisSession = true }) {
                        Text("بعداً")
                    }
                }
            }
        )
    }

    if (showTxSheet) {
        TransactionEditorSheet(
            initialTx = editingTx,
            defaultIsIncome = defaultNewTxIncome,
            categories = state.categories,
            people = state.people,
            allBanks = state.allBanks,
            showInRial = state.preferences.showInRial,
            onDismiss = { showTxSheet = false },
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                showTxSheet = false
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx)
                showTxSheet = false
            }
        )
    }
}

@Composable
private fun DestinationIcon(
    dest: AppDestination,
    selected: Boolean,
    pendingSmsCount: Int
) {
    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest == AppDestination.SMS && pendingSmsCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(JalaliCalendar.toPersianDigits(pendingSmsCount))
                }
            }
        ) {
            Icon(icon, contentDescription = dest.label)
        }
    } else {
        Icon(icon, contentDescription = dest.label)
    }
}
