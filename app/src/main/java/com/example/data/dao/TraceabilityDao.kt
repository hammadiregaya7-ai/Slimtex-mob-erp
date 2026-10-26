package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.FabricRollLot
import com.example.data.entity.RollQualityGrade
import kotlinx.coroutines.flow.Flow

@Dao
interface TraceabilityDao {
    @Query("SELECT * FROM fabric_roll_lots ORDER BY inspectedAt DESC")
    fun getAllRollLots(): Flow<List<FabricRollLot>>

    @Query("SELECT * FROM fabric_roll_lots WHERE itemId = :itemId ORDER BY inspectedAt DESC")
    fun getRollLotsForItem(itemId: Long): Flow<List<FabricRollLot>>

    @Query("SELECT * FROM fabric_roll_lots WHERE id = :id")
    suspend fun getRollLotById(id: Long): FabricRollLot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRollLot(rollLot: FabricRollLot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRollLots(rollLots: List<FabricRollLot>)

    @Update
    suspend fun updateRollLot(rollLot: FabricRollLot)

    @Query(
        """
        UPDATE fabric_roll_lots
        SET defectPointsPer100SqYds = :defectPoints,
            qualityGrade = :grade,
            shadeBand = :shadeBand,
            inspectorNotes = :notes
        WHERE id = :rollId
        """
    )
    suspend fun updateRollInspection(
        rollId: Long,
        defectPoints: Int,
        grade: RollQualityGrade,
        shadeBand: String,
        notes: String
    )

    @Query("DELETE FROM fabric_roll_lots WHERE id = :id")
    suspend fun deleteRollLot(id: Long)
}
