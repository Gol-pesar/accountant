package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.AutoRestoreResult
import com.example.data.backup.DriveBackupManager
import com.example.data.local.AccountingDatabase
import com.example.data.local.BankAccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomBankEntity
import com.example.data.local.PendingSmsEntity
import com.example.data.local.PersonEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferences
import com.example.data.repository.AccountingRepository
import com.example.data.repository.SmsSyncResult
import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.IranianSmsParser
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliYearMonth
import com.example.domain.ParsedBankSms
import com.example.security.SecurityCrypto
import com.example.sms.SmsInboxReader
import com.example.ui.components.BudgetStatus
import com.example.ui.components.CategorySlice
import com.example.ui.components.DailyPoint
import com.example.ui.components.MonthlyBarPoint
import com.example.update.AppReleaseInfo
import com.example.update.GitHubUpdateChecker
import com.example.update.UpdateCheckResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonLedgerSummary(
    val person: PersonEntity,
    val netBalanceToman: Long, // >0: they owe me (طلب من), <0: I owe them (بدهی من)
    val transactionCount: Int
)

enum class TxFilterType { ALL, EXPENSE, INCOME }

data class AccountingUiState(
    val selectedMonth: JalaliYearMonth = JalaliCalendar.currentYearMonth(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val pendingSms: List<PendingSmsEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val customBankEntities: List<CustomBankEntity> = emptyList(),
    val customBanks: List<IranianBank> = emptyList(),
    val allBanks: List<IranianBank> = IranianBanks.ALL,
    val preferences: UserPreferences = UserPreferences(),
    val hasSmsPermission: Boolean = false,
    val isScanningSms: Boolean = false,
    val availableUpdate: AppReleaseInfo? = null,
    val isCheckingUpdate: Boolean = false,
    // Derived overview metrics
    val totalNetBalanceToman: Long = 0L,
    val monthIncomeToman: Long = 0L,
    val monthExpenseToman: Long = 0L,
    val monthTransactions: List<TransactionEntity> = emptyList(),
    // Derived analytics
    val expenseSlices: List<CategorySlice> = emptyList(),
    val incomeSlices: List<CategorySlice> = emptyList(),
    val sixMonthSeries: List<MonthlyBarPoint> = emptyList(),
    val dailySpendingSeries: List<DailyPoint> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    // Derived people balances
    val peopleSummaries: List<PersonLedgerSummary> = emptyList(),
    val totalReceivableToman: Long = 0L,
    val totalPayableToman: Long = 0L
)

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AccountingDatabase.getInstance(application)
    private val prefsRepo = PreferencesRepository(application)
    private val smsReader = SmsInboxReader(application)
    private val backupManager = DriveBackupManager(application, db.accountingDao())
    private val repository = AccountingRepository(
        dao = db.accountingDao(),
        preferencesRepository = prefsRepo,
        smsInboxReader = smsReader,
        driveBackupManager = backupManager
    )

    private val _selectedMonth = MutableStateFlow(JalaliCalendar.currentYearMonth())
    private val _hasSmsPermission = MutableStateFlow(repository.hasReadSmsPermission())
    private val _isScanningSms = MutableStateFlow(false)
    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _requestAutoRestoreFolderPicker = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val requestAutoRestoreFolderPicker: SharedFlow<Unit> = _requestAutoRestoreFolderPicker.asSharedFlow()

    // Manual SMS text analyzer state
    private val _manualSmsText = MutableStateFlow("")
    val manualSmsText: StateFlow<String> = _manualSmsText.asStateFlow()

    private val _manualParsedResult = MutableStateFlow<ParsedBankSms?>(null)
    val manualParsedResult: StateFlow<ParsedBankSms?> = _manualParsedResult.asStateFlow()

    // App Lock & Security state
    private val _isSecurityReady = MutableStateFlow(false)
    val isSecurityReady: StateFlow<Boolean> = _isSecurityReady.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // GitHub Releases update state
    private val _availableUpdate = MutableStateFlow<AppReleaseInfo?>(null)
    private val _isCheckingUpdate = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureDefaultCategories()
                val initialPrefs = repository.preferences.first()
                _isAppLocked.value = initialPrefs.pinLockEnabled && initialPrefs.pinHash.isNotBlank()
                _isSecurityReady.value = true

                // Automatically scan SMS inbox on launch when permission is granted and auto-read is ON
                if (initialPrefs.autoApproveSms && repository.hasReadSmsPermission()) {
                    repository.scanDeviceSmsInbox()
                }
            }.onFailure {
                _isSecurityReady.value = true
            }
        }

        // Silent GitHub release check on startup
        checkForAppUpdate(userInitiated = false)
    }

    private val coreDataFlow = combine(
        repository.transactions,
        repository.categories,
        repository.people,
        repository.bankAccounts,
        repository.pendingSms
    ) { txs, cats, people, accounts, pending ->
        CoreBundle(txs, cats, people, accounts, pending)
    }

    private val auxFlow = combine(
        repository.savingsGoals,
        repository.preferences,
        _selectedMonth,
        _hasSmsPermission,
        _isScanningSms
    ) { goals, prefs, month, hasPerm, scanning ->
        AuxBundle(goals, prefs, month, hasPerm, scanning)
    }

    private val extraFlow = combine(
        repository.customBanks,
        _availableUpdate,
        _isCheckingUpdate
    ) { customEntities, updateInfo, checkingUpdate ->
        val customModels = customEntities.map { it.toIranianBank() }
        IranianBanks.updateCustomBanks(customModels)
        ExtraBundle(customEntities, customModels, updateInfo, checkingUpdate)
    }

    val uiState: StateFlow<AccountingUiState> = combine(coreDataFlow, auxFlow, extraFlow) { core, aux, extra ->
        buildUiState(core, aux, extra)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountingUiState(hasSmsPermission = repository.hasReadSmsPermission())
    )

    private fun buildUiState(core: CoreBundle, aux: AuxBundle, extra: ExtraBundle): AccountingUiState {
        val month = aux.selectedMonth
        val catMap = core.categories.associateBy { it.id }

        // Group strictly by exact Jalali (year, month) so months are NEVER mixed across the year
        val txsByJalaliMonth = core.transactions.groupBy { tx ->
            val d = JalaliCalendar.fromEpochMillis(tx.timestampMillis)
            JalaliYearMonth(d.year, d.month)
        }
        val monthTxs = txsByJalaliMonth[month].orEmpty()
        val monthIncome = monthTxs.filter { it.isIncome }.sumOf { it.amountToman }
        val monthExpense = monthTxs.filter { !it.isIncome }.sumOf { it.amountToman }

        val totalNetBalance = if (core.bankAccounts.isNotEmpty()) {
            core.bankAccounts.sumOf { it.balanceToman }
        } else {
            core.transactions.sumOf { if (it.isIncome) it.amountToman else -it.amountToman }
        }

        // Category slices for selected Jalali month only
        val expenseSlices = buildSlices(monthTxs.filter { !it.isIncome }, catMap)
        val incomeSlices = buildSlices(monthTxs.filter { it.isIncome }, catMap)

        // Last 6 Jalali months bar chart (strict per-month totals)
        val sixMonths = JalaliCalendar.lastNMonths(month, 6).map { ym ->
            val list = txsByJalaliMonth[ym].orEmpty()
            MonthlyBarPoint(
                monthLabel = ym.monthName,
                incomeToman = list.filter { it.isIncome }.sumOf { it.amountToman },
                expenseToman = list.filter { !it.isIncome }.sumOf { it.amountToman }
            )
        }

        // Daily spending trend for selected Jalali month
        val daysCount = month.daysInMonth()
        val dailyTotals = LongArray(daysCount)
        monthTxs.filter { !it.isIncome }.forEach { tx ->
            val d = JalaliCalendar.fromEpochMillis(tx.timestampMillis).day
            if (d in 1..daysCount) {
                dailyTotals[d - 1] += tx.amountToman
            }
        }
        val dailySeries = (1..daysCount).map { day ->
            DailyPoint(dayOfMonth = day, expenseToman = dailyTotals[day - 1])
        }

        // Monthly category budgets
        val spentByCategory = monthTxs.filter { !it.isIncome }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountToman } }

        val budgets = core.categories
            .filter { !it.isIncome && it.monthlyBudgetToman > 0L }
            .map { cat ->
                BudgetStatus(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    iconKey = cat.iconKey,
                    colorHex = cat.colorHex,
                    spentToman = spentByCategory[cat.id] ?: 0L,
                    budgetToman = cat.monthlyBudgetToman
                )
            }
            .sortedByDescending { it.progress }

        // People balances
        val peopleSummaries = core.people.map { person ->
            val linked = core.transactions.filter { it.personId == person.id && it.personImpact != 0 }
            val net = linked.sumOf { it.amountToman * it.personImpact }
            PersonLedgerSummary(
                person = person,
                netBalanceToman = net,
                transactionCount = core.transactions.count { it.personId == person.id }
            )
        }.sortedByDescending { kotlin.math.abs(it.netBalanceToman) }

        val totalReceivable = peopleSummaries.filter { it.netBalanceToman > 0L }.sumOf { it.netBalanceToman }
        val totalPayable = peopleSummaries.filter { it.netBalanceToman < 0L }.sumOf { -it.netBalanceToman }

        return AccountingUiState(
            selectedMonth = month,
            transactions = core.transactions,
            categories = core.categories,
            people = core.people,
            bankAccounts = core.bankAccounts,
            pendingSms = core.pendingSms,
            savingsGoals = aux.goals,
            customBankEntities = extra.customBankEntities,
            customBanks = extra.customBanks,
            allBanks = IranianBanks.allBanks(extra.customBanks),
            preferences = aux.prefs,
            hasSmsPermission = aux.hasPerm,
            isScanningSms = aux.isScanning,
            availableUpdate = extra.availableUpdate,
            isCheckingUpdate = extra.isCheckingUpdate,
            totalNetBalanceToman = totalNetBalance,
            monthIncomeToman = monthIncome,
            monthExpenseToman = monthExpense,
            monthTransactions = monthTxs,
            expenseSlices = expenseSlices,
            incomeSlices = incomeSlices,
            sixMonthSeries = sixMonths,
            dailySpendingSeries = dailySeries,
            budgetStatuses = budgets,
            peopleSummaries = peopleSummaries,
            totalReceivableToman = totalReceivable,
            totalPayableToman = totalPayable
        )
    }

    private fun buildSlices(
        txs: List<TransactionEntity>,
        catMap: Map<Long, CategoryEntity>
    ): List<CategorySlice> {
        val total = txs.sumOf { it.amountToman }
        if (total <= 0L) return emptyList()
        return txs.groupBy { it.categoryId ?: -1L }
            .map { (catId, list) ->
                val cat = catMap[catId]
                val sum = list.sumOf { it.amountToman }
                CategorySlice(
                    categoryId = catId,
                    name = cat?.name ?: "دسته‌بندی نشده",
                    amountToman = sum,
                    colorHex = cat?.colorHex ?: 0xFF64748B,
                    iconKey = cat?.iconKey ?: "wallet",
                    percentage = sum.toFloat() / total.toFloat()
                )
            }
            .sortedByDescending { it.amountToman }
    }

    // --- Month Navigation ---
    fun previousMonth() { _selectedMonth.value = _selectedMonth.value.previous() }
    fun nextMonth() { _selectedMonth.value = _selectedMonth.value.next() }
    fun jumpToCurrentMonth() { _selectedMonth.value = JalaliCalendar.currentYearMonth() }

    // --- Transactions ---
    fun saveTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(tx)
            _snackbarEvents.tryEmit(if (tx.id == 0L) "تراکنش با موفقیت ثبت شد" else "تراکنش ویرایش شد")
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _snackbarEvents.tryEmit("تراکنش حذف شد")
        }
    }

    // --- Categories ---
    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.saveCategory(category)
            _snackbarEvents.tryEmit("دسته‌بندی ذخیره شد")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _snackbarEvents.tryEmit("دسته‌بندی حذف شد")
        }
    }

    // --- People ---
    fun savePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.savePerson(person)
            _snackbarEvents.tryEmit("اطلاعات شخص ذخیره شد")
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
            _snackbarEvents.tryEmit("شخص حذف شد")
        }
    }

    fun settlePersonBalance(summary: PersonLedgerSummary, bankCode: String) {
        if (summary.netBalanceToman == 0L) return
        viewModelScope.launch {
            val isReceivingBack = summary.netBalanceToman > 0L
            val amount = kotlin.math.abs(summary.netBalanceToman)
            val tx = TransactionEntity(
                title = "تسویه حساب با ${summary.person.name}",
                amountToman = amount,
                isIncome = isReceivingBack,
                categoryId = null,
                bankCode = bankCode,
                personId = summary.person.id,
                personImpact = if (isReceivingBack) -1 else +1,
                timestampMillis = System.currentTimeMillis(),
                note = "تسویه خودکار دفتر اشخاص"
            )
            repository.saveTransaction(tx)
            _snackbarEvents.tryEmit("حساب ${summary.person.name} تسویه شد")
        }
    }

    // --- Bank Accounts ---
    fun saveBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.saveBankAccount(account)
            _snackbarEvents.tryEmit("کارت بانکی ذخیره شد")
        }
    }

    fun deleteBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
            _snackbarEvents.tryEmit("کارت بانکی حذف شد")
        }
    }

    // --- Custom Banks ---
    fun saveCustomBank(bank: CustomBankEntity) {
        viewModelScope.launch {
            repository.saveCustomBank(bank)
            _snackbarEvents.tryEmit("بانک «${bank.persianName}» به لیست بانک‌ها اضافه شد")
        }
    }

    fun deleteCustomBank(bank: CustomBankEntity) {
        viewModelScope.launch {
            repository.deleteCustomBank(bank)
            _snackbarEvents.tryEmit("بانک «${bank.persianName}» حذف شد")
        }
    }

    // --- GitHub Releases Update Checker ---
    fun checkForAppUpdate(userInitiated: Boolean = true) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = GitHubUpdateChecker.checkForUpdate(
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersionName = BuildConfig.VERSION_NAME
            )
            _isCheckingUpdate.value = false
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    val dismissed = uiState.value.preferences.dismissedUpdateTag
                    if (userInitiated || dismissed != result.release.tagName) {
                        _availableUpdate.value = result.release
                    }
                    if (userInitiated) {
                        _snackbarEvents.tryEmit("نسخه جدید ${result.release.title} آماده دانلود است")
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _availableUpdate.value = null
                    if (userInitiated) {
                        _snackbarEvents.tryEmit("شما از آخرین نسخه برنامه استفاده می‌کنید")
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (userInitiated) {
                        _snackbarEvents.tryEmit(result.message)
                    }
                }
            }
        }
    }

    fun dismissUpdateBanner(tagName: String) {
        _availableUpdate.value = null
        viewModelScope.launch {
            repository.setDismissedUpdateTag(tagName)
        }
    }

    // --- Savings Goals ---
    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveSavingsGoal(goal)
            _snackbarEvents.tryEmit("هدف پس‌انداز ذخیره شد")
        }
    }

    fun addContributionToGoal(goal: SavingsGoalEntity, amountToman: Long) {
        if (amountToman <= 0L) return
        viewModelScope.launch {
            repository.saveSavingsGoal(goal.copy(savedAmountToman = goal.savedAmountToman + amountToman))
            _snackbarEvents.tryEmit("مبلغ به قلک «${goal.title}» اضافه شد")
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            _snackbarEvents.tryEmit("هدف پس‌انداز حذف شد")
        }
    }

    // --- SMS Scanning & Manual Parser ---
    fun refreshSmsPermission() {
        _hasSmsPermission.value = repository.hasReadSmsPermission()
    }

    fun scanDeviceSmsInbox() {
        viewModelScope.launch {
            _hasSmsPermission.value = repository.hasReadSmsPermission()
            if (!_hasSmsPermission.value) {
                _snackbarEvents.tryEmit("لطفاً ابتدا دسترسی خواندن پیامک را تأیید کنید")
                return@launch
            }
            _isScanningSms.value = true
            val result = repository.scanDeviceSmsInbox()
            _isScanningSms.value = false
            result.onSuccess { syncResult ->
                when (syncResult) {
                    is SmsSyncResult.InitialBalancesSynced -> {
                        val msg = if (syncResult.bankCount > 0) {
                            "تراز و موجودی اولیه ${JalaliCalendar.toPersianDigits(syncResult.bankCount)} بانک ثبت شد (بدون ثبت تراکنش‌های قبلی)"
                        } else {
                            "نقطه شروع ثبت شد؛ پیامک بانکی دارای موجودی یافت نشد و تراکنش‌های جدید از این پس بررسی می‌شوند"
                        }
                        _snackbarEvents.tryEmit(msg)
                    }
                    is SmsSyncResult.NewTransactionsQueued -> {
                        val msg = if (syncResult.count > 0) {
                            "${JalaliCalendar.toPersianDigits(syncResult.count)} تراکنش جدید شناسایی شد؛ جزئیات هر تراکنش را تکمیل کنید"
                        } else {
                            "پیامک بانکی جدیدی پس از آخرین بررسی پیدا نشد"
                        }
                        _snackbarEvents.tryEmit(msg)
                    }
                }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "خطا در خواندن پیامک‌ها")
            }
        }
    }

    fun syncInitialBalancesOnly() {
        viewModelScope.launch {
            _hasSmsPermission.value = repository.hasReadSmsPermission()
            if (!_hasSmsPermission.value) {
                _snackbarEvents.tryEmit("لطفاً ابتدا دسترسی خواندن پیامک را تأیید کنید")
                return@launch
            }
            _isScanningSms.value = true
            val result = repository.syncInitialBalancesFromSms()
            _isScanningSms.value = false
            result.onSuccess { bankCount ->
                val msg = if (bankCount > 0) {
                    "موجودی تراز ${JalaliCalendar.toPersianDigits(bankCount)} حساب بانکی از آخرین پیامک‌ها به‌روزرسانی شد"
                } else {
                    "پیامک بانکی دارای مانده حساب در صندوق پیامک پیدا نشد"
                }
                _snackbarEvents.tryEmit(msg)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "خطا در خواندن پیامک‌ها")
            }
        }
    }

    fun approvePendingSms(
        sms: PendingSmsEntity,
        categoryId: Long?,
        title: String,
        personId: Long?,
        personImpact: Int,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.approvePendingSms(sms, categoryId, title, personId, personImpact, note)
            _snackbarEvents.tryEmit("تراکنش با جزئیات کامل در دفتر ثبت شد")
        }
    }

    fun dismissPendingSms(smsId: Long) {
        viewModelScope.launch {
            repository.dismissPendingSms(smsId)
        }
    }

    fun updateManualSmsInput(text: String) {
        _manualSmsText.value = text
        _manualParsedResult.value = if (text.isBlank()) null else IranianSmsParser.parse(
            sender = "manual",
            body = text,
            timestampMillis = System.currentTimeMillis()
        )
    }

    fun commitManualParsedSms(
        parsed: ParsedBankSms,
        categoryId: Long?,
        title: String,
        personId: Long?,
        personImpact: Int,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.saveParsedManualSms(parsed, categoryId, title, personId, personImpact, note)
            _manualSmsText.value = ""
            _manualParsedResult.value = null
            _snackbarEvents.tryEmit("تراکنش تحلیل‌شده با جزئیات کامل ثبت شد")
        }
    }

    // --- Preferences ---
    fun setShowInRial(showInRial: Boolean) {
        viewModelScope.launch { repository.setShowInRial(showInRial) }
    }

    fun setDarkMode(darkMode: Boolean) {
        viewModelScope.launch { repository.setDarkMode(darkMode) }
    }

    fun setAutoApproveSms(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoApproveSms(enabled) }
    }

    // --- App Lock, Security Question & Biometric ---
    fun verifyCurrentPin(pin: String): Boolean {
        val storedHash = uiState.value.preferences.pinHash
        if (storedHash.isBlank()) return false
        return SecurityCrypto.verifySecret(pin, storedHash)
    }

    fun unlockWithPin(pin: String): Boolean {
        val ok = verifyCurrentPin(pin)
        if (ok) {
            _isAppLocked.value = false
        }
        return ok
    }

    fun unlockWithBiometric() {
        _isAppLocked.value = false
    }

    fun lockAppNow() {
        val prefs = uiState.value.preferences
        if (prefs.pinLockEnabled && prefs.pinHash.isNotBlank()) {
            _isAppLocked.value = true
        }
    }

    fun enablePinLock(
        pin: String,
        question: String,
        answer: String,
        enableBiometric: Boolean
    ) {
        viewModelScope.launch {
            val pinHash = SecurityCrypto.hashSecret(pin)
            val answerHash = SecurityCrypto.hashSecret(SecurityCrypto.normalizeAnswer(answer))
            repository.enablePinLock(
                pinHash = pinHash,
                securityQuestion = question,
                securityAnswerHash = answerHash,
                biometricEnabled = enableBiometric
            )
            _snackbarEvents.tryEmit("قفل امنیتی برنامه با موفقیت فعال شد")
        }
    }

    fun changePin(newPin: String) {
        viewModelScope.launch {
            val newHash = SecurityCrypto.hashSecret(newPin)
            repository.updatePinHash(newHash)
            _snackbarEvents.tryEmit("رمز عبور ۴ رقمی با موفقیت تغییر کرد")
        }
    }

    fun updateSecurityQuestion(question: String, answer: String) {
        viewModelScope.launch {
            val answerHash = SecurityCrypto.hashSecret(SecurityCrypto.normalizeAnswer(answer))
            repository.updateSecurityRecovery(question, answerHash)
            _snackbarEvents.tryEmit("سوال امنیتی به‌روزرسانی شد")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
            _snackbarEvents.tryEmit(if (enabled) "ورود با اثر انگشت فعال شد" else "ورود با اثر انگشت غیرفعال شد")
        }
    }

    fun disablePinLock() {
        viewModelScope.launch {
            repository.disablePinLock()
            _isAppLocked.value = false
            _snackbarEvents.tryEmit("قفل برنامه غیرفعال شد")
        }
    }

    fun recoverWithSecurityAnswer(answer: String, newPin: String): Boolean {
        val storedAnswerHash = uiState.value.preferences.securityAnswerHash
        if (storedAnswerHash.isBlank()) return false
        val normalized = SecurityCrypto.normalizeAnswer(answer)
        if (!SecurityCrypto.verifySecret(normalized, storedAnswerHash)) {
            return false
        }
        viewModelScope.launch {
            val newPinHash = SecurityCrypto.hashSecret(newPin)
            repository.updatePinHash(newPinHash)
            _isAppLocked.value = false
            _snackbarEvents.tryEmit("رمز جدید ثبت شد و قفل برنامه باز شد")
        }
        return true
    }

    // --- Google Drive / SAF User-Consented Backup & Restore ---
    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val res = repository.exportBackupToDrive(uri)
            res.onSuccess { stats ->
                _snackbarEvents.tryEmit(
                    "پشتیبان‌گیری انجام شد (${JalaliCalendar.toPersianDigits(stats.transactionCount)} تراکنش)"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "خطا در ذخیره فایل پشتیبان")
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val res = repository.restoreBackupFromDrive(uri)
            res.onSuccess { stats ->
                _snackbarEvents.tryEmit(
                    "بازگردانی دستی با موفقیت انجام شد (${JalaliCalendar.toPersianDigits(stats.transactionCount)} تراکنش و ${JalaliCalendar.toPersianDigits(stats.personCount)} شخص)"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "خطا در بازیابی فایل پشتیبان")
            }
        }
    }

    // --- Real-Time Persistent Auto-Backup ("من را حذف نکنید") & One-Click Auto Restore ---
    fun triggerManualAutoFolderBackup() {
        viewModelScope.launch {
            val res = repository.runRealtimeAutoBackupNow()
            res.onSuccess { stats ->
                _snackbarEvents.tryEmit(
                    "پشتیبان لحظه‌ای در پوشه «${DriveBackupManager.AUTO_BACKUP_FOLDER_NAME}» ذخیره شد (${JalaliCalendar.toPersianDigits(stats.transactionCount)} تراکنش)"
                )
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "خطا در ذخیره فایل پشتیبان خودکار")
            }
        }
    }

    fun restoreFromAutoBackupFolder() {
        viewModelScope.launch {
            when (val result = repository.restoreFromAutoBackupFolder()) {
                is AutoRestoreResult.Restored -> {
                    _snackbarEvents.tryEmit(
                        "بازگردانی خودکار از ${result.sourceLabel} انجام شد (${JalaliCalendar.toPersianDigits(result.stats.transactionCount)} تراکنش و ${JalaliCalendar.toPersianDigits(result.stats.bankAccountCount)} کارت)"
                    )
                }
                is AutoRestoreResult.NeedsFolderAccess -> {
                    _snackbarEvents.tryEmit(result.message)
                    _requestAutoRestoreFolderPicker.tryEmit(Unit)
                }
                is AutoRestoreResult.NotFound -> {
                    _snackbarEvents.tryEmit(result.message)
                }
                is AutoRestoreResult.Error -> {
                    _snackbarEvents.tryEmit(result.message)
                }
            }
        }
    }

    fun onAutoBackupFolderSelectedForRestore(treeUri: Uri) {
        viewModelScope.launch {
            when (val result = repository.restoreFromSelectedAutoBackupFolder(treeUri)) {
                is AutoRestoreResult.Restored -> {
                    _snackbarEvents.tryEmit(
                        "بازگردانی خودکار انجام شد (${JalaliCalendar.toPersianDigits(result.stats.transactionCount)} تراکنش و ${JalaliCalendar.toPersianDigits(result.stats.bankAccountCount)} کارت)"
                    )
                }
                is AutoRestoreResult.NotFound -> {
                    _snackbarEvents.tryEmit(result.message)
                }
                is AutoRestoreResult.Error -> {
                    _snackbarEvents.tryEmit(result.message)
                }
                is AutoRestoreResult.NeedsFolderAccess -> {
                    _snackbarEvents.tryEmit(result.message)
                }
            }
        }
    }

    fun buildMonthlyReportText(): String {
        val state = uiState.value
        val showRial = state.preferences.showInRial
        val sb = StringBuilder()
        sb.appendLine("گزارش مالی «حسابدار من» — ${state.selectedMonth.format()}")
        sb.appendLine("─────────────────────")
        sb.appendLine("درآمد ماه: ${JalaliCalendar.formatCurrency(state.monthIncomeToman, showRial)}")
        sb.appendLine("هزینه ماه: ${JalaliCalendar.formatCurrency(state.monthExpenseToman, showRial)}")
        val net = state.monthIncomeToman - state.monthExpenseToman
        sb.appendLine("تراز خالص: ${JalaliCalendar.formatCurrency(net, showRial)}")
        if (state.expenseSlices.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("تفکیک هزینه‌ها:")
            state.expenseSlices.forEach { s ->
                sb.appendLine("• ${s.name}: ${JalaliCalendar.formatCurrency(s.amountToman, showRial)} (${JalaliCalendar.toPersianDigits((s.percentage * 100).toInt())}٪)")
            }
        }
        if (state.bankAccounts.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("موجودی کارت‌ها:")
            state.bankAccounts.forEach { acc ->
                val b = IranianBanks.findByCode(acc.bankCode)
                sb.appendLine("• ${b.shortName}: ${JalaliCalendar.formatCurrency(acc.balanceToman, showRial)}")
            }
        }
        return sb.toString()
    }

    private data class CoreBundle(
        val transactions: List<TransactionEntity>,
        val categories: List<CategoryEntity>,
        val people: List<PersonEntity>,
        val bankAccounts: List<BankAccountEntity>,
        val pendingSms: List<PendingSmsEntity>
    )

    private data class AuxBundle(
        val goals: List<SavingsGoalEntity>,
        val prefs: UserPreferences,
        val selectedMonth: JalaliYearMonth,
        val hasPerm: Boolean,
        val isScanning: Boolean
    )

    private data class ExtraBundle(
        val customBankEntities: List<CustomBankEntity>,
        val customBanks: List<IranianBank>,
        val availableUpdate: AppReleaseInfo?,
        val isCheckingUpdate: Boolean
    )
}
