package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommercialType {
    PURCHASE_ORDER, // From textile mills / suppliers
    COMMERCIAL_INVOICE // To buyers / retail clients
}

enum class CommercialStatus {
    DRAFT,
    CONFIRMED,
    FULFILLED,
    PAID,
    CANCELLED
}

@Entity(tableName = "commercial_orders")
data class CommercialOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val orderNumber: String, // PO-2026-001 or INV-2026-001
    val type: CommercialType,
    val partyName: String, // Supplier or Customer
    val customerId: Long = 0L, // Link to Customer entity if applicable
    val status: CommercialStatus = CommercialStatus.CONFIRMED,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "commercial_order_items")
data class CommercialOrderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val orderId: Long,
    val itemId: Long,
    val itemName: String,
    val unit: UnitOfMeasure,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double = quantity * unitPrice
)
