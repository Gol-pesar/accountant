package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.domain.IranianSmsParser
import com.example.domain.ParsedBankSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsInboxReader(private val context: Context) {

    fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    /**
     * Reads recent SMS messages from the device inbox and parses Iranian banking transactions.
     */
    suspend fun scanRecentBankSms(maxMessages: Int = 250): Result<List<ParsedBankSms>> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) {
            return@withContext Result.failure(SecurityException("دسترسی خواندن پیامک صادر نشده است"))
        }
        runCatching {
            val parsed = mutableListOf<ParsedBankSms>()
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                var count = 0
                while (cursor.moveToNext() && count < maxMessages) {
                    count++
                    val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val sms = IranianSmsParser.parse(sender = sender, body = body, timestampMillis = date)
                    if (sms != null) {
                        parsed.add(sms)
                    }
                }
            }
            parsed
        }
    }
}
