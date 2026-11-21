package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AccountingDatabase
import com.example.data.local.PendingSmsEntity
import com.example.data.local.PreferencesRepository
import com.example.domain.IranianSmsParser
import com.example.domain.ParsedTxType
import com.example.notifications.BankSmsNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Listens for incoming Iranian bank SMS messages, queues every new transaction in `pending_sms`,
 * and immediately alerts the user via a high-priority notification so they can specify its description.
 */
class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (t: Throwable) {
            Log.w("BankSmsReceiver", "Unable to extract SMS messages from intent", t)
            null
        }
        if (messages.isNullOrEmpty()) return

        // Concatenate multi-part SMS from the same sender
        val grouped = messages.groupBy { it.originatingAddress.orEmpty() }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val db = AccountingDatabase.getInstance(appCtx)
                val dao = db.accountingDao()
                val prefsRepo = PreferencesRepository(appCtx)
                val prefs = prefsRepo.preferencesFlow.first()
                val customBanks = dao.getCustomBanksOnce().map { it.toIranianBank() }
                var maxTimestamp = prefs.smsBaselineTimestampMillis

                for ((sender, parts) in grouped) {
                    runCatching {
                        val fullBody = parts.joinToString("") { it.messageBody.orEmpty() }
                        val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                        val parsed = IranianSmsParser.parse(
                            sender = sender,
                            body = fullBody,
                            timestampMillis = timestamp,
                            customBanks = customBanks
                        ) ?: return@runCatching

                        if (dao.hasTransactionWithSmsHash(parsed.smsHash)) return@runCatching

                        val isIncome = parsed.type == ParsedTxType.INCOME
                        val insertedId = dao.insertPendingSms(
                            PendingSmsEntity(
                                smsHash = parsed.smsHash,
                                sender = parsed.sender,
                                rawBody = parsed.rawBody,
                                bankCode = parsed.bank.code,
                                isIncome = isIncome,
                                amountToman = parsed.amountToman,
                                remainingBalanceToman = parsed.remainingBalanceToman,
                                cardHint = parsed.cardOrAccountHint,
                                suggestedCategoryName = parsed.suggestedCategoryName,
                                timestampMillis = parsed.timestampMillis,
                                status = "PENDING"
                            )
                        )
                        if (insertedId > 0L) {
                            BankSmsNotificationHelper.notifyIncomingBankTransaction(
                                context = appCtx,
                                pendingSmsId = insertedId,
                                bankName = parsed.bank.persianName,
                                isIncome = isIncome,
                                amountToman = parsed.amountToman,
                                remainingBalanceToman = parsed.remainingBalanceToman,
                                showInRial = prefs.showInRial,
                                rawBody = parsed.rawBody
                            )
                        }
                        if (parsed.timestampMillis > maxTimestamp) {
                            maxTimestamp = parsed.timestampMillis
                        }
                    }.onFailure { err ->
                        Log.w("BankSmsReceiver", "Error handling incoming SMS", err)
                    }
                }

                // Advance baseline timestamp if baseline was already established
                if (prefs.smsBaselineTimestampMillis > 0L && maxTimestamp > prefs.smsBaselineTimestampMillis) {
                    prefsRepo.setSmsBaselineTimestampMillis(maxTimestamp)
                }
            } catch (t: Throwable) {
                Log.e("BankSmsReceiver", "Unexpected error in BankSmsReceiver", t)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Throwable) {
                    // Ignore already-finished PendingResult
                }
            }
        }
    }
}
