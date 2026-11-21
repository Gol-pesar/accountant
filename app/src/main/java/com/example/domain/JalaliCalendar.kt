package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

data class JalaliDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int
) : Comparable<JalaliDate> {
    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    val monthName: String
        get() = JalaliCalendar.MONTH_NAMES.getOrElse(month - 1) { "" }

    fun formatFull(): String =
        "${JalaliCalendar.toPersianDigits(day)} $monthName ${JalaliCalendar.toPersianDigits(year)}"

    fun formatShort(): String =
        "${JalaliCalendar.toPersianDigits(year)}/${JalaliCalendar.toPersianDigits(month.toString().padStart(2, '0'))}/${JalaliCalendar.toPersianDigits(day.toString().padStart(2, '0'))}"

    fun formatDayMonth(): String =
        "${JalaliCalendar.toPersianDigits(day)} $monthName"

    fun formatMonthYear(): String =
        "$monthName ${JalaliCalendar.toPersianDigits(year)}"
}

data class JalaliYearMonth(
    val year: Int,
    val month: Int // 1..12
) {
    val monthName: String
        get() = JalaliCalendar.MONTH_NAMES.getOrElse(month - 1) { "" }

    fun format(): String = "$monthName ${JalaliCalendar.toPersianDigits(year)}"

    fun previous(): JalaliYearMonth =
        if (month == 1) JalaliYearMonth(year - 1, 12) else JalaliYearMonth(year, month - 1)

    fun next(): JalaliYearMonth =
        if (month == 12) JalaliYearMonth(year + 1, 1) else JalaliYearMonth(year, month + 1)

    fun daysInMonth(): Int = JalaliCalendar.daysInJalaliMonth(year, month)
}

object JalaliCalendar {
    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun today(): JalaliDate = fromEpochMillis(System.currentTimeMillis())

    fun currentYearMonth(): JalaliYearMonth {
        val t = today()
        return JalaliYearMonth(t.year, t.month)
    }

    fun fromEpochMillis(epochMillis: Long): JalaliDate {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun toEpochMillis(date: JalaliDate, hour: Int = 12, minute: Int = 0): Long {
        val (gy, gm, gd) = jalaliToGregorian(date.year, date.month, date.day)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun monthRangeMillis(ym: JalaliYearMonth): Pair<Long, Long> {
        val start = toEpochMillis(JalaliDate(ym.year, ym.month, 1), hour = 0, minute = 0)
        val nextYm = ym.next()
        val endExclusive = toEpochMillis(JalaliDate(nextYm.year, nextYm.month, 1), hour = 0, minute = 0)
        return start to (endExclusive - 1L)
    }

    fun lastNMonths(anchor: JalaliYearMonth, count: Int): List<JalaliYearMonth> {
        val result = ArrayList<JalaliYearMonth>(count)
        var cur = anchor
        repeat(count) {
            result.add(cur)
            cur = cur.previous()
        }
        return result.reversed()
    }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
            1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            jp = jm
        }
        var n = jy - jp
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return leap == 0
    }

    fun daysInJalaliMonth(jy: Int, jm: Int): Int = when {
        jm in 1..6 -> 31
        jm in 7..11 -> 30
        jm == 12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> 30
    }

    /**
     * Standard Kazimierz Borkowski / Jalaali algorithm for Gregorian to Jalali conversion.
     */
    fun gregorianToJalali(gyInput: Int, gm: Int, gd: Int): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gy = gyInput
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(jyInput: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy = jyInput + 1595
        var days = -355668 + (365 * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4) + jd +
            if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(
            0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 1
        while (gm <= 12 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return Triple(gy, gm, gd)
    }

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(PERSIAN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())

    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Formats an internal Toman amount to Toman or Rial with thousand separators and Persian digits.
     */
    fun formatCurrency(amountToman: Long, showInRial: Boolean = false, withUnit: Boolean = true): String {
        val displayVal = if (showInRial) amountToman * 10L else amountToman
        val isNegative = displayVal < 0
        val absVal = kotlin.math.abs(displayVal)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatted = DecimalFormat("#,###", symbols).format(absVal)
        val persian = toPersianDigits(formatted)
        val signed = if (isNegative) "-$persian" else persian
        if (!withUnit) return signed
        val unit = if (showInRial) "ریال" else "تومان"
        return "$signed $unit"
    }

    fun formatCompactCurrency(amountToman: Long, showInRial: Boolean = false): String {
        val v = kotlin.math.abs(if (showInRial) amountToman * 10L else amountToman)
        return when {
            v >= 1_000_000_000L -> "${toPersianDigits(String.format(Locale.US, "%.1f", v / 1_000_000_000.0))} م"
            v >= 1_000_000L -> "${toPersianDigits(String.format(Locale.US, "%.1f", v / 1_000_000.0))} م"
            v >= 1_000L -> "${toPersianDigits((v / 1_000L).toString())} هـ"
            else -> toPersianDigits(v.toString())
        }
    }
}
