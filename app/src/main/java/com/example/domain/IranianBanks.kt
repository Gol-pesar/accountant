package com.example.domain

enum class BankEmblemType {
    MELLI,
    MELLAT,
    SADERAT,
    TEJARAT,
    SEPAH,
    PASARGAD,
    SAMAN,
    PARSIAN,
    KESHAVARZI,
    MASKAN,
    REFAH,
    AYANDEH,
    BLUBANK,
    SHAHR,
    EGHTESAD_NOVIN,
    SINA,
    DAY,
    POST_BANK,
    RESALAT,
    MEHR_IRAN,
    KARAFARIN,
    GARDESHGARI,
    CASH
}

data class IranianBank(
    val code: String,
    val persianName: String,
    val shortName: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val cardPrefixes: List<String>,
    val smsKeywords: List<String>,
    val senderKeywords: List<String>,
    val emblemType: BankEmblemType
)

object IranianBanks {
    val ALL: List<IranianBank> = listOf(
        IranianBank(
            code = "BLU",
            persianName = "بلوبانک (سامان)",
            shortName = "بلوبانک",
            primaryColorHex = 0xFF007BFF,
            secondaryColorHex = 0xFF00C2FF,
            accentColorHex = 0xFFFFFFFF,
            cardPrefixes = listOf("62198619"),
            smsKeywords = listOf("بلوبانک", "بلو بانک", "blubank", "blu"),
            senderKeywords = listOf("blubank", "blu", "982000", "2000500"),
            emblemType = BankEmblemType.BLUBANK
        ),
        IranianBank(
            code = "MELLI",
            persianName = "بانک ملی ایران",
            shortName = "ملی",
            primaryColorHex = 0xFF003B73,
            secondaryColorHex = 0xFFD4AF37,
            accentColorHex = 0xFFFFD54F,
            cardPrefixes = listOf("603799"),
            smsKeywords = listOf("بانک ملی", "بانك ملی", "بانک ملي", "بام ملی", "bmi"),
            senderKeywords = listOf("bmi", "bankmelli", "melli", "983000999"),
            emblemType = BankEmblemType.MELLI
        ),
        IranianBank(
            code = "MELLAT",
            persianName = "بانک ملت",
            shortName = "ملت",
            primaryColorHex = 0xFFB71C1C,
            secondaryColorHex = 0xFFE53935,
            accentColorHex = 0xFFFFCA28,
            cardPrefixes = listOf("610433", "991975"),
            smsKeywords = listOf("بانک ملت", "بانك ملت", "بهمن ملت", "mellat"),
            senderKeywords = listOf("bankmellat", "mellat", "200033", "300033"),
            emblemType = BankEmblemType.MELLAT
        ),
        IranianBank(
            code = "SADERAT",
            persianName = "بانک صادرات ایران",
            shortName = "صادرات",
            primaryColorHex = 0xFF1A237E,
            secondaryColorHex = 0xFF3949AB,
            accentColorHex = 0xFF8C9EFF,
            cardPrefixes = listOf("603769"),
            smsKeywords = listOf("بانک صادرات", "بانك صادرات", "صادرات", "سپهر", "bsi"),
            senderKeywords = listOf("bsi", "saderat", "sepehr"),
            emblemType = BankEmblemType.SADERAT
        ),
        IranianBank(
            code = "TEJARAT",
            persianName = "بانک تجارت",
            shortName = "تجارت",
            primaryColorHex = 0xFF0D47A1,
            secondaryColorHex = 0xFF1976D2,
            accentColorHex = 0xFF64B5F6,
            cardPrefixes = listOf("627353", "585983"),
            smsKeywords = listOf("بانک تجارت", "بانك تجارت", "تجارت", "tejarat"),
            senderKeywords = listOf("tejarat", "tejaratbank"),
            emblemType = BankEmblemType.TEJARAT
        ),
        IranianBank(
            code = "SEPAH",
            persianName = "بانک سپه",
            shortName = "سپه",
            primaryColorHex = 0xFFC62828,
            secondaryColorHex = 0xFF1565C0,
            accentColorHex = 0xFFFFD54F,
            cardPrefixes = listOf("589210", "627381", "505801", "639599"),
            smsKeywords = listOf("بانک سپه", "بانك سپه", "سپه", "sepah"),
            senderKeywords = listOf("sepah", "banksepah"),
            emblemType = BankEmblemType.SEPAH
        ),
        IranianBank(
            code = "PASARGAD",
            persianName = "بانک پاسارگاد",
            shortName = "پاسارگاد",
            primaryColorHex = 0xFF212121,
            secondaryColorHex = 0xFFFFA000,
            accentColorHex = 0xFFFFD54F,
            cardPrefixes = listOf("502229", "639347"),
            smsKeywords = listOf("بانک پاسارگاد", "پاسارگاد", "ویپاد", "پاد", "bpi", "pasargad"),
            senderKeywords = listOf("bpi", "pasargad", "wepod"),
            emblemType = BankEmblemType.PASARGAD
        ),
        IranianBank(
            code = "SAMAN",
            persianName = "بانک سامان",
            shortName = "سامان",
            primaryColorHex = 0xFF00838F,
            secondaryColorHex = 0xFF26C6DA,
            accentColorHex = 0xFFE0F7FA,
            cardPrefixes = listOf("621986"),
            smsKeywords = listOf("بانک سامان", "بانك سامان", "سامان", "saman", "sb24"),
            senderKeywords = listOf("saman", "sb24", "samanbank"),
            emblemType = BankEmblemType.SAMAN
        ),
        IranianBank(
            code = "PARSIAN",
            persianName = "بانک پارسیان",
            shortName = "پارسیان",
            primaryColorHex = 0xFF880E4F,
            secondaryColorHex = 0xFFD81B60,
            accentColorHex = 0xFFFFC107,
            cardPrefixes = listOf("622106", "639194", "627884"),
            smsKeywords = listOf("بانک پارسیان", "پارسیان", "پارسيان", "parsian"),
            senderKeywords = listOf("parsian", "pec"),
            emblemType = BankEmblemType.PARSIAN
        ),
        IranianBank(
            code = "KESHAVARZI",
            persianName = "بانک کشاورزی",
            shortName = "کشاورزی",
            primaryColorHex = 0xFF1B5E20,
            secondaryColorHex = 0xFF43A047,
            accentColorHex = 0xFFFFEB3B,
            cardPrefixes = listOf("603770", "639217"),
            smsKeywords = listOf("بانک کشاورزی", "کشاورزی", "كشاورزي", "bki", "keshavarzi"),
            senderKeywords = listOf("bki", "keshavarzi", "agri"),
            emblemType = BankEmblemType.KESHAVARZI
        ),
        IranianBank(
            code = "MASKAN",
            persianName = "بانک مسکن",
            shortName = "مسکن",
            primaryColorHex = 0xFFE65100,
            secondaryColorHex = 0xFFFB8C00,
            accentColorHex = 0xFFFFE0B2,
            cardPrefixes = listOf("628023"),
            smsKeywords = listOf("بانک مسکن", "بانك مسكن", "مسکن", "maskan"),
            senderKeywords = listOf("maskan", "bankmaskan"),
            emblemType = BankEmblemType.MASKAN
        ),
        IranianBank(
            code = "REFAH",
            persianName = "بانک رفاه کارگران",
            shortName = "رفاه",
            primaryColorHex = 0xFF004D40,
            secondaryColorHex = 0xFF00897B,
            accentColorHex = 0xFF80CBC4,
            cardPrefixes = listOf("589463"),
            smsKeywords = listOf("بانک رفاه", "رفاه کارگران", "رفاه", "refah"),
            senderKeywords = listOf("refah", "refahbank"),
            emblemType = BankEmblemType.REFAH
        ),
        IranianBank(
            code = "AYANDEH",
            persianName = "بانک آینده",
            shortName = "آینده",
            primaryColorHex = 0xFF4E342E,
            secondaryColorHex = 0xFF8D6E63,
            accentColorHex = 0xFFFFB300,
            cardPrefixes = listOf("636214"),
            smsKeywords = listOf("بانک آینده", "بانك آينده", "آینده", "آينده", "ayandeh"),
            senderKeywords = listOf("ayandeh", "ba24"),
            emblemType = BankEmblemType.AYANDEH
        ),
        IranianBank(
            code = "SHAHR",
            persianName = "بانک شهر",
            shortName = "شهر",
            primaryColorHex = 0xFFB71C1C,
            secondaryColorHex = 0xFFF57C00,
            accentColorHex = 0xFFFFE082,
            cardPrefixes = listOf("504706", "502806"),
            smsKeywords = listOf("بانک شهر", "بانك شهر", "shahr"),
            senderKeywords = listOf("shahr", "shahrbank"),
            emblemType = BankEmblemType.SHAHR
        ),
        IranianBank(
            code = "ENBANK",
            persianName = "بانک اقتصاد نوین",
            shortName = "اقتصاد نوین",
            primaryColorHex = 0xFF4A148C,
            secondaryColorHex = 0xFF8E24AA,
            accentColorHex = 0xFFCE93D8,
            cardPrefixes = listOf("627412"),
            smsKeywords = listOf("اقتصاد نوین", "اقتصادنوين", "enbank"),
            senderKeywords = listOf("enbank", "eghtesad"),
            emblemType = BankEmblemType.EGHTESAD_NOVIN
        ),
        IranianBank(
            code = "SINA",
            persianName = "بانک سینا",
            shortName = "سینا",
            primaryColorHex = 0xFF01579B,
            secondaryColorHex = 0xFF0288D1,
            accentColorHex = 0xFFFFB300,
            cardPrefixes = listOf("639346"),
            smsKeywords = listOf("بانک سینا", "بانك سينا", "سینا", "sina"),
            senderKeywords = listOf("sina", "sinabank"),
            emblemType = BankEmblemType.SINA
        ),
        IranianBank(
            code = "DAY",
            persianName = "بانک دی",
            shortName = "دی",
            primaryColorHex = 0xFF006064,
            secondaryColorHex = 0xFF0097A7,
            accentColorHex = 0xFFFFCA28,
            cardPrefixes = listOf("502938"),
            smsKeywords = listOf("بانک دی", "بانك دي", "daybank"),
            senderKeywords = listOf("daybank", "bankday"),
            emblemType = BankEmblemType.DAY
        ),
        IranianBank(
            code = "POST",
            persianName = "پست بانک ایران",
            shortName = "پست بانک",
            primaryColorHex = 0xFF2E7D32,
            secondaryColorHex = 0xFF66BB6A,
            accentColorHex = 0xFFFFEB3B,
            cardPrefixes = listOf("627760"),
            smsKeywords = listOf("پست بانک", "پست بانك", "postbank"),
            senderKeywords = listOf("postbank"),
            emblemType = BankEmblemType.POST_BANK
        ),
        IranianBank(
            code = "RESALAT",
            persianName = "بانک قرض‌الحسنه رسالت",
            shortName = "رسالت",
            primaryColorHex = 0xFF00695C,
            secondaryColorHex = 0xFF26A69A,
            accentColorHex = 0xFFB2DFDB,
            cardPrefixes = listOf("504172"),
            smsKeywords = listOf("بانک رسالت", "رسالت", "قرض الحسنه رسالت", "resalat"),
            senderKeywords = listOf("resalat", "rqbank"),
            emblemType = BankEmblemType.RESALAT
        ),
        IranianBank(
            code = "MEHR",
            persianName = "بانک قرض‌الحسنه مهر ایران",
            shortName = "مهر ایران",
            primaryColorHex = 0xFF1B5E20,
            secondaryColorHex = 0xFF2E7D32,
            accentColorHex = 0xFFFFCA28,
            cardPrefixes = listOf("606373"),
            smsKeywords = listOf("مهر ایران", "مهرایران", "مهر ايران", "qmb"),
            senderKeywords = listOf("qmb", "mehriran"),
            emblemType = BankEmblemType.MEHR_IRAN
        ),
        IranianBank(
            code = "KARAFARIN",
            persianName = "بانک کارآفرین",
            shortName = "کارآفرین",
            primaryColorHex = 0xFF004D40,
            secondaryColorHex = 0xFF00796B,
            accentColorHex = 0xFFFFAB40,
            cardPrefixes = listOf("627488", "502910"),
            smsKeywords = listOf("کارآفرین", "كارآفرين", "karafarin"),
            senderKeywords = listOf("karafarin"),
            emblemType = BankEmblemType.KARAFARIN
        ),
        IranianBank(
            code = "GARDESHGARI",
            persianName = "بانک گردشگری",
            shortName = "گردشگری",
            primaryColorHex = 0xFFB71C1C,
            secondaryColorHex = 0xFFC62828,
            accentColorHex = 0xFFFFD54F,
            cardPrefixes = listOf("505416"),
            smsKeywords = listOf("گردشگری", "گردشگري", "tourismbank"),
            senderKeywords = listOf("tourism", "gardeshgari"),
            emblemType = BankEmblemType.GARDESHGARI
        ),
        IranianBank(
            code = "CASH",
            persianName = "نقد / کیف پول",
            shortName = "کیف پول",
            primaryColorHex = 0xFF0B4F4A,
            secondaryColorHex = 0xFF10B981,
            accentColorHex = 0xFFF59E0B,
            cardPrefixes = emptyList(),
            smsKeywords = emptyList(),
            senderKeywords = emptyList(),
            emblemType = BankEmblemType.CASH
        )
    )

    val BANK_MAP: Map<String, IranianBank> = ALL.associateBy { it.code }

    fun findByCode(code: String?): IranianBank =
        BANK_MAP[code] ?: BANK_MAP["CASH"]!!

    /**
     * Detects the Iranian bank from a card number (supports 8-digit BluBank prefix first, then 6-digit BIN).
     */
    fun detectByCardNumber(rawCardNumber: String): IranianBank? {
        val digits = JalaliCalendar.toLatinDigits(rawCardNumber).filter { it.isDigit() }
        if (digits.length < 6) return null
        // Check longer prefixes first (e.g., BluBank 62198619 before Saman 621986)
        val sortedCandidates = ALL.flatMap { bank ->
            bank.cardPrefixes.map { prefix -> prefix to bank }
        }.sortedByDescending { it.first.length }
        return sortedCandidates.firstOrNull { (prefix, _) -> digits.startsWith(prefix) }?.second
    }

    fun detectBySms(sender: String, normalizedBody: String): IranianBank? {
        val lowerSender = sender.lowercase()
        val lowerBody = normalizedBody.lowercase()

        // 1. Check BluBank before Saman because BluBank is powered by Saman
        for (bank in ALL) {
            if (bank.code == "CASH") continue
            if (bank.smsKeywords.any { kw -> lowerBody.contains(kw.lowercase()) }) {
                return bank
            }
        }
        // 2. Check sender address match
        for (bank in ALL) {
            if (bank.code == "CASH") continue
            if (bank.senderKeywords.any { kw -> lowerSender.contains(kw.lowercase()) }) {
                return bank
            }
        }
        // 3. Check embedded 6-digit or 8-digit BIN prefix in card numbers
        val cardPrefixRegex = Regex("""\b(62198619|[569]\d{5})\d{0,10}\b""")
        for (match in cardPrefixRegex.findAll(lowerBody)) {
            val detected = detectByCardNumber(match.value)
            if (detected != null) return detected
        }
        return null
    }
}
