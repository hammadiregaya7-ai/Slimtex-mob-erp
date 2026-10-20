package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val entryNumber: String, // e.g. JE-1001
    val timestamp: Long = System.currentTimeMillis(),
    val memo: String,
    val sourceReference: String = "", // PO-101, BAT-101, INV-201
    val totalAmount: Double = 0.0
)

@Entity(tableName = "journal_lines")
data class JournalLine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val journalEntryId: Long,
    val accountCode: String,
    val accountName: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0
)
