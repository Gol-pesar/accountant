package com.example.scanner

import com.example.domain.IranianBank
import com.example.domain.IranianBanks
import com.example.domain.JalaliCalendar

data class ScannedBankCard(
    val cardNumber: String,
    val formattedCardNumber: String,
    val expiryDate: String?,
    val detectedBank: IranianBank?
)

/**
 * Pure Kotlin parser for OCR text captured from Iranian bank cards.
 * Extracts 16-digit Shetab card numbers (validated with Luhn checksum),
 * expiry date (MM/YY or YY/MM), and matches the issuing Iranian or custom bank.
 */
object BankCardTextParser {

    fun parse(
        rawText: String,
        customBanks: List<IranianBank> = IranianBanks.customBanks()
    ): ScannedBankCard? {
        if (rawText.isBlank()) return null

        val normalized = JalaliCalendar.toLatinDigits(rawText)
        val cardNumber = extractCardNumber(normalized, customBanks) ?: return null
        val expiry = extractExpiryDate(normalized)
        val bank = IranianBanks.detectByCardNumber(cardNumber, customBanks)

        return ScannedBankCard(
            cardNumber = cardNumber,
            formattedCardNumber = formatCardNumber(cardNumber),
            expiryDate = expiry,
            detectedBank = bank
        )
    }

    private fun extractCardNumber(text: String, customBanks: List<IranianBank>): String? {
        val candidates = LinkedHashSet<String>()

        // 1. Standard 4x4 grouped card number on one or across adjacent lines
        val groupedRegex = Regex("""(?<!\d)(\d{4})[\s\-._/]{0,3}(\d{4})[\s\-._/]{0,3}(\d{4})[\s\-._/]{0,3}(\d{4})(?!\d)""")
        for (match in groupedRegex.findAll(text)) {
            val digits = match.groupValues.drop(1).joinToString("")
            if (digits.length == 16) candidates.add(digits)
        }

        // 2. Check line-by-line after correcting common OCR look-alike chars (O->0, I/l->1, S->5, B->8)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (i in lines.indices) {
            val fixedLine = fixOcrDigits(lines[i])
            val lineDigits = fixedLine.filter { it.isDigit() }
            if (lineDigits.length == 16) {
                candidates.add(lineDigits)
            } else if (lineDigits.length > 16) {
                for (start in 0..(lineDigits.length - 16)) {
                    candidates.add(lineDigits.substring(start, start + 16))
                }
            }
            // Some Iranian cards print 8 digits on one line and 8 digits on the next line
            if (lineDigits.length == 8 && i + 1 < lines.size) {
                val nextDigits = fixOcrDigits(lines[i + 1]).filter { it.isDigit() }
                if (nextDigits.length == 8) {
                    candidates.add(lineDigits + nextDigits)
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

    private fun fixOcrDigits(line: String): String {
        val digitCount = line.count { it.isDigit() }
        if (digitCount < 4) return line
        return buildString(line.length) {
            for (ch in line) {
                when (ch) {
                    'O', 'o', 'D' -> append('0')
                    'I', 'l', '|' -> append('1')
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
