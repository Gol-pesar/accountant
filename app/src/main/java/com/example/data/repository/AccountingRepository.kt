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

    // --- SMS Scanning & Approval ---
    fun hasReadSmsPermission(): Boolean = smsInboxReader.hasReadSmsPermission()

    suspend fun scanDeviceSmsInbox(): Result<Pair<Int, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val customBankModels = dao.getCustomBanksOnce().map { it.toIranianBank() }
            val scanResult = smsInboxReader.scanRecentBankSms(customBanks = customBankModels)
            if (scanResult.isFailure) {
                return@withContext Result.failure(
                    scanResult.exceptionOrNull() ?: IllegalStateException("خطا در خواندن پیامک‌ها")
                )
            }
            val parsedList = scanResult.getOrDefault(emptyList())
            val prefs = preferencesRepository.preferencesFlow.first()
            val categories = dao.getCategoriesOnce()
            var processedCount = 0

            for (parsed in parsedList) {
                runCatching {
                    if (dao.hasTransactionWithSmsHash(parsed.smsHash)) return@runCatching
                    if (prefs.autoApproveSms) {
                        val isIncome = parsed.type == ParsedTxType.INCOME
                        val matchedCat = categories.firstOrNull {
                            it.isIncome == isIncome && it.name == parsed.suggestedCategoryName
                        } ?: categories.firstOrNull { it.isIncome == isIncome }

                        dao.insertTransaction(
                            TransactionEntity(
                                title = matchedCat?.name ?: "${parsed.bank.shortName} - ${if (isIncome) "واریز پیامکی" else "برداشت پیامکی"}",
                                amountToman = parsed.amountToman,
                                isIncome = isIncome,
                                categoryId = matchedCat?.id,
                                bankCode = parsed.bank.code,
                                timestampMillis = parsed.timestampMillis,
                                note = parsed.cardOrAccountHint?.let { "ثبت خودکار از پیامک ($it)" } ?: "ثبت خودکار از پیامک بانکی",
                                fromSms = true,
                                smsHash = parsed.smsHash
                            )
                        )
                        if (parsed.remainingBalanceToman != null) {
                            val acc = dao.findBankAccountByCode(parsed.bank.code)
                            if (acc != null) {
                                dao.updateBankAccount(acc.copy(balanceToman = parsed.remainingBalanceToman))
                            }
                        }
                        processedCount++
                    } else {
                        val rowId = dao.insertPendingSms(parsed.toPendingEntity())
                        if (rowId != -1L) processedCount++
                    }
                }
            }
            Result.success(processedCount to prefs.autoApproveSms)
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
        personImpact: Int
    ) {
        dao.insertTransaction(
            TransactionEntity(
                title = title,
                amountToman = sms.amountToman,
                isIncome = sms.isIncome,
                categoryId = categoryId,
                bankCode = sms.bankCode,
                personId = personId,
                personImpact = personImpact,
                timestampMillis = sms.timestampMillis,
                note = sms.cardHint?.let { "پیامک بانکی ($it)" } ?: "ثبت از پیامک بانکی",
                fromSms = true,
                smsHash = sms.smsHash
            )
        )
        dao.updatePendingSmsStatus(sms.id, "APPROVED")
        if (sms.remainingBalanceToman != null) {
            val acc = dao.findBankAccountByCode(sms.bankCode)
            if (acc != null) {
                dao.updateBankAccount(acc.copy(balanceToman = sms.remainingBalanceToman))
            }
        }
    }

    suspend fun dismissPendingSms(smsId: Long) {
        dao.updatePendingSmsStatus(smsId, "DISMISSED")
    }

    suspend fun saveParsedManualSms(
        parsed: ParsedBankSms,
        categoryId: Long?,
        title: String,
        personId: Long?,
        personImpact: Int
    ) {
        dao.insertTransaction(
            TransactionEntity(
                title = title,
                amountToman = parsed.amountToman,
                isIncome = parsed.type == ParsedTxType.INCOME,
                categoryId = categoryId,
                bankCode = parsed.bank.code,
                personId = personId,
                personImpact = personImpact,
                timestampMillis = parsed.timestampMillis,
                note = parsed.cardOrAccountHint?.let { "تحلیل پیامک ($it)" } ?: "تحلیل پیامک بانکی",
                fromSms = true,
                smsHash = parsed.smsHash
            )
        )
        if (parsed.remainingBalanceToman != null) {
            val acc = dao.findBankAccountByCode(parsed.bank.code)
            if (acc != null) {
                dao.updateBankAccount(acc.copy(balanceToman = parsed.remainingBalanceToman))
            }
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
