package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CustomerAccountType {
    WHOLESALE,
    RETAIL_BOUTIQUE,
    CORPORATE,
    EXPORT
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val code: String, // e.g. CUST-101
    val name: String, // e.g. Nordic Apparel Wholesalers
    val accountType: CustomerAccountType = CustomerAccountType.WHOLESALE,
    val contactPerson: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 25000.0,
    val paymentTerms: String = "Net 30", // e.g. Net 15, Net 30, Net 60, Advance
    val currentBalance: Double = 0.0, // Outstanding Accounts Receivable
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
