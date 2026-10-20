package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import kotlinx.coroutines.flow.Flow

@Dao
interface CommercialDao {
    @Query("SELECT * FROM commercial_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<CommercialOrder>>

    @Query("SELECT * FROM commercial_orders WHERE type = :type ORDER BY timestamp DESC")
    fun getOrdersByType(type: CommercialType): Flow<List<CommercialOrder>>

    @Query("SELECT * FROM commercial_orders WHERE id = :id")
    suspend fun getOrderById(id: Long): CommercialOrder?

    @Query("SELECT * FROM commercial_orders WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getOrdersForCustomer(customerId: Long): Flow<List<CommercialOrder>>

    @Query("SELECT * FROM commercial_orders WHERE partyName = :partyName ORDER BY timestamp DESC")
    fun getOrdersForParty(partyName: String): Flow<List<CommercialOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: CommercialOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<CommercialOrder>)

    @Update
    suspend fun updateOrder(order: CommercialOrder)

    @Query("UPDATE commercial_orders SET paidAmount = paidAmount + :paymentDelta, status = CASE WHEN (paidAmount + :paymentDelta) >= totalAmount THEN 'PAID' ELSE status END WHERE id = :orderId")
    suspend fun recordPayment(orderId: Long, paymentDelta: Double)

    @Query("DELETE FROM commercial_orders WHERE id = :id")
    suspend fun deleteOrder(id: Long)

    // Items
    @Query("SELECT * FROM commercial_order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: Long): Flow<List<CommercialOrderItem>>

    @Query("SELECT * FROM commercial_order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: Long): List<CommercialOrderItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: CommercialOrderItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrderItems(items: List<CommercialOrderItem>)

    @Transaction
    suspend fun createOrderWithItems(order: CommercialOrder, items: List<CommercialOrderItem>): Long {
        val total = items.sumOf { it.lineTotal }
        val orderId = insertOrder(order.copy(totalAmount = total))
        val populatedItems = items.map { it.copy(orderId = orderId) }
        insertAllOrderItems(populatedItems)
        return orderId
    }
}
