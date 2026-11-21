package com.example.scanner

import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar
import java.math.BigInteger

data class ScannedBankCard(
    val cardNumber: String?,
    val formattedCardNumber: String?,
    val shebaNumber: String?,
    val formattedShebaNumber: String?,
    val expiryDate: String?,
    val detectedBank: IranianBank?
) {
    val hasAnyInfo: Boolean
        get() = !cardNumber.isNullOrBlank() || !shebaNumber.isNullOrBlank()
}

/**
 * Pure Kotlin parser for OCR text captured from Iranian bank cards.
 * Supports:
 * - 16-digit Shetab card numbers in horizontal (4x4), 2-line (8+8), and vertical/stacked 4-digits-per-line (4x4 across lines) layouts
 * - 26-char Iranian Sheba / IBAN numbers (IR + 24 digits, ISO 7064 Mod-97 validated)
 * - Expiry date (YY/MM or MM/YY)
 * - Issuing Iranian or custom bank detection from Card BIN or Sheba bank code
 */
object BankCardTextParser {

    fun parse(
        rawText: String,
        customBanks: List<IranianBank> = IranianBanks.customBanks()
    ): ScannedBankCard? {
        if (rawText.isBlank()) return null

        val normalized = JalaliCalendar.toLatinDigits(rawText)
        val sheba = extractShebaNumber(normalized)
        val cardNumber = extractCardNumber(normalized, sheba, customBanks)

        if (cardNumber == null && sheba == null) return null

        val expiry = extractExpiryDate(normalized)
        val bank = cardNumber?.let { IranianBanks.detectByCardNumber(it, customBanks) }
            ?: sheba?.let { IranianBanks.detectBySheba(it, customBanks) }

        return ScannedBankCard(
            cardNumber = cardNumber,
            formattedCardNumber = cardNumber?.let { formatCardNumber(it) },
            shebaNumber = sheba,
            formattedShebaNumber = sheba?.let { formatShebaNumber(it) },
            expiryDate = expiry,
            detectedBank = bank
        )
    }

    /**
     * Merges a newly parsed camera frame into previously accumulated findings so card number and Sheba
     * can be captured cleanly across separate frames without losing previously detected fields.
     */
    fun merge(
        previous: ScannedBankCard?,
        incoming: ScannedBankCard,
        customBanks: List<IranianBank> = IranianBanks.customBanks()
    ): ScannedBankCard {
        if (previous == null) return incoming
        val card = incoming.cardNumber ?: previous.cardNumber
        val sheba = incoming.shebaNumber ?: previous.shebaNumber
        val expiry = incoming.expiryDate ?: previous.expiryDate
        val bank = card?.let { IranianBanks.detectByCardNumber(it, customBanks) }
            ?: sheba?.let { IranianBanks.detectBySheba(it, customBanks) }
            ?: incoming.detectedBank
            ?: previous.detectedBank
        return ScannedBankCard(
            cardNumber = card,
            formattedCardNumber = card?.let { formatCardNumber(it) },
            shebaNumber = sheba,
            formattedShebaNumber = sheba?.let { formatShebaNumber(it) },
            expiryDate = expiry,
            detectedBank = bank
        )
    }

    private fun extractCardNumber(
        text: String,
        detectedSheba: String?,
        customBanks: List<IranianBank>
    ): String? {
        val candidates = LinkedHashSet<String>()
        val shebaDigits = detectedSheba?.removePrefix("IR")

        // 1. Standard 4x4 grouped card number (supports spaces, dashes, dots, or newlines between 4-digit blocks)
        val groupedRegex = Regex("""(?<!\d)(\d{4})[\s\-._/\r\n]{0,4}(\d{4})[\s\-._/\r\n]{0,4}(\d{4})[\s\-._/\r\n]{0,4}(\d{4})(?!\d)""")
        for (match in groupedRegex.findAll(text)) {
            val digits = match.groupValues.drop(1).joinToString("")
            if (digits.length == 16 && !isSubstringOfSheba(digits, shebaDigits)) {
                candidates.add(digits)
            }
        }

        // 2. Line-by-line analysis after fixing OCR lookalike chars (O->0, I/l->1, S->5, B->8, Z->2)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val nonShebaLines = lines.filterNot { looksLikeShebaLine(it) }
        val lineDigitsList = nonShebaLines.map { line ->
            fixOcrDigits(line).filter { it.isDigit() }
        }

        for (i in lineDigitsList.indices) {
            val lineDigits = lineDigitsList[i]
            if (lineDigits.length == 16 && !isSubstringOfSheba(lineDigits, shebaDigits)) {
                candidates.add(lineDigits)
            } else if (lineDigits.length in 17..22) {
                for (start in 0..(lineDigits.length - 16)) {
                    val sub = lineDigits.substring(start, start + 16)
                    if (!isSubstringOfSheba(sub, shebaDigits)) {
                        candidates.add(sub)
                    }
                }
            }

            // 2-line layout: 8 digits on line i + 8 digits on line i+1
            if (lineDigits.length == 8 && i + 1 < lineDigitsList.size) {
                val nextDigits = lineDigitsList[i + 1]
                if (nextDigits.length == 8) {
                    val combined = lineDigits + nextDigits
                    if (!isSubstringOfSheba(combined, shebaDigits)) {
                        candidates.add(combined)
                    }
                }
            }

            // Vertical / stacked 4-digits-per-line layout (4 consecutive lines of 4 digits)
            if (lineDigits.length == 4 && i + 3 < lineDigitsList.size) {
                val d2 = lineDigitsList[i + 1]
                val d3 = lineDigitsList[i + 2]
                val d4 = lineDigitsList[i + 3]
                if (d2.length == 4 && d3.length == 4 && d4.length == 4) {
                    val combined = lineDigits + d2 + d3 + d4
                    if (!isSubstringOfSheba(combined, shebaDigits)) {
                        candidates.add(combined)
                    }
                }
            }
        }

        // 3. Sliding window over 4-digit tokens across non-Sheba lines (handles stacked 4-digit rows
        // even if OCR places another short label on an adjacent line or splits blocks slightly)
        val fourDigitTokens = mutableListOf<String>()
        val tokenRegex = Regex("""(?<!\d)\d{4}(?!\d)""")
        for (line in nonShebaLines) {
            val fixed = fixOcrDigits(line)
            for (m in tokenRegex.findAll(fixed)) {
                fourDigitTokens.add(m.value)
            }
        }
        if (fourDigitTokens.size >= 4) {
            for (i in 0..(fourDigitTokens.size - 4)) {
                val combined = fourDigitTokens[i] + fourDigitTokens[i + 1] + fourDigitTokens[i + 2] + fourDigitTokens[i + 3]
                if (!isSubstringOfSheba(combined, shebaDigits)) {
                    candidates.add(combined)
                }
            }
        }

        // Prefer candidates that BOTH pass Luhn AND match a known/custom Iranian bank BIN
        val validLuhn = candidates.filter { isValidLuhn(it) }
        val knownBankMatch = validLuhn.firstOrNull {
            IranianBanks.detectByCardNumber(it, customBanks) != null
        }
        if (knownBankMatch != null) return knownBankMatch

        // Next prefer Luhn-valid numbers with Iranian Shetab starting digit (5, 6, or 9)
        val shetabMatch = validLuhn.firstOrNull { it.first() in listOf('5', '6', '9') }
        if (shetabMatch != null) return shetabMatch

        return validLuhn.firstOrNull()
    }

    private fun isSubstringOfSheba(digits16: String, shebaDigits: String?): Boolean {
        return shebaDigits != null && shebaDigits.contains(digits16)
    }

    private fun looksLikeShebaLine(line: String): Boolean {
        val upper = line.uppercase()
        return upper.contains("IR") || upper.contains("IBAN") || upper.contains("شبا")
    }

    /**
     * Extracts and ISO 7064 Mod-97 validates an Iranian Sheba (IBAN) number (IR + 24 digits).
     * Tolerates spaces, hyphens, line breaks, and common OCR prefix confusions (1R, lR, |R -> IR).
     */
    fun extractShebaNumber(text: String): String? {
        val normalized = JalaliCalendar.toLatinDigits(text)

        // Match IR (or OCR lookalike 1R/lR/|R) followed by up to ~40 chars containing digits/separators/OCR lookalikes
        val prefixRegex = Regex("""(?i)(?<![A-Z0-9])[I1l|]R[\s\-._:/\r\n]*([0-9OoDdIil|SsBbZz\s\-._/\r\n]{24,42})""")
        for (match in prefixRegex.findAll(normalized)) {
            val rawBody = match.groupValues[1]
            val fixedBody = fixOcrDigitsUnconditional(rawBody).filter { it.isDigit() }
            if (fixedBody.length >= 24) {
                val candidate = "IR" + fixedBody.take(24)
                if (isValidIranianSheba(candidate)) {
                    return candidate
                }
            }
        }

        // Fallback: check concatenated uppercase text in case Sheba was broken cleanly across lines
        val compact = normalized.replace(Regex("""[\s\-._/]+"""), "").uppercase()
        val compactRegex = Regex("""[I1L|]R([0-9ODISBZ]{24})""")
        for (match in compactRegex.findAll(compact)) {
            val digits = fixOcrDigitsUnconditional(match.groupValues[1]).filter { it.isDigit() }
            if (digits.length == 24) {
                val candidate = "IR$digits"
                if (isValidIranianSheba(candidate)) {
                    return candidate
                }
            }
        }

        return null
    }

    /**
     * Validates an Iranian Sheba (IBAN) string (`IR` + 24 digits) using the ISO 7064 Mod-97-10 algorithm.
     */
    fun isValidIranianSheba(rawSheba: String): Boolean {
        val clean = normalizeSheba(rawSheba) ?: return false
        if (clean.length != 26 || !clean.startsWith("IR")) return false
        val body = clean.substring(2)
        if (!body.all { it.isDigit() }) return false
        // ISO 7064: move first 4 chars (IRxx) to end, replacing I=18 and R=27
        val rearranged = clean.substring(4) + "1827" + clean.substring(2, 4)
        return try {
            BigInteger(rearranged).mod(BigInteger.valueOf(97L)) == BigInteger.ONE
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Normalizes raw user/OCR input into canonical `IR` + up to 24 digits (uppercase, no spaces).
     */
    fun normalizeSheba(raw: String): String? {
        val latin = JalaliCalendar.toLatinDigits(raw).uppercase().replace(Regex("""[\s\-._]+"""), "")
        if (latin.isBlank()) return null
        val withPrefix = when {
            latin.startsWith("IR") -> "IR" + latin.drop(2).filter { it.isDigit() }
            latin.all { it.isDigit() } && latin.length == 24 -> "IR$latin"
            else -> return null
        }
        return withPrefix.take(26)
    }

    fun formatShebaNumber(sheba: String): String {
        val clean = normalizeSheba(sheba) ?: return sheba
        return clean.chunked(4).joinToString(" ")
    }

    private fun fixOcrDigits(line: String): String {
        val trimmed = line.trim()
        val digitCount = trimmed.count { it.isDigit() }
        // Support 4-digit stacked lines even when 1 character is misread as O/I/l/S/B/Z
        val shouldFix = digitCount >= 4 || (trimmed.length in 4..7 && digitCount >= 3)
        if (!shouldFix) return line
        return fixOcrDigitsUnconditional(line)
    }

    private fun fixOcrDigitsUnconditional(text: String): String {
        return buildString(text.length) {
            for (ch in text) {
                when (ch) {
                    'O', 'o', 'D' -> append('0')
                    'I', 'i', 'l', '|' -> append('1')
                    'S', 's' -> append('5')
                    'B' -> append('8')
                    'Z', 'z' -> append('2')
                    else -> append(ch)
                }
            }
        }
    }

    private fun extractExpiryDate(text: String): String? {
        // Matches 04/08, 1406/11, 08/29, 07/05 etc.
        val jalaliFull = Regex("""(14\d{2})\s*/\s*(0[1-9]|1[0-2])""").find(text)
        if (jalaliFull != null) {
            val yy = jalaliFull.groupValues[1].takeLast(2)
            val mm = jalaliFull.groupValues[2]
            return "$yy/$mm"
        }

        val slashDate = Regex("""(?<!\d)(\d{2})\s*/\s*(\d{2})(?!\d)""").findAll(text)
        for (m in slashDate) {
            val first = m.groupValues[1].toIntOrNull() ?: continue
            val second = m.groupValues[2].toIntOrNull() ?: continue
            // Either YY/MM (Iranian cards: e.g. 07/11) or MM/YY (e.g. 11/28)
            val looksValid = (second in 1..12 && first in 0..35) || (first in 1..12 && second in 0..35)
            if (looksValid) {
                return "${m.groupValues[1]}/${m.groupValues[2]}"
            }
        }
        return null
    }

    fun isValidLuhn(cardNumber: String): Boolean {
        if (cardNumber.length != 16 || !cardNumber.all { it.isDigit() }) return false
        var sum = 0
        var alternate = false
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun formatCardNumber(digits: String): String {
        val clean = digits.filter { it.isDigit() }.take(16)
        return clean.chunked(4).joinToString(" - ")
    }
}
