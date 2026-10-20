package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemCategory {
    RAW_MATERIAL,
    FINISHED_GOOD,
    TRIM,
    DYE_CHEMICAL
}

enum class UnitOfMeasure {
    METERS,
    KG,
    PIECES,
    ROLLS,
    YARDS,
    BOXES
}

@Entity(tableName = "inventory_items")
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
    val description: String = ""
)
