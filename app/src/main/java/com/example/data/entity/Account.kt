package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE
}

enum class NormalBalance {
    DEBIT,
    CREDIT
}

@Entity(tableName = "chart_of_accounts")
data class Account(
    @PrimaryKey
    val accountCode: String, // e.g. "1010", "1200", "2000", "4000", "5000"
    val accountName: String,
    val type: AccountType,
    val normalBalance: NormalBalance,
    val currentBalance: Double = 0.0,
    val description: String = ""
)
