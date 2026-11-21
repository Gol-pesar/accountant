package com.example

import com.example.data.local.CustomBankEntity
import com.example.domain.IranianBanks
import com.example.domain.IranianSmsParser
import com.example.domain.JalaliCalendar
import com.example.domain.ParsedTxType
import com.example.scanner.BankCardTextParser
import com.example.security.SecurityCrypto
import com.example.update.GitHubUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianAccountingDomainTest {

    @Test
    fun parsesMellatWithdrawalRialSmsAccurately() {
        val sms = "بانک ملت\nبرداشت: 2,500,000 ریال\nکارت: 610433****1234\nمانده: 18,400,000 ریال"
        val parsed = IranianSmsParser.parse(sender = "BankMellat", body = sms)
        assertNotNull(parsed)
        assertEquals("MELLAT", parsed!!.bank.code)
        assertEquals(ParsedTxType.EXPENSE, parsed.type)
        assertEquals(250_000L, parsed.amountToman)
        assertEquals(1_840_000L, parsed.remainingBalanceToman)
    }

    @Test
    fun parsesBluBankDepositTomanSmsAccurately() {
        val sms = "بلوبانک\nواریز مبلغ ۵۰۰,۰۰۰ تومان به حساب شما\nموجودی: ۱۲,۰۰۰,۰۰۰ تومان"
        val parsed = IranianSmsParser.parse(sender = "BluBank", body = sms)
        assertNotNull(parsed)
        assertEquals("BLU", parsed!!.bank.code)
        assertEquals(ParsedTxType.INCOME, parsed.type)
        assertEquals(500_000L, parsed.amountToman)
        assertEquals(12_000_000L, parsed.remainingBalanceToman)
    }

    @Test
    fun ignoresOtpDynamicPasswordMessages() {
        val otp = "بانک ملی\nرمز پویا: 482910\nمبلغ: 500,000 ریال"
        val parsed = IranianSmsParser.parse(sender = "BMI", body = otp)
        assertNull(parsed)
    }

    @Test
    fun detectsIranianBanksFromCardBinPrefixes() {
        assertEquals("MELLI", IranianBanks.detectByCardNumber("6037991122334455")?.code)
        assertEquals("BLU", IranianBanks.detectByCardNumber("6219861912345678")?.code)
        assertEquals("SAMAN", IranianBanks.detectByCardNumber("6219861012345678")?.code)
        assertEquals("PASARGAD", IranianBanks.detectByCardNumber("5022291012345678")?.code)
    }

    @Test
    fun convertsGregorianToJalaliAndBackAccurately() {
        // 2026-03-21 is 1405-01-01 (Nowruz 1405)
        val jalali = JalaliCalendar.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, jalali.year)
        assertEquals(1, jalali.month)
        assertEquals(1, jalali.day)

        val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(1405, 1, 1)
        assertEquals(2026, gy)
        assertEquals(3, gm)
        assertEquals(21, gd)
    }

    @Test
    fun normalizesPersianAndArabicSecurityAnswersConsistently() {
        val a = SecurityCrypto.normalizeAnswer("  تهران   ۱۳۷۵ ")
        val b = SecurityCrypto.normalizeAnswer("تهران 1375")
        assertEquals(a, b)
        assertEquals("کیان", SecurityCrypto.normalizeAnswer("كيان"))
    }

    @Test
    fun parsesIranianBankCardNumberAndExpiryFromOcrText() {
        val ocrText = """
            BANK MELLAT
            6104 3378 1234 5674
            انقضا: 07/11
            CVV2: 814
        """.trimIndent()
        val card = BankCardTextParser.parse(ocrText)
        assertNotNull(card)
        assertEquals("6104337812345674", card!!.cardNumber)
        assertEquals("MELLAT", card.detectedBank?.code)
        assertEquals("07/11", card.expiryDate)
    }

    @Test
    fun detectsCustomBankInCardScannerAndSmsParser() {
        val customBank = CustomBankEntity(
            code = "CUSTOM_IZBANK",
            persianName = "بانک ایران زمین",
            shortName = "ایران‌زمین",
            primaryColorHex = 0xFF7B1FA2,
            cardPrefix = "505785",
            smsKeyword = "ایران زمین"
        ).toIranianBank()

        val card = BankCardTextParser.parse(
            rawText = "5057 8510 0000 0015\n08/09",
            customBanks = listOf(customBank)
        )
        assertNotNull(card)
        assertEquals("CUSTOM_IZBANK", card!!.detectedBank?.code)

        val sms = IranianSmsParser.parse(
            sender = "IZBank",
            body = "بانک ایران زمین\nبرداشت 1,200,000 ریال\nمانده 5,000,000 ریال",
            customBanks = listOf(customBank)
        )
        assertNotNull(sms)
        assertEquals("CUSTOM_IZBANK", sms!!.bank.code)
        assertEquals(120_000L, sms.amountToman)
    }

    @Test
    fun comparesGitHubReleaseVersionsAccurately() {
        assertTrue(
            GitHubUpdateChecker.isRemoteNewer(
                remoteTag = "debug-apk-build-10-1",
                remoteTitle = "Debug APK Build #10",
                currentVersionCode = 11,
                currentVersionName = "1.1.9"
            )
        )
        assertFalse(
            GitHubUpdateChecker.isRemoteNewer(
                remoteTag = "debug-apk-build-9-1",
                remoteTitle = "Debug APK Build #9",
                currentVersionCode = 11,
                currentVersionName = "1.1.9"
            )
        )
        assertTrue(
            GitHubUpdateChecker.isRemoteNewer(
                remoteTag = "v1.2.0",
                remoteTitle = "Release 1.2.0",
                currentVersionCode = 2,
                currentVersionName = "1.1.0"
            )
        )
    }
}
