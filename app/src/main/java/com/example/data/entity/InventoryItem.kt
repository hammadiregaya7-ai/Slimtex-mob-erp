package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ItemCategory(
    val displayName: String,
    val defaultReorderThreshold: Double,
    val categoryDescription: String
) {
    RAW_MATERIAL("Raw Materials", 250.0, "Base fabrics, yarns, and greige goods"),
    FINISHED_GOOD("Finished Goods", 50.0, "Completed garments ready for commercial dispatch"),
    TRIM("Trims & Notions", 500.0, "Buttons, zippers, labels, and interlinings"),
    DYE_CHEMICAL("Dyes & Chemicals", 100.0, "Reactive dyes, softeners, and finishing agents")
}

enum class UnitOfMeasure {
    METERS,
    KG,
    PIECES,
    ROLLS,
    YARDS,
    BOXES
}

enum class StockAlertSeverity {
    CRITICAL,
    LOW_STOCK,
    HEALTHY
}

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["category"])
    ]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sku: String,
    val name: String,
    val category: ItemCategory,
    val unit: UnitOfMeasure,
    val currentStock: Double,
    val reorderLevel: Double,
    val averageCost: Double, // Cost per unit
    val sellingPrice: Double = 0.0, // Used for finished goods or trading resale
    val storageLocation: String = "Main Warehouse",
    val description: String = "",
    val criticalThreshold: Double = (reorderLevel * 0.4).coerceAtLeast(1.0),
    val autoAlertEnabled: Boolean = true,
    val preferredSupplier: String = "Primary Textile Mill",
    val leadTimeDays: Int = 7
) {
    val isLowStock: Boolean
        get() = autoAlertEnabled && currentStock <= reorderLevel

    val isCriticalStock: Boolean
        get() = autoAlertEnabled && (currentStock <= criticalThreshold || currentStock <= 0.0)

    val alertSeverity: StockAlertSeverity
        get() = when {
            !autoAlertEnabled -> StockAlertSeverity.HEALTHY
            currentStock <= criticalThreshold || currentStock <= 0.0 -> StockAlertSeverity.CRITICAL
            currentStock <= reorderLevel -> StockAlertSeverity.LOW_STOCK
            else -> StockAlertSeverity.HEALTHY
        }

    val stockDeficit: Double
        get() = (reorderLevel - currentStock).coerceAtLeast(0.0)

    val recommendedRestockQty: Double
        get() = ((reorderLevel * 2.0) - currentStock).coerceAtLeast(reorderLevel)
}

@Entity(
    tableName = "stock_threshold_alerts",
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["isAcknowledged"])
    ]
)
data class StockThresholdAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val itemId: Long,
    val sku: String,
    val itemName: String,
    val category: ItemCategory,
    val unit: UnitOfMeasure,
    val currentStock: Double,
    val thresholdLevel: Double,
    val criticalLevel: Double,
    val severity: StockAlertSeverity,
    val deficitQuantity: Double,
    val recommendedOrderQuantity: Double,
    val isAcknowledged: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CategoryStockSummary(
    val category: ItemCategory,
    val totalSkus: Int,
    val totalUnitsInStock: Double,
    val totalCategoryValuation: Double,
    val lowStockAlertCount: Int
)
