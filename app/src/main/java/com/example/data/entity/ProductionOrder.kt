package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProductionStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "production_orders")
data class ProductionOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val batchNumber: String, // e.g. BAT-2026-101
    val finishedItemId: Long,
    val finishedItemName: String,
    val targetQuantity: Double, // Target pieces to manufacture
    val completedQuantity: Double = 0.0,
    val status: ProductionStatus = ProductionStatus.PLANNED,
    val directLaborCost: Double = 0.0, // Cutting & Tailoring labor
    val overheadCost: Double = 0.0, // Machinery power, factory overhead, packaging
    val rawMaterialCost: Double = 0.0, // Aggregated from materials
    val totalBatchCost: Double = 0.0, // Material + Labor + Overhead
    val unitProductionCost: Double = 0.0, // Total / TargetQuantity
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
