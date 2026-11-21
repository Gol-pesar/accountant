package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.AccountingDao
import com.example.data.local.BankAccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.PersonEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupStats(
    val transactionCount: Int,
    val categoryCount: Int,
    val personCount: Int,
    val bankAccountCount: Int,
    val savingsGoalCount: Int
)

/**
 * Exports and restores complete app accounting data as JSON via Android's Storage Access Framework
 * (supporting explicit user-consented Google Drive or local file selection with zero storage permissions).
 */
class DriveBackupManager(
    private val context: Context,
    private val dao: AccountingDao
) {
    suspend fun exportBackupToUri(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = dao.getCategoriesOnce()
            val people = dao.getPeopleOnce()
            val accounts = dao.getBankAccountsOnce()
            val transactions = dao.getTransactionsOnce()
            val goals = dao.getSavingsGoalsOnce()

            val root = JSONObject().apply {
                put("app", "HesabdarMan")
                put("version", 1)
                put("exportedAtMillis", System.currentTimeMillis())

                put("categories", JSONArray().apply {
                    categories.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("isIncome", c.isIncome)
                            put("iconKey", c.iconKey)
                            put("colorHex", c.colorHex)
                            put("monthlyBudgetToman", c.monthlyBudgetToman)
                        })
                    }
                })

                put("people", JSONArray().apply {
                    people.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("phone", p.phone)
                            put("relationNote", p.relationNote)
                            put("avatarColorHex", p.avatarColorHex)
                            put("createdAtMillis", p.createdAtMillis)
                        })
                    }
                })

                put("bankAccounts", JSONArray().apply {
                    accounts.forEach { a ->
                        put(JSONObject().apply {
                            put("id", a.id)
                            put("bankCode", a.bankCode)
                            put("cardNumber", a.cardNumber)
                            put("accountTitle", a.accountTitle)
                            put("balanceToman", a.balanceToman)
                            put("isDefault", a.isDefault)
                        })
                    }
                })

                put("transactions", JSONArray().apply {
                    transactions.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("title", t.title)
                            put("amountToman", t.amountToman)
                            put("isIncome", t.isIncome)
                            if (t.categoryId != null) put("categoryId", t.categoryId)
                            put("bankCode", t.bankCode)
                            if (t.personId != null) put("personId", t.personId)
                            put("personImpact", t.personImpact)
                            put("timestampMillis", t.timestampMillis)
                            put("note", t.note)
                            put("fromSms", t.fromSms)
                            if (t.smsHash != null) put("smsHash", t.smsHash)
                        })
                    }
                })

                put("savingsGoals", JSONArray().apply {
                    goals.forEach { g ->
                        put(JSONObject().apply {
                            put("id", g.id)
                            put("title", g.title)
                            put("targetAmountToman", g.targetAmountToman)
                            put("savedAmountToman", g.savedAmountToman)
                            put("colorHex", g.colorHex)
                            put("iconKey", g.iconKey)
                            put("createdAtMillis", g.createdAtMillis)
                        })
                    }
                })
            }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(root.toString(2).toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("امکان نوشتن در فایل انتخابی وجود نداشت")

            BackupStats(
                transactionCount = transactions.size,
                categoryCount = categories.size,
                personCount = people.size,
                bankAccountCount = accounts.size,
                savingsGoalCount = goals.size
            )
        }
    }

    suspend fun restoreBackupFromUri(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("امکان خواندن فایل پشتیبان وجود نداشت")

            val root = JSONObject(content)
            require(root.optString("app") == "HesabdarMan") {
                "فایل انتخابی مربوط به نسخه پشتیبان «حسابدار من» نیست"
            }

            val categories = parseArray(root.optJSONArray("categories")) { o ->
                CategoryEntity(
                    id = o.optLong("id", 0L),
                    name = o.getString("name"),
                    isIncome = o.optBoolean("isIncome", false),
                    iconKey = o.optString("iconKey", "shopping_bag"),
                    colorHex = o.optLong("colorHex", 0xFF0B4F4A),
                    monthlyBudgetToman = o.optLong("monthlyBudgetToman", 0L)
                )
            }

            val people = parseArray(root.optJSONArray("people")) { o ->
                PersonEntity(
                    id = o.optLong("id", 0L),
                    name = o.getString("name"),
                    phone = o.optString("phone", ""),
                    relationNote = o.optString("relationNote", ""),
                    avatarColorHex = o.optLong("avatarColorHex", 0xFF0B4F4A),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }

            val accounts = parseArray(root.optJSONArray("bankAccounts")) { o ->
                BankAccountEntity(
                    id = o.optLong("id", 0L),
                    bankCode = o.optString("bankCode", "CASH"),
                    cardNumber = o.optString("cardNumber", ""),
                    accountTitle = o.optString("accountTitle", ""),
                    balanceToman = o.optLong("balanceToman", 0L),
                    isDefault = o.optBoolean("isDefault", false)
                )
            }

            val transactions = parseArray(root.optJSONArray("transactions")) { o ->
                TransactionEntity(
                    id = o.optLong("id", 0L),
                    title = o.getString("title"),
                    amountToman = o.getLong("amountToman"),
                    isIncome = o.optBoolean("isIncome", false),
                    categoryId = if (o.has("categoryId")) o.optLong("categoryId") else null,
                    bankCode = o.optString("bankCode", "CASH"),
                    personId = if (o.has("personId")) o.optLong("personId") else null,
                    personImpact = o.optInt("personImpact", 0),
                    timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()),
                    note = o.optString("note", ""),
                    fromSms = o.optBoolean("fromSms", false),
                    smsHash = if (o.has("smsHash")) o.optString("smsHash") else null
                )
            }

            val goals = parseArray(root.optJSONArray("savingsGoals")) { o ->
                SavingsGoalEntity(
                    id = o.optLong("id", 0L),
                    title = o.getString("title"),
                    targetAmountToman = o.getLong("targetAmountToman"),
                    savedAmountToman = o.optLong("savedAmountToman", 0L),
                    colorHex = o.optLong("colorHex", 0xFF10B981),
                    iconKey = o.optString("iconKey", "savings"),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                )
            }

            dao.replaceAllData(
                categories = categories,
                people = people,
                bankAccounts = accounts,
                transactions = transactions,
                savingsGoals = goals
            )

            BackupStats(
                transactionCount = transactions.size,
                categoryCount = categories.size,
                personCount = people.size,
                bankAccountCount = accounts.size,
                savingsGoalCount = goals.size
            )
        }
    }

    private inline fun <T> parseArray(array: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        val result = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(mapper(obj))
        }
        return result
    }
}
