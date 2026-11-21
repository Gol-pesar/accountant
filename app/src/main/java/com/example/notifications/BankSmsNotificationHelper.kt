package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.JalaliCalendar

object BankSmsNotificationHelper {
    const val CHANNEL_ID = "bank_sms_transactions"
    const val CHANNEL_NAME = "تراکنش‌های پیامکی بانک"
    const val CHANNEL_DESC = "اعلان فوری هنگام دریافت پیامک بانکی جدید برای تعیین شرح تراکنش"

    const val EXTRA_PENDING_SMS_ID = "extra_pending_sms_id"
    const val KEY_QUICK_DESCRIPTION = "key_quick_description"
    const val ACTION_QUICK_REPLY_TX = "com.example.action.QUICK_REPLY_BANK_SMS"

    private const val NOTIFICATION_ID_BASE = 41000
    private const val TEST_NOTIFICATION_ID = 40999
    private const val TAG = "BankSmsNotification"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!runtimeOk) return false
        return runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(true)
    }

    fun notificationIdFor(pendingSmsId: Long): Int {
        val offset = ((pendingSmsId and 0x7FFF).toInt()).coerceAtLeast(1)
        return NOTIFICATION_ID_BASE + offset
    }

    fun notifyIncomingBankTransaction(
        context: Context,
        pendingSmsId: Long,
        bankName: String,
        isIncome: Boolean,
        amountToman: Long,
        remainingBalanceToman: Long?,
        showInRial: Boolean,
        rawBody: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val txTypeLabel = if (isIncome) "واریز" else "برداشت"
        val formattedAmount = JalaliCalendar.formatCurrency(amountToman, showInRial)
        val title = "$bankName • $txTypeLabel $formattedAmount"
        val balanceLine = remainingBalanceToman?.let {
            "\nمانده حساب: ${JalaliCalendar.formatCurrency(it, showInRial)}"
        }.orEmpty()
        val shortText = "شرح این تراکنش را تعیین کنید (لمس کنید یا پاسخ سریع بدهید)"
        val bigText = "پیامک جدید از $bankName شناسایی شد.\n$txTypeLabel به مبلغ $formattedAmount$balanceLine\n\nبرای تعیین شرح، دسته‌بندی یا شخص مرتبط ضربه بزنید یا با «تعیین شرح فوری» همین‌جا بنویسید."

        val notifId = notificationIdFor(pendingSmsId)

        // Tap notification -> open MainActivity directly on this pending SMS detail dialog
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_PENDING_SMS_ID, pendingSmsId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Inline quick reply action so user can type the transaction description directly in the notification shade
        val remoteInput = RemoteInput.Builder(KEY_QUICK_DESCRIPTION)
            .setLabel("شرح تراکنش (مثلاً خرید سوپرمارکت)...")
            .build()

        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = ACTION_QUICK_REPLY_TX
            putExtra(EXTRA_PENDING_SMS_ID, pendingSmsId)
        }
        val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 10000,
            replyIntent,
            replyFlags
        )

        val quickReplyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_hesabdar,
            "تعیین شرح فوری",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()

        val fullDetailsAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_hesabdar,
            "تعیین جزئیات کامل",
            openPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hesabdar)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(quickReplyAction)
            .addAction(fullDetailsAction)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Notification permission missing while posting bank SMS alert", se)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to post bank SMS notification", t)
        }
    }

    fun showQuickSavedConfirmation(
        context: Context,
        pendingSmsId: Long,
        txTitle: String,
        formattedAmount: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)
        val notifId = notificationIdFor(pendingSmsId)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hesabdar)
            .setContentTitle("تراکنش ثبت شد")
            .setContentText("«$txTitle» ($formattedAmount) با موفقیت ثبت و پشتیبان‌گیری شد")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(4000L)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: Throwable) {
            // Ignore if revoked
        }
    }

    fun sendTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_PENDING_SMS_ID, -1L)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hesabdar)
            .setContentTitle("اعلان فوری پیامک‌های بانکی فعال است")
            .setContentText("از این پس هر پیامک بانکی که دریافت شود، بلافاصله همین‌جا از شما شرح تراکنش پرسیده می‌شود.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "اعلان فوری حسابدار من فعال شد. هر پیامک بانکی که برسد بلافاصله با دکمه «تعیین شرح فوری» و «تعیین جزئیات کامل» در اعلانات نمایش داده می‌شود."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun cancelNotification(context: Context, pendingSmsId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationIdFor(pendingSmsId))
        }
    }
}
