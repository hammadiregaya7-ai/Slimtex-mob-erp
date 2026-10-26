package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RollQualityGrade(val displayName: String) {
    A_GRADE("A-Grade (First Quality)"),
    B_GRADE("B-Grade (Minor Shade/Slub)"),
    QUARANTINED("Quarantined (Hold)")
}

@Entity(
    tableName = "fabric_roll_lots",
    indices = [
        Index(value = ["rollNumber"], unique = true),
        Index(value = ["dyeLotCode"]),
        Index(value = ["itemId"])
    ]
)
data class FabricRollLot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val rollNumber: String,
    val dyeLotCode: String,
    val itemId: Long,
    val itemSku: String,
    val itemName: String,
    val supplierName: String,
    val shadeBand: String = "A1",
    val lengthMeters: Double,
    val gsm: Int = 180,
    val widthCm: Int = 150,
    val shrinkagePercent: Double = 1.5,
    val defectPointsPer100SqYds: Int = 8,
    val qualityGrade: RollQualityGrade = RollQualityGrade.A_GRADE,
    val batchUsedIn: String = "",
    val inspectorNotes: String = "",
    val inspectedAt: Long = System.currentTimeMillis()
) {
    val passesFourPointSystem: Boolean
        get() = defectPointsPer100SqYds <= 28 && qualityGrade != RollQualityGrade.QUARANTINED
}
