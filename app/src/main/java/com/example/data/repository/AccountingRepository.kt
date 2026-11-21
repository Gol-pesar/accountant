package com.example.data.repository

import android.net.Uri
import com.example.data.backup.BackupStats
import com.example.data.backup.DriveBackupManager
import com.example.data.local.AccountingDao
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
import com.example.domain.ParsedBankSms
import com.example.domain.ParsedTxType
import com.example.sms.SmsInboxReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed class SmsSyncResult {
    data class InitialBalancesSynced(val bankCount: Int) : SmsSyncResult()
    data class NewTransactionsQueued(val count: Int) : SmsSyncResult()
}

class AccountingRepository(
    private val dao: AccountingDao,
    private val preferencesRepository: PreferencesRepository,
    private val smsInboxReader: SmsInboxReader,
    private val driveBackupManager: DriveBackupManager
) {
    val transactions: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val people: Flow<List<PersonEntity>> = dao.observePeople()
    val bankAccounts: Flow<List<BankAccountEntity>> = dao.observeBankAccounts()
    val pendingSms: Flow<List<PendingSmsEntity>> = dao.observePendingSms()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = dao.observeSavingsGoals()
    val customBanks: Flow<List<CustomBankEntity>> = dao.observeCustomBanks()
    val preferences: Flow<UserPreferences> = preferencesRepository.preferencesFlow

    suspend fun ensureDefaultCategories() {
        if (dao.getCategoriesOnce().isEmpty()) {
            dao.insertCategories(AccountingDatabase.DEFAULT_CATEGORIES)
        }
    }

    // --- Transactions ---
    suspend fun saveTransaction(tx: TransactionEntity) {
        if (tx.id == 0L) {
            dao.insertTransaction(tx)
            // Update matching bank account balance if user tracks that bank
            val account = dao.findBankAccountByCode(tx.bankCode)
            if (account != null) {
                val delta = if (tx.isIncome) tx.amountToman else -tx.amountToman
                dao.updateBankAccount(account.copy(balanceToman = account.balanceToman + delta))
            }
        } else {
            dao.updateTransaction(tx)
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        dao.deleteTransaction(tx)
        val account = dao.findBankAccountByCode(tx.bankCode)
        if (account != null) {
            val reverseDelta = if (tx.isIncome) -tx.amountToman else tx.amountToman
            dao.updateBankAccount(account.copy(balanceToman = account.balanceToman + reverseDelta))
        }
    }

    // --- Categories ---
    suspend fun saveCategory(category: CategoryEntity) {
        if (category.id == 0L) dao.insertCategory(category) else dao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = dao.deleteCategory(category)

    // --- People ---
    suspend fun savePerson(person: PersonEntity) {
        if (person.id == 0L) dao.insertPerson(person) else dao.updatePerson(person)
    }

    suspend fun deletePerson(person: PersonEntity) = dao.deletePerson(person)

    // --- Bank Accounts ---
    suspend fun saveBankAccount(account: BankAccountEntity) {
        if (account.id == 0L) dao.insertBankAccount(account) else dao.updateBankAccount(account)
    }

    suspend fun deleteBankAccount(account: BankAccountEntity) = dao.deleteBankAccount(account)

    // --- Savings Goals ---
    suspend fun saveSavingsGoal(goal: SavingsGoalEntity) {
        if (goal.id == 0L) dao.insertSavingsGoal(goal) else dao.updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = dao.deleteSavingsGoal(goal)

    // --- Custom Banks ---
    suspend fun saveCustomBank(bank: CustomBankEntity) {
        if (bank.id == 0L) dao.insertCustomBank(bank) else dao.updateCustomBank(bank)
    }

    suspend fun deleteCustomBank(bank: CustomBankEntity) = dao.deleteCustomBank(bank)

    // --- SMS Scanning, Initial Balance Baseline & Detail Approval ---
    fun hasReadSmsPermission(): Boolean = smsInboxReader.hasReadSmsPermission()

    /**
     * First-time / Balance-only SMS sync: scans recent bank SMS strictly to capture each bank's
     * latest remaining balance without importing historical transactions, and sets the baseline timestamp.
     */
    suspend fun syncInitialBalancesFromSms(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val customBankModels = dao.getCustomBanksOnce().map { it.toIranianBank() }
            val scanResult = smsInboxReader.scanRecentBankSms(
                maxMessages = 400,
                sinceTimestampMillis = 0L,
                customBanks = customBankModels
            )
            if (scanResult.isFailure) {
                return@withContext Result.failure(
                    scanResult.exceptionOrNull() ?: IllegalStateException("خطا در خواندن پیامک‌ها")
                )
            }
            val parsedList = scanResult.getOrDefault(emptyList())
            val latestByBank = parsedList
                .filter { it.remainingBalanceToman != null }
                .groupBy { it.bank.code }
                .mapValues { (_, list) -> list.maxBy { it.timestampMillis } }

            var syncedBanks = 0
            for ((bankCode, latestSms) in latestByBank) {
                val balance = latestSms.remainingBalanceToman ?: continue
                val existing = dao.findBankAccountByCode(bankCode)
                if (existing != null) {
                    val updatedCard = if (existing.cardNumber.isBlank() && !latestSms.cardOrAccountHint.isNullOrBlank()) {
                        latestSms.cardOrAccountHint
                    } else existing.cardNumber
                    dao.updateBankAccount(
                        existing.copy(
                            balanceToman = balance,
                            cardNumber = updatedCard
                        )
                    )
                } else {
                    dao.insertBankAccount(
                        BankAccountEntity(
                            bankCode = bankCode,
                            cardNumber = latestSms.cardOrAccountHint.orEmpty(),
                            accountTitle = "تراز پیامکی",
                            balanceToman = balance
                        )
                    )
                }
                syncedBanks++
            }

            val newestSmsTs = parsedList.maxOfOrNull { it.timestampMillis } ?: 0L
            val baselineTs = maxOf(System.currentTimeMillis(), newestSmsTs)
            preferencesRepository.setSmsBaselineTimestampMillis(baselineTs)

            Result.success(syncedBanks)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(
                IllegalStateException(t.message ?: "خطا در ثبت تراز اولیه از پیامک‌ها")
            )
        }
    }

    /**
     * On first run (no baseline yet): ONLY records current bank balances (never imports historical transactions).
     * After baseline is set: reads only newly arrived bank SMS strictly newer than baseline and queues
     * every one into `pending_sms` so the user specifies complete transaction details.
     */
    suspend fun scanDeviceSmsInbox(): Result<SmsSyncResult> = withContext(Dispatchers.IO) {
        try {
            val prefs = preferencesRepository.preferencesFlow.first()
            if (prefs.smsBaselineTimestampMillis <= 0L) {
                return@withContext syncInitialBalancesFromSms().map { bankCount ->
                    SmsSyncResult.InitialBalancesSynced(bankCount)
                }
            }

            val customBankModels = dao.getCustomBanksOnce().map { it.toIranianBank() }
            val scanResult = smsInboxReader.scanRecentBankSms(
                maxMessages = 150,
                sinceTimestampMillis = prefs.smsBaselineTimestampMillis,
                customBanks = customBankModels
            )
            if (scanResult.isFailure) {
                return@withContext Result.failure(
                    scanResult.exceptionOrNull() ?: IllegalStateException("خطا در خواندن پیامک‌ها")
                )
            }
            val parsedList = scanResult.getOrDefault(emptyList())
            var queuedCount = 0
            var maxSeenTs = prefs.smsBaselineTimestampMillis

            for (parsed in parsedList) {
                runCatching {
                    if (parsed.timestampMillis > maxSeenTs) {
                        maxSeenTs = parsed.timestampMillis
                    }
                    if (dao.hasTransactionWithSmsHash(parsed.smsHash)) return@runCatching
                    val rowId = dao.insertPendingSms(parsed.toPendingEntity())
                    if (rowId != -1L) queuedCount++
                }
            }

            if (maxSeenTs > prefs.smsBaselineTimestampMillis) {
                preferencesRepository.setSmsBaselineTimestampMillis(maxSeenTs)
            }

            Result.success(SmsSyncResult.NewTransactionsQueued(queuedCount))
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(
                IllegalStateException(t.message ?: "خطا در تحلیل پیامک‌های بانکی")
            )
        }
    }

    suspend fun approvePendingSms(
        sms: PendingSmsEntity,
        categoryId: Long?,
        title: String,
        personId: Long?,
        personImpact: Int,
        note: String = ""
    ) {
        val resolvedNote = note.trim().ifBlank {
            sms.cardHint?.let { "پیامک بانکی ($it)" } ?: "ثبت از پیامک بانکی"
        }
        dao.insertTransaction(
            TransactionEntity(
                title = title.trim(),
                amountToman = sms.amountToman,
                isIncome = sms.isIncome,
                categoryId = categoryId,
                bankCode = sms.bankCode,
                personId = personId,
                personImpact = personImpact,
                timestampMillis = sms.timestampMillis,
                note = resolvedNote,
                fromSms = true,
                smsHash = sms.smsHash
            )
        )
        dao.updatePendingSmsStatus(sms.id, "APPROVED")
        syncAccountBalanceAfterSms(
            bankCode = sms.bankCode,
            isIncome = sms.isIncome,
            amountToman = sms.amountToman,
            remainingBalanceToman = sms.remainingBalanceToman,
            cardHint = sms.cardHint
        )
    }

    suspend fun dismissPendingSms(smsId: Long) {
        dao.updatePendingSmsStatus(smsId, "DISMISSED")
    }

    suspend fun saveParsedManualSms(
        parsed: ParsedBankSms,
        categoryId: Long?,
        title: String,
        personId: Long?,
        personImpact: Int,
        note: String = ""
    ) {
        val isIncome = parsed.type == ParsedTxType.INCOME
        val resolvedNote = note.trim().ifBlank {
            parsed.cardOrAccountHint?.let { "تحلیل پیامک ($it)" } ?: "تحلیل پیامک بانکی"
        }
        dao.insertTransaction(
            TransactionEntity(
                title = title.trim(),
                amountToman = parsed.amountToman,
                isIncome = isIncome,
                categoryId = categoryId,
                bankCode = parsed.bank.code,
                personId = personId,
                personImpact = personImpact,
                timestampMillis = parsed.timestampMillis,
                note = resolvedNote,
                fromSms = true,
                smsHash = parsed.smsHash
            )
        )
        syncAccountBalanceAfterSms(
            bankCode = parsed.bank.code,
            isIncome = isIncome,
            amountToman = parsed.amountToman,
            remainingBalanceToman = parsed.remainingBalanceToman,
            cardHint = parsed.cardOrAccountHint
        )
    }

    private suspend fun syncAccountBalanceAfterSms(
        bankCode: String,
        isIncome: Boolean,
        amountToman: Long,
        remainingBalanceToman: Long?,
        cardHint: String?
    ) {
        val acc = dao.findBankAccountByCode(bankCode)
        if (remainingBalanceToman != null) {
            if (acc != null) {
                dao.updateBankAccount(acc.copy(balanceToman = remainingBalanceToman))
            } else {
                dao.insertBankAccount(
                    BankAccountEntity(
                        bankCode = bankCode,
                        cardNumber = cardHint.orEmpty(),
                        accountTitle = "حساب پیامکی",
                        balanceToman = remainingBalanceToman
                    )
                )
            }
        } else if (acc != null) {
            val delta = if (isIncome) amountToman else -amountToman
            dao.updateBankAccount(acc.copy(balanceToman = acc.balanceToman + delta))
        }
    }

    // --- Preferences ---
    suspend fun setShowInRial(showInRial: Boolean) = preferencesRepository.setShowInRial(showInRial)
    suspend fun setDarkMode(darkMode: Boolean) = preferencesRepository.setDarkMode(darkMode)
    suspend fun setAutoApproveSms(enabled: Boolean) = preferencesRepository.setAutoApproveSms(enabled)
    suspend fun setDismissedUpdateTag(tag: String) = preferencesRepository.setDismissedUpdateTag(tag)

    // --- App Lock & Security ---
    suspend fun enablePinLock(
        pinHash: String,
        securityQuestion: String,
        securityAnswerHash: String,
        biometricEnabled: Boolean
    ) = preferencesRepository.enablePinLock(pinHash, securityQuestion, securityAnswerHash, biometricEnabled)

    suspend fun updatePinHash(pinHash: String) = preferencesRepository.updatePinHash(pinHash)

    suspend fun updateSecurityRecovery(question: String, answerHash: String) =
        preferencesRepository.updateSecurityRecovery(question, answerHash)

    suspend fun setBiometricEnabled(enabled: Boolean) = preferencesRepository.setBiometricEnabled(enabled)

    suspend fun disablePinLock() = preferencesRepository.disablePinLock()

    // --- User-Consented Google Drive / SAF Backup & Restore ---
    suspend fun exportBackupToDrive(uri: Uri): Result<BackupStats> {
        val result = driveBackupManager.exportBackupToUri(uri)
        if (result.isSuccess) {
            preferencesRepository.setLastBackupMillis(System.currentTimeMillis())
        }
        return result
    }

    suspend fun restoreBackupFromDrive(uri: Uri): Result<BackupStats> =
        driveBackupManager.restoreBackupFromUri(uri)

    private fun ParsedBankSms.toPendingEntity(): PendingSmsEntity = PendingSmsEntity(
        smsHash = smsHash,
        sender = sender,
        rawBody = rawBody,
        bankCode = bank.code,
        isIncome = type == ParsedTxType.INCOME,
        amountToman = amountToman,
        remainingBalanceToman = remainingBalanceToman,
        cardHint = cardOrAccountHint,
        suggestedCategoryName = suggestedCategoryName,
        timestampMillis = timestampMillis,
        status = "PENDING"
    )
}
