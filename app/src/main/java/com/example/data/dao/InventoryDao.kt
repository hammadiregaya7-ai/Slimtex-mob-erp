package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CategoryStockSummary
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.StockThresholdAlert
import com.example.data.entity.StockTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE category = :category ORDER BY name ASC")
    fun getItemsByCategory(category: ItemCategory): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE autoAlertEnabled = 1 AND currentStock <= reorderLevel ORDER BY currentStock ASC")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE autoAlertEnabled = 1 AND (currentStock <= criticalThreshold OR currentStock <= 0) ORDER BY currentStock ASC")
    fun getCriticalStockItems(): Flow<List<InventoryItem>>

    @Query(
        """
        SELECT category,
               COUNT(*) AS totalSkus,
               COALESCE(SUM(currentStock), 0.0) AS totalUnitsInStock,
               COALESCE(SUM(currentStock * averageCost), 0.0) AS totalCategoryValuation,
               COALESCE(SUM(CASE WHEN autoAlertEnabled = 1 AND currentStock <= reorderLevel THEN 1 ELSE 0 END), 0) AS lowStockAlertCount
        FROM inventory_items
        GROUP BY category
        ORDER BY category ASC
        """
    )
    fun getCategoryStockSummaries(): Flow<List<CategoryStockSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("UPDATE inventory_items SET currentStock = currentStock + :delta WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Double)

    @Query(
        """
        UPDATE inventory_items
        SET reorderLevel = :reorderLevel,
            criticalThreshold = :criticalThreshold,
            autoAlertEnabled = :autoAlertEnabled,
            leadTimeDays = :leadTimeDays
        WHERE id = :id
        """
    )
    suspend fun updateThresholdConfig(
        id: Long,
        reorderLevel: Double,
        criticalThreshold: Double,
        autoAlertEnabled: Boolean,
        leadTimeDays: Int
    )

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    // --- Low-Stock Threshold Alerts ---
    @Query("SELECT * FROM stock_threshold_alerts WHERE isAcknowledged = 0 ORDER BY createdAt DESC")
    fun getActiveThresholdAlerts(): Flow<List<StockThresholdAlert>>

    @Query("SELECT * FROM stock_threshold_alerts ORDER BY createdAt DESC")
    fun getAllThresholdAlerts(): Flow<List<StockThresholdAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThresholdAlert(alert: StockThresholdAlert): Long

    @Query("UPDATE stock_threshold_alerts SET isAcknowledged = 1 WHERE id = :alertId")
    suspend fun acknowledgeThresholdAlert(alertId: Long)

    @Query("DELETE FROM stock_threshold_alerts WHERE itemId = :itemId")
    suspend fun deleteAlertsForItem(itemId: Long)

    // --- Stock Transactions ---
    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<StockTransaction>>

    @Query("SELECT * FROM stock_transactions WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getTransactionsForItem(itemId: Long): Flow<List<StockTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<StockTransaction>)
}
