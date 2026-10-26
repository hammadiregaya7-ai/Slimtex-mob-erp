package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommercialType(val displayName: String) {
    SALES_QUOTATION("Sales Quotation"),
    PURCHASE_ORDER("Purchase Order"),
    COMMERCIAL_INVOICE("Commercial Invoice")
}

enum class CommercialStatus {
    DRAFT,
    PENDING_APPROVAL,
    CONFIRMED,
    FULFILLED,
    PAID,
    CANCELLED
}

enum class ErpCurrency(
    val code: String,
    val symbol: String,
    val rateToUsd: Double,
    val label: String
) {
    USD("USD", "$", 1.0, "US Dollar ($)"),
    EUR("EUR", "€", 1.09, "Euro (€)"),
    JPY("JPY", "¥", 0.0067, "Japanese Yen (¥)"),
    GBP("GBP", "£", 1.27, "British Pound (£)"),
    DKK("DKK", "kr", 0.146, "Danish Krone (kr)");

    companion object {
        fun fromCode(code: String): ErpCurrency =
            values().find { it.code.equals(code, ignoreCase = true) } ?: USD
    }
}

enum class ErpRole(
    val displayName: String,
    val badgeCode: String,
    val canApproveHighValuePo: Boolean,
    val canOverrideCreditLimit: Boolean
) {
    EXECUTIVE_CONTROLLER("Executive Controller", "CTRL", true, true),
    PRODUCTION_MANAGER("Production Manager", "PROD", false, false),
    WAREHOUSE_LEAD("Warehouse Lead", "WHSE", false, false)
}

@Entity(tableName = "commercial_orders")
data class CommercialOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val orderNumber: String, // QT-2026-001, PO-2026-001, or INV-2026-001
    val type: CommercialType,
    val partyName: String, // Supplier or Customer
    val customerId: Long = 0L, // Link to Customer entity if applicable
    val supplierId: Long = 0L, // Link to Supplier entity if applicable
    val status: CommercialStatus = CommercialStatus.CONFIRMED,
    val totalAmount: Double = 0.0, // Stored in USD base ledger value
    val paidAmount: Double = 0.0,
    val currencyCode: String = "USD",
    val exchangeRateToUsd: Double = 1.0,
    val incoterms: String = "FOB",
    val parentDocumentRef: String = "", // Document Chaining parent (e.g., QT-2026-012 or BAT-2026-101)
    val linkedBatchNumber: String = "", // Chained Production Work Order batch
    val requiresApproval: Boolean = false,
    val approvedByRole: String = "",
    val packingListNumber: String = "",
    val totalCartons: Int = 0,
    val netWeightKg: Double = 0.0,
    val grossWeightKg: Double = 0.0,
    val hsTariffCode: String = "6205.20.00",
    val portOfLoading: String = "Rotterdam / Factory Gate",
    val portOfDischarge: String = "Buyer Warehouse",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val foreignCurrencyTotal: Double
        get() = if (exchangeRateToUsd > 0.0) totalAmount / exchangeRateToUsd else totalAmount
}

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
