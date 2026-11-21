package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.data.backup.DriveBackupManager
import com.example.data.local.AccountingDatabase
import com.example.data.local.BankAccountEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.TransactionEntity
import com.example.domain.JalaliCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles inline quick-reply descriptions entered directly from the Android notification shade
 * for newly received bank SMS transactions.
 */
class NotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BankSmsNotificationHelper.ACTION_QUICK_REPLY_TX) return

        val pendingSmsId = intent.getLongExtra(BankSmsNotificationHelper.EXTRA_PENDING_SMS_ID, -1L)
        if (pendingSmsId <= 0L) return

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(BankSmsNotificationHelper.KEY_QUICK_DESCRIPTION)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (replyText.isBlank()) {
            BankSmsNotificationHelper.cancelNotification(context, pendingSmsId)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val db = AccountingDatabase.getInstance(appCtx)
                val dao = db.accountingDao()
                val prefsRepo = PreferencesRepository(appCtx)
                val prefs = prefsRepo.preferencesFlow.first()

                val sms = dao.getPendingSmsById(pendingSmsId)
                if (sms == null || sms.status != "PENDING") {
                    BankSmsNotificationHelper.cancelNotification(appCtx, pendingSmsId)
                    return@launch
                }

                val categories = dao.getCategoriesOnce().filter { it.isIncome == sms.isIncome }
                val matchedCatId = categories.firstOrNull { it.name == sms.suggestedCategoryName }?.id
                    ?: categories.firstOrNull()?.id

                val note = sms.cardHint?.let { "ثبت فوری از اعلان پیامک ($it)" } ?: "ثبت فوری از اعلان پیامک"
                dao.insertTransaction(
                    TransactionEntity(
                        title = replyText,
                        amountToman = sms.amountToman,
                        isIncome = sms.isIncome,
                        categoryId = matchedCatId,
                        bankCode = sms.bankCode,
                        personId = null,
                        personImpact = 0,
                        timestampMillis = sms.timestampMillis,
                        note = note,
                        fromSms = true,
                        smsHash = sms.smsHash
                    )
                )
                dao.updatePendingSmsStatus(sms.id, "APPROVED")

                // Update matching bank account balance
                val existingAcc = dao.findBankAccountByCode(sms.bankCode)
                val remaining = sms.remainingBalanceToman
                if (remaining != null) {
                    if (existingAcc != null) {
                        dao.updateBankAccount(existingAcc.copy(balanceToman = remaining))
                    } else {
                        dao.insertBankAccount(
                            BankAccountEntity(
                                bankCode = sms.bankCode,
                                cardNumber = sms.cardHint.orEmpty(),
                                accountTitle = "حساب پیامکی",
                                balanceToman = remaining
                            )
                        )
                    }
                } else if (existingAcc != null) {
                    val delta = if (sms.isIncome) sms.amountToman else -sms.amountToman
                    dao.updateBankAccount(existingAcc.copy(balanceToman = existingAcc.balanceToman + delta))
                }

                // Trigger real-time persistent backup into "من را حذف نکنید"
                val backupManager = DriveBackupManager(appCtx, dao)
                val writeRes = backupManager.writeRealtimeAutoBackup(
                    savedTreeUri = prefs.autoBackupTreeUri,
                    savedMediaUri = prefs.autoBackupMediaUri
                )
                writeRes.onSuccess { res ->
                    prefsRepo.setLastAutoBackupState(System.currentTimeMillis(), res.savedMediaUri)
                }

                val formattedAmount = JalaliCalendar.formatCurrency(sms.amountToman, prefs.showInRial)
                BankSmsNotificationHelper.showQuickSavedConfirmation(
                    context = appCtx,
                    pendingSmsId = pendingSmsId,
                    txTitle = replyText,
                    formattedAmount = formattedAmount
                )
            } catch (t: Throwable) {
                Log.e("NotificationReply", "Failed saving quick reply from notification", t)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Throwable) {
                    // Ignore already finished
                }
            }
        }
    }
}
