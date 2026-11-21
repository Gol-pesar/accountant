package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.IranianSmsParser
import com.example.domain.ParsedBankSms
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsInboxReader(private val context: Context) {

    fun hasReadSmsPermission(): Boolean = try {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    /**
     * Reads recent SMS messages from the device inbox and parses Iranian banking transactions.
     * Guarded per-row and against OEM/Restricted-Settings SecurityExceptions so it can never crash.
     */
    suspend fun scanRecentBankSms(
        maxMessages: Int = 250,
        sinceTimestampMillis: Long = 0L,
        customBanks: List<IranianBank> = IranianBanks.customBanks()
    ): Result<List<ParsedBankSms>> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) {
            return@withContext Result.failure(SecurityException("دسترسی خواندن پیامک صادر نشده است"))
        }
        try {
            val parsed = mutableListOf<ParsedBankSms>()
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val selection = if (sinceTimestampMillis > 0L) "${Telephony.Sms.DATE} > ?" else null
            val selectionArgs = if (sinceTimestampMillis > 0L) arrayOf(sinceTimestampMillis.toString()) else null
            val cursor = try {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC"
                )
            } catch (sec: SecurityException) {
                Log.w("SmsInboxReader", "SMS inbox blocked by system/OEM restricted settings", sec)
                return@withContext Result.failure(
                    SecurityException("سیستم اندروید دسترسی پیامک را محدود کرده است. از تنظیمات برنامه، مجوز پیامک را فعال کنید.")
                )
            }

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                var count = 0
                while (count < maxMessages) {
                    val hasNext = try {
                        c.moveToNext()
                    } catch (t: Throwable) {
                        Log.w("SmsInboxReader", "Cursor moveToNext stopped early", t)
                        false
                    }
                    if (!hasNext) break
                    count++

                    val rawDate = if (dateIdx >= 0 && !c.isNull(dateIdx)) {
                        c.getLong(dateIdx)
                    } else System.currentTimeMillis()
                    val safeDate = if (rawDate > 0L) rawDate else System.currentTimeMillis()

                    if (sinceTimestampMillis > 0L && safeDate <= sinceTimestampMillis) {
                        break
                    }

                    runCatching {
                        val sender = if (addressIdx >= 0 && !c.isNull(addressIdx)) {
                            c.getString(addressIdx).orEmpty()
                        } else ""
                        val body = if (bodyIdx >= 0 && !c.isNull(bodyIdx)) {
                            c.getString(bodyIdx).orEmpty()
                        } else ""

                        if (body.isNotBlank()) {
                            val sms = IranianSmsParser.parse(
                                sender = sender,
                                body = body,
                                timestampMillis = safeDate,
                                customBanks = customBanks
                            )
                            if (sms != null) {
                                parsed.add(sms)
                            }
                        }
                    }
                }
            }
            Result.success(parsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (sec: SecurityException) {
            Result.failure(
                SecurityException("دسترسی خواندن پیامک توسط اندروید محدود شده است. لطفاً از تنظیمات برنامه مجوز را فعال کنید.")
            )
        } catch (t: Throwable) {
            Log.e("SmsInboxReader", "Failed reading SMS inbox", t)
            Result.failure(IllegalStateException("خطا در خواندن صندوق پیامک گوشی؛ لطفاً دوباره تلاش کنید"))
        }
    }
}
