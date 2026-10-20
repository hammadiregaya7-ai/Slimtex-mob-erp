package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.Account
import com.example.data.entity.AccountType
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import kotlinx.coroutines.flow.Flow

data class JournalEntryWithLines(
    val entry: JournalEntry,
    val lines: List<JournalLine>
)

@Dao
interface AccountingDao {
    @Query("SELECT * FROM chart_of_accounts ORDER BY accountCode ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM chart_of_accounts WHERE accountCode = :code")
    suspend fun getAccountByCode(code: String): Account?

    @Query("SELECT * FROM chart_of_accounts WHERE type = :type ORDER BY accountCode ASC")
    fun getAccountsByType(type: AccountType): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccounts(accounts: List<Account>)

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE chart_of_accounts SET currentBalance = currentBalance + :delta WHERE accountCode = :code")
    suspend fun adjustAccountBalance(code: String, delta: Double)

    // Journal entries
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId")
    suspend fun getLinesForEntry(entryId: Long): List<JournalLine>

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId")
    fun getLinesForEntryFlow(entryId: Long): Flow<List<JournalLine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLine>)

    @Transaction
    suspend fun recordBalancedEntry(entry: JournalEntry, lines: List<JournalLine>): Long {
        val entryId = insertJournalEntry(entry)
        val populatedLines = lines.map { it.copy(journalEntryId = entryId) }
        insertJournalLines(populatedLines)

        // Update respective account balances according to NormalBalance rule
        for (line in populatedLines) {
            val account = getAccountByCode(line.accountCode)
            if (account != null) {
                // Asset & Expense normal balance is DEBIT: Debit increases balance, Credit decreases
                // Liability, Equity & Revenue normal balance is CREDIT: Credit increases balance, Debit decreases
                val delta = if (account.normalBalance == com.example.data.entity.NormalBalance.DEBIT) {
                    line.debit - line.credit
                } else {
                    line.credit - line.debit
                }
                adjustAccountBalance(line.accountCode, delta)
            }
        }
        return entryId
    }
}
