package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.BankEmblemType
import com.example.domain.IranianBank

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val isIncome: Boolean,
    val iconKey: String,
    val colorHex: Long,
    val monthlyBudgetToman: Long = 0L
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val relationNote: String = "",
    val avatarColorHex: Long = 0xFF0B4F4A,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bankCode: String,
    val cardNumber: String = "",
    val accountTitle: String = "",
    val balanceToman: Long = 0L,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["categoryId"]),
        Index(value = ["personId"]),
        Index(value = ["bankCode"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amountToman: Long,
    val isIncome: Boolean,
    val categoryId: Long?,
    val bankCode: String,
    val personId: Long? = null,
    /**
     * 0 = no debt/credit impact
     * +1 = I paid / lent to person -> person owes me (+receivable)
     * -1 = I received / borrowed from person -> I owe person (-payable / settles their debt)
     */
    val personImpact: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val fromSms: Boolean = false,
    val smsHash: String? = null
)

@Entity(
    tableName = "pending_sms",
    indices = [Index(value = ["smsHash"], unique = true)]
)
data class PendingSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val smsHash: String,
    val sender: String,
    val rawBody: String,
    val bankCode: String,
    val isIncome: Boolean,
    val amountToman: Long,
    val remainingBalanceToman: Long? = null,
    val cardHint: String? = null,
    val suggestedCategoryName: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, APPROVED, DISMISSED
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val targetAmountToman: Long,
    val savedAmountToman: Long = 0L,
    val colorHex: Long = 0xFF10B981,
    val iconKey: String = "savings",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_banks",
    indices = [Index(value = ["code"], unique = true)]
)
data class CustomBankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val code: String,
    val persianName: String,
    val shortName: String,
    val primaryColorHex: Long = 0xFF0B4F4A,
    val cardPrefix: String = "",
    val smsKeyword: String = ""
) {
    fun toIranianBank(): IranianBank {
        val prefixes = cardPrefix
            .split(",", " ", "،")
            .map { it.trim() }
            .filter { it.length >= 4 }
        val keywords = buildList {
            if (smsKeyword.isNotBlank()) {
                addAll(smsKeyword.split(",", "،").map { it.trim() }.filter { it.isNotBlank() })
            }
            if (persianName.isNotBlank()) add(persianName.trim())
            if (shortName.isNotBlank() && shortName.trim().length >= 3) add(shortName.trim())
        }.distinct()
        return IranianBank(
            code = code,
            persianName = persianName,
            shortName = shortName.ifBlank { persianName },
            primaryColorHex = primaryColorHex,
            secondaryColorHex = primaryColorHex,
            accentColorHex = 0xFFFFD54F,
            cardPrefixes = prefixes,
            smsKeywords = keywords,
            senderKeywords = keywords,
            emblemType = BankEmblemType.CUSTOM,
            isCustom = true
        )
    }
}
