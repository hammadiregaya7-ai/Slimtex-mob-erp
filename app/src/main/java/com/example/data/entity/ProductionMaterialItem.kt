package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "production_material_items")
data class ProductionMaterialItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productionOrderId: Long,
    val itemId: Long,
    val itemName: String,
    val unit: UnitOfMeasure,
    val quantityRequired: Double,
    val unitCost: Double,
    val totalCost: Double = quantityRequired * unitCost
)
