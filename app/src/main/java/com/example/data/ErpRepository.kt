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
import com.example.data.entity.ErpCurrency
import com.example.data.entity.ErpRole
import com.example.data.entity.FabricRollLot
import com.example.data.entity.RollQualityGrade
import com.example.data.entity.StockThresholdAlert
import com.example.data.entity.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ErpRepository(private val db: AppDatabase) {
    private val inventoryDao = db.inventoryDao()
    private val productionDao = db.productionDao()
    private val accountingDao = db.accountingDao()
    private val commercialDao = db.commercialDao()
    private val customerDao = db.customerDao()
    private val supplierDao = db.supplierDao()
    private val traceabilityDao = db.traceabilityDao()

    // --- Supplier / Mill Directory Streams & Actions ---
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()

    suspend fun saveSupplier(supplier: Supplier): Long = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) {
            supplierDao.insertSupplier(supplier)
        } else {
            supplierDao.updateSupplier(supplier)
            supplier.id
        }
    }

    suspend fun deleteSupplier(id: Long) = withContext(Dispatchers.IO) {
        supplierDao.deleteSupplier(id)
    }

    suspend fun recordSupplierDirectPayment(supplierId: Long, amount: Double, memo: String) = withContext(Dispatchers.IO) {
        val supplier = supplierDao.getSupplierById(supplierId) ?: return@withContext
        supplierDao.adjustSupplierApBalance(supplierId, -amount)

        accountingDao.recordBalancedEntry(
            entry = JournalEntry(
                entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                memo = "AP Settlement to ${supplier.name}: $memo",
                sourceReference = supplier.code,
                totalAmount = amount
            ),
            lines = listOf(
                JournalLine(0L, 0L, "2000", "Accounts Payable (AP)", debit = amount, credit = 0.0),
                JournalLine(0L, 0L, "1020", "Operating Bank Account", debit = 0.0, credit = amount)
            )
        )
    }

    // --- Fabric Roll & Dye-Lot Traceability Streams & Actions ---
    val allRollLots: Flow<List<FabricRollLot>> = traceabilityDao.getAllRollLots()

    suspend fun saveRollLot(rollLot: FabricRollLot): Long = withContext(Dispatchers.IO) {
        if (rollLot.id == 0L) {
            traceabilityDao.insertRollLot(rollLot)
        } else {
            traceabilityDao.updateRollLot(rollLot)
            rollLot.id
        }
    }

    suspend fun updateRollInspection(
        rollId: Long,
        defectPoints: Int,
        grade: RollQualityGrade,
        shadeBand: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        traceabilityDao.updateRollInspection(rollId, defectPoints, grade, shadeBand, notes)
    }

    suspend fun deleteRollLot(id: Long) = withContext(Dispatchers.IO) {
        traceabilityDao.deleteRollLot(id)
    }

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
    val criticalStockItems: Flow<List<InventoryItem>> = inventoryDao.getCriticalStockItems()
    val categoryStockSummaries: Flow<List<com.example.data.entity.CategoryStockSummary>> = inventoryDao.getCategoryStockSummaries()
    val activeThresholdAlerts: Flow<List<com.example.data.entity.StockThresholdAlert>> = inventoryDao.getActiveThresholdAlerts()
    val stockTransactions: Flow<List<StockTransaction>> = inventoryDao.getAllTransactions()

    fun getItemsByCategory(category: com.example.data.entity.ItemCategory): Flow<List<InventoryItem>> {
        return inventoryDao.getItemsByCategory(category)
    }

    private suspend fun syncItemThresholdAlert(itemId: Long) {
        val updated = inventoryDao.getItemById(itemId) ?: return
        inventoryDao.deleteAlertsForItem(itemId)
        if (updated.isLowStock) {
            inventoryDao.insertThresholdAlert(
                com.example.data.entity.StockThresholdAlert(
                    itemId = updated.id,
                    sku = updated.sku,
                    itemName = updated.name,
                    category = updated.category,
                    unit = updated.unit,
                    currentStock = updated.currentStock,
                    thresholdLevel = updated.reorderLevel,
                    criticalLevel = updated.criticalThreshold,
                    severity = updated.alertSeverity,
                    deficitQuantity = updated.stockDeficit,
                    recommendedOrderQuantity = updated.recommendedRestockQty
                )
            )
        }
    }

    suspend fun saveItem(item: InventoryItem): Long = withContext(Dispatchers.IO) {
        val savedId = if (item.id == 0L) {
            inventoryDao.insertItem(item)
        } else {
            inventoryDao.updateItem(item)
            item.id
        }
        syncItemThresholdAlert(savedId)
        savedId
    }

    suspend fun updateItemThresholdConfig(
        itemId: Long,
        reorderLevel: Double,
        criticalThreshold: Double,
        autoAlertEnabled: Boolean,
        leadTimeDays: Int
    ) = withContext(Dispatchers.IO) {
        inventoryDao.updateThresholdConfig(
            id = itemId,
            reorderLevel = reorderLevel,
            criticalThreshold = criticalThreshold,
            autoAlertEnabled = autoAlertEnabled,
            leadTimeDays = leadTimeDays
        )
        syncItemThresholdAlert(itemId)
    }

    suspend fun acknowledgeThresholdAlert(alertId: Long) = withContext(Dispatchers.IO) {
        inventoryDao.acknowledgeThresholdAlert(alertId)
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        inventoryDao.deleteAlertsForItem(id)
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
        syncItemThresholdAlert(itemId)

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

    // --- Commercial Orders (Sales, Quotations, Purchasing, Approval Gates & Document Chaining) ---
    val allCommercialOrders = commercialDao.getAllOrders()
    val pendingApprovalOrders = commercialDao.getPendingApprovalOrders()

    fun getCommercialOrderItems(orderId: Long): Flow<List<CommercialOrderItem>> =
        commercialDao.getItemsForOrder(orderId)

    suspend fun generatePurchaseOrderForLowStockAlert(
        alert: StockThresholdAlert,
        activeRole: ErpRole
    ): Long = withContext(Dispatchers.IO) {
        val item = inventoryDao.getItemById(alert.itemId)
        val supplierName = item?.preferredSupplier?.ifBlank { "Indus Spinning & Weaving Mills" }
            ?: "Indus Spinning & Weaving Mills"
        val supplier = supplierDao.getSupplierByName(supplierName)
        val unitCost = item?.averageCost ?: 5.0
        val qty = alert.recommendedOrderQuantity.coerceAtLeast(10.0)
        val lineTotal = qty * unitCost
        val needsControllerGate = lineTotal >= 5000.0 && !activeRole.canApproveHighValuePo

        val currency = ErpCurrency.fromCode(supplier?.currencyCode ?: "USD")
        val poNumber = "PO-2026-${(System.currentTimeMillis() % 900) + 100}"

        val order = CommercialOrder(
            orderNumber = poNumber,
            type = CommercialType.PURCHASE_ORDER,
            partyName = supplierName,
            supplierId = supplier?.id ?: 0L,
            status = if (needsControllerGate) CommercialStatus.PENDING_APPROVAL else CommercialStatus.CONFIRMED,
            requiresApproval = needsControllerGate,
            approvedByRole = if (!needsControllerGate) activeRole.displayName else "",
            currencyCode = currency.code,
            exchangeRateToUsd = currency.rateToUsd,
            incoterms = "CIF",
            parentDocumentRef = "ALERT-${alert.sku}",
            notes = "Auto-generated from Low-Stock Threshold Alert (${alert.sku})"
        )
        val items = listOf(
            CommercialOrderItem(
                orderId = 0L,
                itemId = alert.itemId,
                itemName = alert.itemName,
                unit = alert.unit,
                quantity = qty,
                unitPrice = unitCost,
                lineTotal = lineTotal
            )
        )
        val orderId = commercialDao.createOrderWithItems(order, items)
        if (!needsControllerGate && supplier != null) {
            supplierDao.adjustSupplierApBalance(supplier.id, lineTotal)
        }
        inventoryDao.acknowledgeThresholdAlert(alert.id)
        orderId
    }

    suspend fun createCommercialOrder(
        order: CommercialOrder,
        items: List<CommercialOrderItem>,
        activeRole: ErpRole = ErpRole.EXECUTIVE_CONTROLLER
    ): Long = withContext(Dispatchers.IO) {
        val total = items.sumOf { it.lineTotal }
        var requiresGate = order.requiresApproval

        if (order.type == CommercialType.PURCHASE_ORDER && total >= 5000.0 && !activeRole.canApproveHighValuePo) {
            requiresGate = true
        }
        if (order.type == CommercialType.COMMERCIAL_INVOICE && order.customerId != 0L) {
            val customer = customerDao.getCustomerById(order.customerId)
            if (customer != null && (customer.currentBalance + total) > customer.creditLimit && !activeRole.canOverrideCreditLimit) {
                requiresGate = true
            }
        }

        val finalOrder = order.copy(
            status = if (requiresGate) CommercialStatus.PENDING_APPROVAL else order.status,
            requiresApproval = requiresGate,
            approvedByRole = if (!requiresGate && order.approvedByRole.isBlank()) activeRole.displayName else order.approvedByRole
        )

        val orderId = commercialDao.createOrderWithItems(finalOrder, items)

        if (!requiresGate) {
            postOrderAccountingAndStockEffects(finalOrder.copy(id = orderId, totalAmount = total), items)
        }
        orderId
    }

    private suspend fun postOrderAccountingAndStockEffects(
        order: CommercialOrder,
        items: List<CommercialOrderItem>
    ) {
        val total = items.sumOf { it.lineTotal }
        if (order.type == CommercialType.COMMERCIAL_INVOICE) {
            accountingDao.recordBalancedEntry(
                entry = JournalEntry(
                    entryNumber = "JE-${System.currentTimeMillis() % 100000}",
                    memo = "Commercial Invoice ${order.orderNumber} for ${order.partyName} (${order.currencyCode})",
                    sourceReference = order.orderNumber,
                    totalAmount = total
                ),
                lines = listOf(
                    JournalLine(0L, 0L, "1100", "Accounts Receivable (AR)", debit = total, credit = 0.0),
                    JournalLine(0L, 0L, "4000", "Textile Sales Revenue", debit = 0.0, credit = total)
                )
            )

            for (item in items) {
                inventoryDao.adjustStock(item.itemId, -item.quantity)
                syncItemThresholdAlert(item.itemId)
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

            if (order.customerId != 0L) {
                customerDao.adjustCustomerBalance(order.customerId, total)
            }
        } else if (order.type == CommercialType.PURCHASE_ORDER && order.supplierId != 0L) {
            supplierDao.adjustSupplierApBalance(order.supplierId, total)
        }
    }

    suspend fun approveCommercialOrder(orderId: Long, approvedByRole: String) = withContext(Dispatchers.IO) {
        val order = commercialDao.getOrderById(orderId) ?: return@withContext
        if (order.status != CommercialStatus.PENDING_APPROVAL) return@withContext
        val items = commercialDao.getItemsForOrderSync(orderId)

        commercialDao.approveOrder(orderId, approvedByRole)
        postOrderAccountingAndStockEffects(
            order.copy(status = CommercialStatus.CONFIRMED, requiresApproval = false, approvedByRole = approvedByRole),
            items
        )
    }

    suspend fun chainQuotationToWorkOrder(quoteId: Long): Long = withContext(Dispatchers.IO) {
        val quote = commercialDao.getOrderById(quoteId) ?: return@withContext 0L
        val items = commercialDao.getItemsForOrderSync(quoteId)
        val firstLine = items.firstOrNull() ?: return@withContext 0L
        val batchNum = "BAT-2026-${(System.currentTimeMillis() % 800) + 150}"

        val allInv = inventoryDao.getAllItems()
        // Estimate standard raw material requirement (2.0m fabric per garment unit)
        val targetQty = firstLine.quantity.coerceAtLeast(10.0)
        val labor = targetQty * 3.25
        val overhead = targetQty * 1.40
        val estMaterialCost = targetQty * 7.50
        val totalBatch = estMaterialCost + labor + overhead

        val prodId = productionDao.insertOrder(
            ProductionOrder(
                batchNumber = batchNum,
                finishedItemId = firstLine.itemId,
                finishedItemName = firstLine.itemName,
                targetQuantity = targetQty,
                completedQuantity = 0.0,
                status = ProductionStatus.IN_PROGRESS,
                directLaborCost = labor,
                overheadCost = overhead,
                rawMaterialCost = estMaterialCost,
                totalBatchCost = totalBatch,
                unitProductionCost = totalBatch / targetQty,
                notes = "Chained from Sales Quotation ${quote.orderNumber} for ${quote.partyName}"
            )
        )
        commercialDao.updateLinkedBatchNumber(quoteId, batchNum)
        prodId
    }

    suspend fun chainQuotationToInvoice(quoteId: Long, activeRole: ErpRole): Long = withContext(Dispatchers.IO) {
        val quote = commercialDao.getOrderById(quoteId) ?: return@withContext 0L
        val items = commercialDao.getItemsForOrderSync(quoteId)
        val totalUnits = items.sumOf { it.quantity }.toInt().coerceAtLeast(1)
        val cartons = (totalUnits / 20).coerceAtLeast(1)
        val netKg = totalUnits * 0.42
        val grossKg = netKg + (cartons * 1.1)
        val invNum = "INV-2026-${(System.currentTimeMillis() % 800) + 120}"
        val pklNum = "PKL-${invNum.removePrefix("INV-")}"

        val invoiceOrder = CommercialOrder(
            orderNumber = invNum,
            type = CommercialType.COMMERCIAL_INVOICE,
            partyName = quote.partyName,
            customerId = quote.customerId,
            status = CommercialStatus.CONFIRMED,
            currencyCode = quote.currencyCode,
            exchangeRateToUsd = quote.exchangeRateToUsd,
            incoterms = quote.incoterms,
            parentDocumentRef = quote.orderNumber,
            linkedBatchNumber = quote.linkedBatchNumber,
            packingListNumber = pklNum,
            totalCartons = cartons,
            netWeightKg = netKg,
            grossWeightKg = grossKg,
            hsTariffCode = quote.hsTariffCode,
            portOfLoading = quote.portOfLoading,
            portOfDischarge = quote.portOfDischarge,
            notes = "Chained from ${quote.orderNumber}" +
                if (quote.linkedBatchNumber.isNotBlank()) " & Work Order ${quote.linkedBatchNumber}" else ""
        )

        val newInvoiceId = createCommercialOrder(
            order = invoiceOrder,
            items = items.map { it.copy(id = 0L, orderId = 0L) },
            activeRole = activeRole
        )
        commercialDao.updateOrder(quote.copy(status = CommercialStatus.FULFILLED))
        newInvoiceId
    }

    suspend fun updateExportPackingList(
        orderId: Long,
        packingListNumber: String,
        totalCartons: Int,
        netWeightKg: Double,
        grossWeightKg: Double,
        hsTariffCode: String,
        incoterms: String,
        portOfLoading: String,
        portOfDischarge: String
    ) = withContext(Dispatchers.IO) {
        commercialDao.updateExportPackingList(
            orderId = orderId,
            packingListNumber = packingListNumber,
            totalCartons = totalCartons,
            netWeightKg = netWeightKg,
            grossWeightKg = grossWeightKg,
            hsTariffCode = hsTariffCode,
            incoterms = incoterms,
            portOfLoading = portOfLoading,
            portOfDischarge = portOfDischarge
        )
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
            } else if (order.type == CommercialType.PURCHASE_ORDER) {
                if (order.supplierId != 0L) {
                    supplierDao.adjustSupplierApBalance(order.supplierId, -paymentAmount)
                }
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
