package com.example.domain

import java.security.MessageDigest

enum class ParsedTxType {
    EXPENSE,
    INCOME
}

data class ParsedBankSms(
    val smsHash: String,
    val sender: String,
    val rawBody: String,
    val bank: IranianBank,
    val type: ParsedTxType,
    val amountToman: Long,
    val originalAmountInRial: Boolean,
    val remainingBalanceToman: Long?,
    val cardOrAccountHint: String?,
    val suggestedCategoryName: String,
    val timestampMillis: Long
)

object IranianSmsParser {

    private val EXPENSE_KEYWORDS = listOf(
        "برداشت", "خرید", "خريد", "کسر", "كسر", "پرداخت", "کاهش", "كاهش",
        "بدهکار", "بدهكار", "انتقال به", "کارت به کارت", "برداشت از", "خروجی",
        "انتقال وجه", "قسط", "کارمزد", "كارمزد", "قبض", "شارژ", "پایا از", "ساتنا از",
        "-"
    )

    private val INCOME_KEYWORDS = listOf(
        "واریز", "واريز", "افزایش", "افزايش", "بستانکار", "بستانكار", "حقوق",
        "دریافت", "دريافت", "برگشت", "سود", "ورودی", "تراکنش مثبت", "+"
    )

    private val BANK_INDICATOR_KEYWORDS = listOf(
        "مانده", "موجودی", "موجودي", "برداشت", "واریز", "واريز", "کارت", "كارت",
        "حساب", "تراکنش", "تراكنش", "ریال", "ريال", "تومان", "تومن", "انتقال", "خرید"
    )

    private val OTP_NOISE_KEYWORDS = listOf(
        "رمز پویا", "رمز یکبار", "رمز دوم", "کد تایید", "کد تأیید", "رمز:", "otp", "رمز ورود"
    )

    fun normalizeText(raw: String): String {
        val latinDigits = JalaliCalendar.toLatinDigits(raw)
        return latinDigits
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('٬', ',')
            .replace('،', ',')
            .replace('\u200C', ' ')
            .trim()
    }

    fun parse(
        sender: String,
        body: String,
        timestampMillis: Long = System.currentTimeMillis(),
        customBanks: List<IranianBank> = IranianBanks.customBanks()
    ): ParsedBankSms? = runCatching {
        val safeBody = if (body.length > 4000) body.take(4000) else body
        val normalized = normalizeText(safeBody)
        if (normalized.length < 10) return@runCatching null

        // Ignore OTP / dynamic password SMS
        val lower = normalized.lowercase()
        if (OTP_NOISE_KEYWORDS.any { lower.contains(it.lowercase()) }) {
            return@runCatching null
        }

        // Must look like a banking transaction SMS
        val hasBankIndicator = BANK_INDICATOR_KEYWORDS.count { normalized.contains(it) } >= 2
        val detectedBank = IranianBanks.detectBySms(sender, normalized, customBanks)
        if (detectedBank == null && !hasBankIndicator) return@runCatching null

        // Determine transaction type (Expense vs Income)
        val type = detectTransactionType(normalized) ?: return@runCatching null

        // Extract transaction amount and balance
        val (amountRaw, isTomanExplicit) = extractTransactionAmount(normalized) ?: return@runCatching null
        if (amountRaw <= 0L) return@runCatching null

        val balanceRaw = extractRemainingBalance(normalized)
        val isRial = !isTomanExplicit && (normalized.contains("ریال") || normalized.contains("IRR", ignoreCase = true) || amountRaw >= 10_000L)
        val amountToman = if (isRial) (amountRaw / 10L).coerceAtLeast(1L) else amountRaw
        val balanceToman = balanceRaw?.let { if (isRial) it / 10L else it }

        val cardOrAccount = extractAccountOrCardHint(normalized)
        val bank = detectedBank ?: IranianBanks.findByCode("CASH")
        val suggestedCategory = suggestCategory(normalized, type)
        val safeTimestamp = if (timestampMillis > 0L) timestampMillis else System.currentTimeMillis()
        val hash = computeHash("$sender|$amountRaw|${safeTimestamp / 60_000L}|${normalized.take(60)}")

        ParsedBankSms(
            smsHash = hash,
            sender = sender,
            rawBody = safeBody.trim(),
            bank = bank,
            type = type,
            amountToman = amountToman,
            originalAmountInRial = isRial,
            remainingBalanceToman = balanceToman,
            cardOrAccountHint = cardOrAccount,
            suggestedCategoryName = suggestedCategory,
            timestampMillis = safeTimestamp
        )
    }.getOrNull()

    private fun detectTransactionType(text: String): ParsedTxType? {
        val lines = text.lines()
        for (line in lines) {
            if (line.contains("مانده") || line.contains("موجودی")) continue
            if (Regex("""[+]\s*[\d,]{3,}""").containsMatchIn(line)) return ParsedTxType.INCOME
            if (Regex("""[-]\s*[\d,]{3,}""").containsMatchIn(line)) return ParsedTxType.EXPENSE
        }

        val hasIncome = INCOME_KEYWORDS.filter { it != "+" }.any { text.contains(it) }
        val hasExpense = EXPENSE_KEYWORDS.filter { it != "-" }.any { text.contains(it) }

        return when {
            hasIncome && !hasExpense -> ParsedTxType.INCOME
            hasExpense && !hasIncome -> ParsedTxType.EXPENSE
            hasIncome && hasExpense -> {
                // Whichever appears earlier in the SMS usually describes the primary action
                val firstIncome = INCOME_KEYWORDS.filter { it != "+" }
                    .mapNotNull { kw -> text.indexOf(kw).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                val firstExpense = EXPENSE_KEYWORDS.filter { it != "-" }
                    .mapNotNull { kw -> text.indexOf(kw).takeIf { it >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                if (firstIncome < firstExpense) ParsedTxType.INCOME else ParsedTxType.EXPENSE
            }
            else -> null
        }
    }

    private fun extractTransactionAmount(text: String): Pair<Long, Boolean>? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Look for labeled amount lines (مبلغ, برداشت, واریز, خرید, انتقال, + / -) excluding مانده/موجودی
        val labeledPatterns = listOf(
            Regex("""(?:مبلغ|برداشت|واریز|خرید|انتقال|پرداخت|کسر|افزایش|کاهش)\s*:?\s*[+\-]?\s*([\d,]{3,15})\s*(تومان|تومن|ریال)?"""),
            Regex("""[+\-]\s*([\d,]{3,15})\s*(تومان|تومن|ریال)?""")
        )
        for (line in lines) {
            if (line.contains("مانده") || line.contains("موجودی")) continue
            for (pattern in labeledPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    val value = parseNumber(match.groupValues[1])
                    val unit = match.groupValues.getOrNull(2).orEmpty()
                    if (value != null && value >= 100L) {
                        val isToman = unit.contains("توم") || (!unit.contains("ریال") && line.contains("تومان"))
                        return value to isToman
                    }
                }
            }
        }

        // 2. Fallback: number followed by ریال or تومان not on a balance line
        val unitRegex = Regex("""([\d,]{3,15})\s*(ریال|تومان|تومن)""")
        for (line in lines) {
            if (line.contains("مانده") || line.contains("موجودی")) continue
            val match = unitRegex.find(line)
            if (match != null) {
                val value = parseNumber(match.groupValues[1])
                val unit = match.groupValues[2]
                if (value != null && value >= 100L) {
                    return value to unit.contains("توم")
                }
            }
        }

        return null
    }

    private fun extractRemainingBalance(text: String): Long? {
        val balanceRegex = Regex("""(?:مانده|موجودی)\s*:?\s*([\d,]{3,16})""")
        val match = balanceRegex.find(text) ?: return null
        return parseNumber(match.groupValues[1])
    }

    private fun extractAccountOrCardHint(text: String): String? {
        val maskedCard = Regex("""(\d{4})\*+(\d{4})""").find(text)
        if (maskedCard != null) return "کارت ${maskedCard.groupValues[2]}****"

        val accountLabeled = Regex("""(?:حساب|کارت|شماره)\s*:?\s*([\d.*\-/]{4,20})""").find(text)
        if (accountLabeled != null) {
            val digits = accountLabeled.groupValues[1].filter { it.isDigit() }
            if (digits.length >= 4) return digits.takeLast(4)
        }
        return null
    }

    private fun suggestCategory(text: String, type: ParsedTxType): String {
        if (type == ParsedTxType.INCOME) {
            return when {
                text.contains("حقوق") || text.contains("دستمزد") -> "حقوق و دستمزد"
                text.contains("سود") -> "سرمایه‌گذاری"
                else -> "درآمد متفرقه"
            }
        }
        return when {
            listOf("اسنپ", "تپسی", "بنزین", "سوخت", "تاکسی", "مترو", "بلیت", "پرواز").any { text.contains(it) } -> "حمل‌ونقل"
            listOf("رستوران", "کافه", "سوپر", "هایپر", "افق کوروش", "جانبو", "رفاه", "نان", "فست فود", "غذا").any { text.contains(it) } -> "خوراک و رستوران"
            listOf("قبض", "برق", "گاز", "آب", "همراه اول", "ایرانسل", "رایتل", "اینترنت", "شارژ").any { text.contains(it) } -> "قبوض و اینترنت"
            listOf("داروخانه", "بیمارستان", "درمانگاه", "پزشک", "آزمایشگاه", "دندان").any { text.contains(it) } -> "درمان و سلامت"
            listOf("دیجی کالا", "دیجی‌کالا", "باسلام", "پوشاک", "فروشگاه", "خرید").any { text.contains(it) } -> "خرید و پوشاک"
            listOf("اجاره", "شارژ ساختمان", "مسکن").any { text.contains(it) } -> "مسکن و اجاره"
            else -> "خرید و پوشاک"
        }
    }

    private fun parseNumber(raw: String): Long? =
        raw.replace(",", "").trim().toLongOrNull()

    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }
}
