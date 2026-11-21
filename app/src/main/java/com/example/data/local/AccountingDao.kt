package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY isIncome ASC, id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isIncome ASC, id ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // --- People ---
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun observePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getPeopleOnce(): List<PersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM people")
    suspend fun clearPeople()

    // --- Bank Accounts ---
    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, id ASC")
    fun observeBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, id ASC")
    suspend fun getBankAccountsOnce(): List<BankAccountEntity>

    @Query("SELECT * FROM bank_accounts WHERE bankCode = :bankCode LIMIT 1")
    suspend fun findBankAccountByCode(bankCode: String): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccounts(accounts: List<BankAccountEntity>)

    @Update
    suspend fun updateBankAccount(account: BankAccountEntity)

    @Delete
    suspend fun deleteBankAccount(account: BankAccountEntity)

    @Query("DELETE FROM bank_accounts")
    suspend fun clearBankAccounts()

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC, id DESC")
    suspend fun getTransactionsOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsHash = :hash)")
    suspend fun hasTransactionWithSmsHash(hash: String): Boolean

    // --- Pending SMS ---
    @Query("SELECT * FROM pending_sms WHERE status = 'PENDING' ORDER BY timestampMillis DESC")
    fun observePendingSms(): Flow<List<PendingSmsEntity>>

    @Query("SELECT * FROM pending_sms WHERE id = :id LIMIT 1")
    suspend fun getPendingSmsById(id: Long): PendingSmsEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingSms(sms: PendingSmsEntity): Long

    @Query("UPDATE pending_sms SET status = :status WHERE id = :id")
    suspend fun updatePendingSmsStatus(id: Long, status: String)

    @Query("DELETE FROM pending_sms WHERE status != 'PENDING'")
    suspend fun pruneHandledSms()

    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY createdAtMillis DESC")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY createdAtMillis DESC")
    suspend fun getSavingsGoalsOnce(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals")
    suspend fun clearSavingsGoals()

    // --- Custom Banks ---
    @Query("SELECT * FROM custom_banks ORDER BY id ASC")
    fun observeCustomBanks(): Flow<List<CustomBankEntity>>

    @Query("SELECT * FROM custom_banks ORDER BY id ASC")
    suspend fun getCustomBanksOnce(): List<CustomBankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomBank(bank: CustomBankEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomBanks(banks: List<CustomBankEntity>)

    @Update
    suspend fun updateCustomBank(bank: CustomBankEntity)

    @Delete
    suspend fun deleteCustomBank(bank: CustomBankEntity)

    @Query("DELETE FROM custom_banks")
    suspend fun clearCustomBanks()

    @Transaction
    suspend fun replaceAllData(
        categories: List<CategoryEntity>,
        people: List<PersonEntity>,
        bankAccounts: List<BankAccountEntity>,
        transactions: List<TransactionEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        customBanks: List<CustomBankEntity> = emptyList()
    ) {
        clearTransactions()
        clearCategories()
        clearPeople()
        clearBankAccounts()
        clearSavingsGoals()
        clearCustomBanks()
        if (categories.isNotEmpty()) insertCategories(categories)
        if (people.isNotEmpty()) insertPeople(people)
        if (bankAccounts.isNotEmpty()) insertBankAccounts(bankAccounts)
        if (transactions.isNotEmpty()) insertTransactions(transactions)
        if (savingsGoals.isNotEmpty()) insertSavingsGoals(savingsGoals)
        if (customBanks.isNotEmpty()) insertCustomBanks(customBanks)
    }
}
