package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.AccountingDatabase
import com.example.data.local.PendingSmsEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.TransactionEntity
import com.example.domain.IranianSmsParser
import com.example.domain.ParsedTxType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Listens for incoming Iranian bank SMS messages and queues them for review
 * (or auto-logs them if the user enabled auto-approve in settings).
 */
class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Concatenate multi-part SMS from the same sender
        val grouped = messages.groupBy { it.originatingAddress.orEmpty() }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = AccountingDatabase.getInstance(context.applicationContext)
                val dao = db.accountingDao()
                val prefs = PreferencesRepository(context.applicationContext).preferencesFlow.first()
                val categories = dao.getCategoriesOnce()

                for ((sender, parts) in grouped) {
                    val fullBody = parts.joinToString("") { it.messageBody.orEmpty() }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    val parsed = IranianSmsParser.parse(sender, fullBody, timestamp) ?: continue

                    if (dao.hasTransactionWithSmsHash(parsed.smsHash)) continue

                    if (prefs.autoApproveSms) {
                        val isIncome = parsed.type == ParsedTxType.INCOME
                        val matchedCat = categories.firstOrNull {
                            it.isIncome == isIncome && it.name == parsed.suggestedCategoryName
                        } ?: categories.firstOrNull { it.isIncome == isIncome }

                        dao.insertTransaction(
                            TransactionEntity(
                                title = "${parsed.bank.shortName} - ${if (isIncome) "واریز پیامکی" else "برداشت پیامکی"}",
                                amountToman = parsed.amountToman,
                                isIncome = isIncome,
                                categoryId = matchedCat?.id,
                                bankCode = parsed.bank.code,
                                timestampMillis = parsed.timestampMillis,
                                note = parsed.cardOrAccountHint?.let { "شماره: $it" } ?: "",
                                fromSms = true,
                                smsHash = parsed.smsHash
                            )
                        )
                        // Update bank balance if account exists and balance was parsed
                        if (parsed.remainingBalanceToman != null) {
                            val acc = dao.findBankAccountByCode(parsed.bank.code)
                            if (acc != null) {
                                dao.updateBankAccount(acc.copy(balanceToman = parsed.remainingBalanceToman))
                            }
                        }
                    } else {
                        dao.insertPendingSms(
                            PendingSmsEntity(
                                smsHash = parsed.smsHash,
                                sender = parsed.sender,
                                rawBody = parsed.rawBody,
                                bankCode = parsed.bank.code,
                                isIncome = parsed.type == ParsedTxType.INCOME,
                                amountToman = parsed.amountToman,
                                remainingBalanceToman = parsed.remainingBalanceToman,
                                cardHint = parsed.cardOrAccountHint,
                                suggestedCategoryName = parsed.suggestedCategoryName,
                                timestampMillis = parsed.timestampMillis,
                                status = "PENDING"
                            )
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
