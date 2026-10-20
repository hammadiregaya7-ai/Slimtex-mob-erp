package com.example.data

import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import com.example.data.entity.InventoryItem
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.ProductionStatus
import com.example.data.entity.StockTransaction
import com.example.data.entity.TransactionType
import com.example.data.entity.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ErpRepository(private val db: AppDatabase) {
    private val inventoryDao = db.inventoryDao()
    private val productionDao = db.productionDao()
    private val accountingDao = db.accountingDao()
    private val commercialDao = db.commercialDao()
    private val customerDao = db.customerDao()

    // --- Customer / Client Streams & Actions ---
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(id: Long) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(id)
    }

    suspend fun getCustomerById(id: Long): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    fun getCustomerOrders(customerId: Long): Flow<List<CommercialOrder>> {
        return commercialDao.getOrdersForCustomer(customerId)
    }

    suspend fun recordCustomerDirectPayment(customerId: Long, amount: Double, memo: String) = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId) ?: return@withContext
        customerDao.adjustCustomerBalance(customerId, -amount)

        // Post Journal Entry: Debit Operating Bank Account (1020), Credit Accounts Receivable (1100)
        accountingDao.recordBalancedEntry(
            entry = JournalEntry(
                entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                memo = "Direct payment from ${customer.name}: $memo",
                sourceReference = customer.code,
                totalAmount = amount
            ),
            lines = listOf(
                JournalLine(0L, 0L, "1020", "Operating Bank Account", debit = amount, credit = 0.0),
                JournalLine(0L, 0L, "1100", "Accounts Receivable (AR)", debit = 0.0, credit = amount)
            )
        )
    }

    // --- Inventory Streams & Actions ---
    val allItems: Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    val lowStockItems: Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()
    val stockTransactions: Flow<List<StockTransaction>> = inventoryDao.getAllTransactions()

    suspend fun saveItem(item: InventoryItem): Long = withContext(Dispatchers.IO) {
        if (item.id == 0L) {
            inventoryDao.insertItem(item)
        } else {
            inventoryDao.updateItem(item)
            item.id
        }
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        inventoryDao.deleteItem(id)
    }

    suspend fun recordStockMovement(
        itemId: Long,
        delta: Double,
        unitCost: Double,
        type: TransactionType,
        refNumber: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val item = inventoryDao.getItemById(itemId) ?: return@withContext
        val totalCost = kotlin.math.abs(delta) * unitCost

        // Adjust inventory
        inventoryDao.adjustStock(itemId, delta)

        // Log transaction
        inventoryDao.insertTransaction(
            StockTransaction(
                itemId = itemId,
                itemName = item.name,
                type = type,
                quantityDelta = delta,
                unitCost = unitCost,
                totalCost = totalCost,
                referenceNumber = refNumber,
                notes = notes
            )
        )

        // If purchase receipt, post double entry (Debit Raw Materials, Credit Accounts Payable or Cash)
        if (type == TransactionType.PURCHASE_RECEIPT && totalCost > 0) {
            accountingDao.recordBalancedEntry(
                entry = JournalEntry(
                    entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                    memo = "Stock In: ${item.name} ($refNumber)",
                    sourceReference = refNumber,
                    totalAmount = totalCost
                ),
                lines = listOf(
                    JournalLine(0L, 0L, "1200", "Inventory - Raw Materials", debit = totalCost, credit = 0.0),
                    JournalLine(0L, 0L, "2000", "Accounts Payable (AP)", debit = 0.0, credit = totalCost)
                )
            )
        }
    }

    // --- Production Streams & Actions ---
    val allProductionOrders: Flow<List<ProductionOrder>> = productionDao.getAllOrders()

    fun getMaterialsForOrder(orderId: Long): Flow<List<ProductionMaterialItem>> {
        return productionDao.getMaterialsForOrder(orderId)
    }

    suspend fun createProductionBatch(
        batchNumber: String,
        finishedItemId: Long,
        finishedItemName: String,
        targetQuantity: Double,
        directLaborCost: Double,
        overheadCost: Double,
        notes: String,
        materials: List<ProductionMaterialItem>
    ): Long = withContext(Dispatchers.IO) {
        val materialTotal = materials.sumOf { it.totalCost }
        val totalBatchCost = materialTotal + directLaborCost + overheadCost
        val unitCost = if (targetQuantity > 0) totalBatchCost / targetQuantity else 0.0

        val order = ProductionOrder(
            batchNumber = batchNumber,
            finishedItemId = finishedItemId,
            finishedItemName = finishedItemName,
            targetQuantity = targetQuantity,
            directLaborCost = directLaborCost,
            overheadCost = overheadCost,
            rawMaterialCost = materialTotal,
            totalBatchCost = totalBatchCost,
            unitProductionCost = unitCost,
            notes = notes,
            status = ProductionStatus.IN_PROGRESS
        )
        val orderId = productionDao.insertOrder(order)
        val populatedMaterials = materials.map { it.copy(productionOrderId = orderId) }
        productionDao.insertAllMaterials(populatedMaterials)
        orderId
    }

    suspend fun updateProductionCosts(
        orderId: Long,
        directLaborCost: Double,
        overheadCost: Double
    ) = withContext(Dispatchers.IO) {
        val order = productionDao.getOrderById(orderId) ?: return@withContext
        val materials = productionDao.getMaterialsForOrderSync(orderId)
        val materialTotal = materials.sumOf { it.totalCost }
        val totalBatchCost = materialTotal + directLaborCost + overheadCost
        val unitCost = if (order.targetQuantity > 0) totalBatchCost / order.targetQuantity else 0.0

        productionDao.updateOrder(
            order.copy(
                directLaborCost = directLaborCost,
                overheadCost = overheadCost,
                rawMaterialCost = materialTotal,
                totalBatchCost = totalBatchCost,
                unitProductionCost = unitCost
            )
        )
    }

    suspend fun addMaterialToBatch(material: ProductionMaterialItem) = withContext(Dispatchers.IO) {
        productionDao.insertMaterial(material)
        // Recalculate order total
        val order = productionDao.getOrderById(material.productionOrderId) ?: return@withContext
        val materials = productionDao.getMaterialsForOrderSync(material.productionOrderId)
        val materialTotal = materials.sumOf { it.totalCost }
        val totalBatchCost = materialTotal + order.directLaborCost + order.overheadCost
        val unitCost = if (order.targetQuantity > 0) totalBatchCost / order.targetQuantity else 0.0
        productionDao.updateOrder(
            order.copy(
                rawMaterialCost = materialTotal,
                totalBatchCost = totalBatchCost,
                unitProductionCost = unitCost
            )
        )
    }

    suspend fun completeProductionBatch(orderId: Long) = withContext(Dispatchers.IO) {
        val order = productionDao.getOrderById(orderId) ?: return@withContext
        if (order.status == ProductionStatus.COMPLETED) return@withContext

        val materials = productionDao.getMaterialsForOrderSync(orderId)
        val finishedItem = inventoryDao.getItemById(order.finishedItemId)

        // 1. Consume raw materials
        for (mat in materials) {
            inventoryDao.adjustStock(mat.itemId, -mat.quantityRequired)
            inventoryDao.insertTransaction(
                StockTransaction(
                    itemId = mat.itemId,
                    itemName = mat.itemName,
                    type = TransactionType.PRODUCTION_CONSUMPTION,
                    quantityDelta = -mat.quantityRequired,
                    unitCost = mat.unitCost,
                    totalCost = mat.totalCost,
                    referenceNumber = order.batchNumber,
                    notes = "Consumed for Batch ${order.batchNumber}"
                )
            )
        }

        // 2. Output finished goods
        if (finishedItem != null) {
            inventoryDao.adjustStock(finishedItem.id, order.targetQuantity)
            // Update average unit cost if applicable
            val newAvgCost = if (finishedItem.currentStock + order.targetQuantity > 0) {
                ((finishedItem.currentStock * finishedItem.averageCost) + order.totalBatchCost) /
                        (finishedItem.currentStock + order.targetQuantity)
            } else {
                order.unitProductionCost
            }
            inventoryDao.updateItem(finishedItem.copy(averageCost = newAvgCost))

            inventoryDao.insertTransaction(
                StockTransaction(
                    itemId = finishedItem.id,
                    itemName = finishedItem.name,
                    type = TransactionType.PRODUCTION_OUTPUT,
                    quantityDelta = order.targetQuantity,
                    unitCost = order.unitProductionCost,
                    totalCost = order.totalBatchCost,
                    referenceNumber = order.batchNumber,
                    notes = "Finished output from Batch ${order.batchNumber}"
                )
            )
        }

        // 3. Post Double-Entry Journal Entry
        // Debit: Finished Goods (1400) = Total Batch Cost
        // Credit: Raw Materials (1200) = Raw Materials Subtotal
        // Credit: Direct Labor Applied (5100) = Labor Cost
        // Credit: Factory Overhead Applied (5200) = Overhead Cost
        val lines = mutableListOf<JournalLine>()
        lines.add(
            JournalLine(0L, 0L, "1400", "Inventory - Finished Goods", debit = order.totalBatchCost, credit = 0.0)
        )
        if (order.rawMaterialCost > 0) {
            lines.add(
                JournalLine(0L, 0L, "1200", "Inventory - Raw Materials", debit = 0.0, credit = order.rawMaterialCost)
            )
        }
        if (order.directLaborCost > 0) {
            lines.add(
                JournalLine(0L, 0L, "5100", "Direct Tailoring Labor", debit = 0.0, credit = order.directLaborCost)
            )
        }
        if (order.overheadCost > 0) {
            lines.add(
                JournalLine(0L, 0L, "5200", "Factory Overhead & Power", debit = 0.0, credit = order.overheadCost)
            )
        }

        accountingDao.recordBalancedEntry(
            entry = JournalEntry(
                entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                memo = "Completed Production Batch ${order.batchNumber} (${order.targetQuantity} units ${order.finishedItemName})",
                sourceReference = order.batchNumber,
                totalAmount = order.totalBatchCost
            ),
            lines = lines
        )

        // 4. Mark order completed
        productionDao.updateOrder(
            order.copy(
                status = ProductionStatus.COMPLETED,
                completedQuantity = order.targetQuantity,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Accounting Streams & Actions ---
    val allAccounts = accountingDao.getAllAccounts()
    val allJournalEntries = accountingDao.getAllJournalEntries()

    fun getJournalLines(entryId: Long): Flow<List<JournalLine>> = accountingDao.getLinesForEntryFlow(entryId)

    suspend fun postManualJournalEntry(
        memo: String,
        sourceRef: String,
        lines: List<JournalLine>
    ): Long = withContext(Dispatchers.IO) {
        val totalDebits = lines.sumOf { it.debit }
        val totalCredits = lines.sumOf { it.credit }
        require(kotlin.math.abs(totalDebits - totalCredits) < 0.01) {
            "Total Debits ($totalDebits) must equal Total Credits ($totalCredits)"
        }
        accountingDao.recordBalancedEntry(
            entry = JournalEntry(
                entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                memo = memo,
                sourceReference = sourceRef,
                totalAmount = totalDebits
            ),
            lines = lines
        )
    }

    // --- Commercial Orders (Sales & Purchasing) ---
    val allCommercialOrders = commercialDao.getAllOrders()

    fun getCommercialOrderItems(orderId: Long): Flow<List<CommercialOrderItem>> =
        commercialDao.getItemsForOrder(orderId)

    suspend fun createCommercialOrder(
        order: CommercialOrder,
        items: List<CommercialOrderItem>
    ): Long = withContext(Dispatchers.IO) {
        val orderId = commercialDao.createOrderWithItems(order, items)
        val total = items.sumOf { it.lineTotal }

        // Post accounting entry for confirmed order
        if (order.type == CommercialType.COMMERCIAL_INVOICE) {
            // Debit Accounts Receivable (1100), Credit Sales Revenue (4000)
            accountingDao.recordBalancedEntry(
                entry = JournalEntry(
                    entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                    memo = "Commercial Invoice ${order.orderNumber} for ${order.partyName}",
                    sourceReference = order.orderNumber,
                    totalAmount = total
                ),
                lines = listOf(
                    JournalLine(0L, 0L, "1100", "Accounts Receivable (AR)", debit = total, credit = 0.0),
                    JournalLine(0L, 0L, "4000", "Textile Sales Revenue", debit = 0.0, credit = total)
                )
            )

            // Adjust inventory for finished goods sold if items are in stock
            for (item in items) {
                inventoryDao.adjustStock(item.itemId, -item.quantity)
                inventoryDao.insertTransaction(
                    StockTransaction(
                        itemId = item.itemId,
                        itemName = item.itemName,
                        type = TransactionType.SALES_DISPATCH,
                        quantityDelta = -item.quantity,
                        unitCost = item.unitPrice,
                        totalCost = item.lineTotal,
                        referenceNumber = order.orderNumber,
                        notes = "Sales dispatch for invoice ${order.orderNumber}"
                    )
                )
            }

            // Update customer balance if linked
            if (order.customerId != 0L) {
                customerDao.adjustCustomerBalance(order.customerId, total)
            }
        }
        orderId
    }

    suspend fun recordPayment(orderId: Long, paymentAmount: Double, bankAccountCode: String = "1020") =
        withContext(Dispatchers.IO) {
            val order = commercialDao.getOrderById(orderId) ?: return@withContext
            commercialDao.recordPayment(orderId, paymentAmount)

            // Double entry:
            if (order.type == CommercialType.COMMERCIAL_INVOICE) {
                if (order.customerId != 0L) {
                    customerDao.adjustCustomerBalance(order.customerId, -paymentAmount)
                }
                // Customer paid: Debit Bank (1020), Credit Accounts Receivable (1100)
                accountingDao.recordBalancedEntry(
                    entry = JournalEntry(
                        entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                        memo = "Payment received from ${order.partyName} on ${order.orderNumber}",
                        sourceReference = order.orderNumber,
                        totalAmount = paymentAmount
                    ),
                    lines = listOf(
                        JournalLine(0L, 0L, bankAccountCode, "Operating Bank Account", debit = paymentAmount, credit = 0.0),
                        JournalLine(0L, 0L, "1100", "Accounts Receivable (AR)", debit = 0.0, credit = paymentAmount)
                    )
                )
            } else {
                // Paid supplier: Debit Accounts Payable (2000), Credit Bank (1020)
                accountingDao.recordBalancedEntry(
                    entry = JournalEntry(
                        entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                        memo = "Payment to ${order.partyName} for ${order.orderNumber}",
                        sourceReference = order.orderNumber,
                        totalAmount = paymentAmount
                    ),
                    lines = listOf(
                        JournalLine(0L, 0L, "2000", "Accounts Payable (AP)", debit = paymentAmount, credit = 0.0),
                        JournalLine(0L, 0L, bankAccountCode, "Operating Bank Account", debit = 0.0, credit = paymentAmount)
                    )
                )
            }
        }
}
