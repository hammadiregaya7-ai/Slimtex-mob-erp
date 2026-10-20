package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.ProductionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionDao {
    @Query("SELECT * FROM production_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<ProductionOrder>>

    @Query("SELECT * FROM production_orders WHERE id = :id")
    suspend fun getOrderById(id: Long): ProductionOrder?

    @Query("SELECT * FROM production_orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: ProductionStatus): Flow<List<ProductionOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: ProductionOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<ProductionOrder>)

    @Update
    suspend fun updateOrder(order: ProductionOrder)

    @Query("DELETE FROM production_orders WHERE id = :id")
    suspend fun deleteOrder(id: Long)

    // Materials / BOM
    @Query("SELECT * FROM production_material_items WHERE productionOrderId = :orderId")
    fun getMaterialsForOrder(orderId: Long): Flow<List<ProductionMaterialItem>>

    @Query("SELECT * FROM production_material_items WHERE productionOrderId = :orderId")
    suspend fun getMaterialsForOrderSync(orderId: Long): List<ProductionMaterialItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: ProductionMaterialItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMaterials(materials: List<ProductionMaterialItem>)

    @Query("DELETE FROM production_material_items WHERE id = :id")
    suspend fun deleteMaterial(id: Long)

    @Query("DELETE FROM production_material_items WHERE productionOrderId = :orderId")
    suspend fun deleteMaterialsForOrder(orderId: Long)
}
