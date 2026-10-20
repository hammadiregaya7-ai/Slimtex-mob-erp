package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    PURCHASE_RECEIPT,
    PRODUCTION_CONSUMPTION,
    PRODUCTION_OUTPUT,
    SALES_DISPATCH,
    ADJUSTMENT
}

@Entity(tableName = "stock_transactions")
data class StockTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val itemId: Long,
    val itemName: String,
    val type: TransactionType,
    val quantityDelta: Double, // Positive for stock in, negative for stock out
    val unitCost: Double,
    val totalCost: Double,
    val referenceNumber: String, // e.g. PO-101, BAT-2026-101, INV-201
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
