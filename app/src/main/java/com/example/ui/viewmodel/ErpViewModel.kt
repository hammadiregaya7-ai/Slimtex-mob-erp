package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ErpRepository
import com.example.data.entity.Account
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialType
import com.example.data.entity.Customer
import com.example.data.entity.ErpCurrency
import com.example.data.entity.ErpRole
import com.example.data.entity.FabricRollLot
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.RollQualityGrade
import com.example.data.entity.StockThresholdAlert
import com.example.data.entity.StockTransaction
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierSpecialty
import com.example.data.entity.TransactionType
import com.example.data.entity.UnitOfMeasure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val totalInventoryValuation: Double = 0.0,
    val rawMaterialValuation: Double = 0.0,
    val finishedGoodsValuation: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val accountsReceivable: Double = 0.0,
    val accountsPayable: Double = 0.0,
    val cashAndBankBalance: Double = 0.0,
    val activeBatchesCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalCustomersCount: Int = 0,
    val customersWithBalanceCount: Int = 0,
    val totalSuppliersCount: Int = 0,
    val pendingApprovalsCount: Int = 0,
    val quarantinedRollsCount: Int = 0
)

class ErpViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ErpRepository = ErpRepository(AppDatabase.getInstance(application))

    // Active Role & Multi-Currency Display State
    private val _activeRole = MutableStateFlow(ErpRole.EXECUTIVE_CONTROLLER)
    val activeRole: StateFlow<ErpRole> = _activeRole.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(ErpCurrency.USD)
    val selectedCurrency: StateFlow<ErpCurrency> = _selectedCurrency.asStateFlow()

    fun setActiveRole(role: ErpRole) {
        _activeRole.value = role
    }

    fun setSelectedCurrency(currency: ErpCurrency) {
        _selectedCurrency.value = currency
    }

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<InventoryItem>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryStockSummaries: StateFlow<List<com.example.data.entity.CategoryStockSummary>> = repository.categoryStockSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeThresholdAlerts: StateFlow<List<StockThresholdAlert>> = repository.activeThresholdAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockTransactions: StateFlow<List<StockTransaction>> = repository.stockTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fabricRollLots: StateFlow<List<FabricRollLot>> = repository.allRollLots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productionOrders: StateFlow<List<ProductionOrder>> = repository.allProductionOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedOrderId = MutableStateFlow<Long?>(null)
    val selectedOrderId = _selectedOrderId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedOrderMaterials: StateFlow<List<ProductionMaterialItem>> = _selectedOrderId
        .flatMapLatest { id ->
            if (id != null) repository.getMaterialsForOrder(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commercialOrders: StateFlow<List<CommercialOrder>> = repository.allCommercialOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingApprovalOrders: StateFlow<List<CommercialOrder>> = repository.pendingApprovalOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class ExtraDashboardState(
        val suppliersCount: Int,
        val pendingApprovalsCount: Int,
        val quarantinedRollsCount: Int
    )

    private val extraDashboardState = combine(
        suppliers,
        pendingApprovalOrders,
        fabricRollLots
    ) { sups, pending, rolls ->
        ExtraDashboardState(
            suppliersCount = sups.size,
            pendingApprovalsCount = pending.size,
            quarantinedRollsCount = rolls.count { it.qualityGrade == RollQualityGrade.QUARANTINED }
        )
    }

    // Live Financial Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        combine(inventoryItems, accounts, productionOrders) { a, b, c -> Triple(a, b, c) },
        combine(lowStockItems, customers, extraDashboardState) { d, e, f -> Triple(d, e, f) }
    ) { (items, accs, orders), (lowStock, custs, extra) ->
        val totalInv = items.sumOf { it.currentStock * it.averageCost }
        val rawInv = items.filter { it.category == ItemCategory.RAW_MATERIAL || it.category == ItemCategory.TRIM || it.category == ItemCategory.DYE_CHEMICAL }
            .sumOf { it.currentStock * it.averageCost }
        val fgInv = items.filter { it.category == ItemCategory.FINISHED_GOOD }
            .sumOf { it.currentStock * it.averageCost }

        val ar = accs.find { it.accountCode == "1100" }?.currentBalance ?: 0.0
        val ap = accs.find { it.accountCode == "2000" }?.currentBalance ?: 0.0
        val rev = accs.filter { it.type == com.example.data.entity.AccountType.REVENUE }.sumOf { it.currentBalance }
        val exp = accs.filter { it.type == com.example.data.entity.AccountType.EXPENSE }.sumOf { it.currentBalance }
        val netProfit = rev - exp
        val marginPct = if (rev > 0.0) (netProfit / rev) * 100.0 else 0.0
        val cash = (accs.find { it.accountCode == "1010" }?.currentBalance ?: 0.0) +
                (accs.find { it.accountCode == "1020" }?.currentBalance ?: 0.0)
        val activeBatches = orders.count { it.status == com.example.data.entity.ProductionStatus.IN_PROGRESS }

        DashboardMetrics(
            totalInventoryValuation = totalInv,
            rawMaterialValuation = rawInv,
            finishedGoodsValuation = fgInv,
            totalRevenue = rev,
            totalExpenses = exp,
            netProfit = netProfit,
            profitMarginPercent = marginPct,
            accountsReceivable = ar,
            accountsPayable = ap,
            cashAndBankBalance = cash,
            activeBatchesCount = activeBatches,
            lowStockCount = lowStock.size,
            totalCustomersCount = custs.size,
            customersWithBalanceCount = custs.count { it.currentBalance > 0.01 },
            totalSuppliersCount = extra.suppliersCount,
            pendingApprovalsCount = extra.pendingApprovalsCount,
            quarantinedRollsCount = extra.quarantinedRollsCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun selectProductionOrder(orderId: Long?) {
        _selectedOrderId.value = orderId
    }

    // --- Inventory & Traceability Action Methods ---
    fun saveInventoryItem(
        id: Long = 0L,
        sku: String,
        name: String,
        category: ItemCategory,
        unit: UnitOfMeasure,
        currentStock: Double,
        reorderLevel: Double,
        averageCost: Double,
        sellingPrice: Double,
        location: String,
        description: String = "",
        preferredSupplier: String = "Indus Spinning & Weaving Mills"
    ) {
        viewModelScope.launch {
            repository.saveItem(
                InventoryItem(
                    id = id,
                    sku = sku,
                    name = name,
                    category = category,
                    unit = unit,
                    currentStock = currentStock,
                    reorderLevel = reorderLevel,
                    averageCost = averageCost,
                    sellingPrice = sellingPrice,
                    storageLocation = location,
                    description = description,
                    preferredSupplier = preferredSupplier
                )
            )
        }
    }

    fun deleteInventoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    fun updateItemThresholdConfig(
        itemId: Long,
        reorderLevel: Double,
        criticalThreshold: Double,
        autoAlertEnabled: Boolean,
        leadTimeDays: Int
    ) {
        viewModelScope.launch {
            repository.updateItemThresholdConfig(
                itemId = itemId,
                reorderLevel = reorderLevel,
                criticalThreshold = criticalThreshold,
                autoAlertEnabled = autoAlertEnabled,
                leadTimeDays = leadTimeDays
            )
        }
    }

    fun acknowledgeThresholdAlert(alertId: Long) {
        viewModelScope.launch {
            repository.acknowledgeThresholdAlert(alertId)
        }
    }

    fun generatePurchaseOrderForLowStockAlert(alert: StockThresholdAlert) {
        viewModelScope.launch {
            repository.generatePurchaseOrderForLowStockAlert(alert, _activeRole.value)
        }
    }

    fun recordStockMovement(
        itemId: Long,
        delta: Double,
        unitCost: Double,
        type: TransactionType,
        refNumber: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordStockMovement(itemId, delta, unitCost, type, refNumber, notes)
        }
    }

    // --- Fabric Roll & Dye-Lot Traceability ---
    fun saveFabricRollLot(
        id: Long = 0L,
        rollNumber: String,
        dyeLotCode: String,
        itemId: Long,
        itemSku: String,
        itemName: String,
        supplierName: String,
        shadeBand: String,
        lengthMeters: Double,
        gsm: Int,
        widthCm: Int,
        shrinkagePercent: Double,
        defectPoints: Int,
        qualityGrade: RollQualityGrade,
        batchUsedIn: String,
        inspectorNotes: String
    ) {
        viewModelScope.launch {
            repository.saveRollLot(
                FabricRollLot(
                    id = id,
                    rollNumber = rollNumber,
                    dyeLotCode = dyeLotCode,
                    itemId = itemId,
                    itemSku = itemSku,
                    itemName = itemName,
                    supplierName = supplierName,
                    shadeBand = shadeBand,
                    lengthMeters = lengthMeters,
                    gsm = gsm,
                    widthCm = widthCm,
                    shrinkagePercent = shrinkagePercent,
                    defectPointsPer100SqYds = defectPoints,
                    qualityGrade = qualityGrade,
                    batchUsedIn = batchUsedIn,
                    inspectorNotes = inspectorNotes
                )
            )
        }
    }

    fun updateRollInspection(
        rollId: Long,
        defectPoints: Int,
        grade: RollQualityGrade,
        shadeBand: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.updateRollInspection(rollId, defectPoints, grade, shadeBand, notes)
        }
    }

    fun deleteFabricRollLot(id: Long) {
        viewModelScope.launch {
            repository.deleteRollLot(id)
        }
    }

    // --- Supplier / Mill Directory Operations ---
    fun saveSupplier(
        id: Long = 0L,
        code: String,
        name: String,
        specialty: SupplierSpecialty = SupplierSpecialty.FABRIC_WEAVING_MILL,
        contactPerson: String = "",
        email: String = "",
        phone: String = "",
        country: String = "",
        currencyCode: String = "USD",
        leadTimeDays: Int = 10,
        qualityRating: Double = 4.8,
        paymentTerms: String = "Net 30",
        apBalance: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.saveSupplier(
                Supplier(
                    id = id,
                    code = code,
                    name = name,
                    specialty = specialty,
                    contactPerson = contactPerson,
                    email = email,
                    phone = phone,
                    country = country,
                    currencyCode = currencyCode,
                    leadTimeDays = leadTimeDays,
                    qualityRating = qualityRating,
                    paymentTerms = paymentTerms,
                    apBalance = apBalance,
                    notes = notes
                )
            )
        }
    }

    fun deleteSupplier(id: Long) {
        viewModelScope.launch {
            repository.deleteSupplier(id)
        }
    }

    fun recordSupplierDirectPayment(supplierId: Long, amount: Double, memo: String = "Mill AP Settlement") {
        viewModelScope.launch {
            repository.recordSupplierDirectPayment(supplierId, amount, memo)
        }
    }

    // --- Production Operations ---
    fun createProductionOrder(
        batchNumber: String,
        finishedItemId: Long,
        finishedItemName: String,
        targetQuantity: Double,
        directLaborCost: Double,
        overheadCost: Double,
        notes: String,
        materials: List<ProductionMaterialItem>
    ) {
        viewModelScope.launch {
            val orderId = repository.createProductionBatch(
                batchNumber,
                finishedItemId,
                finishedItemName,
                targetQuantity,
                directLaborCost,
                overheadCost,
                notes,
                materials
            )
            _selectedOrderId.value = orderId
        }
    }

    fun updateProductionOrderCosts(
        orderId: Long,
        directLaborCost: Double,
        overheadCost: Double
    ) {
        viewModelScope.launch {
            repository.updateProductionCosts(orderId, directLaborCost, overheadCost)
        }
    }

    fun addMaterialToOrder(material: ProductionMaterialItem) {
        viewModelScope.launch {
            repository.addMaterialToBatch(material)
        }
    }

    fun completeProductionOrder(orderId: Long) {
        viewModelScope.launch {
            repository.completeProductionBatch(orderId)
        }
    }

    // --- Commercial Orders, Document Chaining, Approval Gates & Packing Lists ---
    fun getCommercialOrderItems(orderId: Long): Flow<List<CommercialOrderItem>> =
        repository.getCommercialOrderItems(orderId)

    fun createCommercialOrder(
        orderNumber: String,
        type: CommercialType,
        partyName: String,
        notes: String,
        items: List<CommercialOrderItem>,
        customerId: Long = 0L,
        supplierId: Long = 0L,
        currencyCode: String = _selectedCurrency.value.code,
        exchangeRateToUsd: Double = _selectedCurrency.value.rateToUsd,
        incoterms: String = "FOB"
    ) {
        viewModelScope.launch {
            repository.createCommercialOrder(
                order = CommercialOrder(
                    orderNumber = orderNumber,
                    type = type,
                    partyName = partyName,
                    customerId = customerId,
                    supplierId = supplierId,
                    currencyCode = currencyCode,
                    exchangeRateToUsd = exchangeRateToUsd,
                    incoterms = incoterms,
                    notes = notes
                ),
                items = items,
                activeRole = _activeRole.value
            )
        }
    }

    fun approveCommercialOrder(orderId: Long) {
        viewModelScope.launch {
            repository.approveCommercialOrder(orderId, _activeRole.value.displayName)
        }
    }

    fun chainQuotationToWorkOrder(quoteId: Long) {
        viewModelScope.launch {
            val newOrderId = repository.chainQuotationToWorkOrder(quoteId)
            if (newOrderId != 0L) {
                _selectedOrderId.value = newOrderId
            }
        }
    }

    fun chainQuotationToInvoice(quoteId: Long) {
        viewModelScope.launch {
            repository.chainQuotationToInvoice(quoteId, _activeRole.value)
        }
    }

    fun updateExportPackingList(
        orderId: Long,
        packingListNumber: String,
        totalCartons: Int,
        netWeightKg: Double,
        grossWeightKg: Double,
        hsTariffCode: String,
        incoterms: String,
        portOfLoading: String,
        portOfDischarge: String
    ) {
        viewModelScope.launch {
            repository.updateExportPackingList(
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
    }

    fun recordCommercialPayment(orderId: Long, amount: Double) {
        viewModelScope.launch {
            repository.recordPayment(orderId, amount)
        }
    }

    // --- Customer / Client Operations ---
    fun saveCustomer(
        id: Long = 0L,
        code: String,
        name: String,
        accountType: com.example.data.entity.CustomerAccountType = com.example.data.entity.CustomerAccountType.WHOLESALE,
        contactPerson: String = "",
        email: String = "",
        phone: String = "",
        address: String = "",
        creditLimit: Double = 25000.0,
        paymentTerms: String = "Net 30",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.saveCustomer(
                com.example.data.entity.Customer(
                    id = id,
                    code = code,
                    name = name,
                    accountType = accountType,
                    contactPerson = contactPerson,
                    email = email,
                    phone = phone,
                    address = address,
                    creditLimit = creditLimit,
                    paymentTerms = paymentTerms,
                    notes = notes
                )
            )
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    fun recordCustomerDirectPayment(customerId: Long, amount: Double, memo: String = "Payment received") {
        viewModelScope.launch {
            repository.recordCustomerDirectPayment(customerId, amount, memo)
        }
    }

    fun getCustomerOrders(customerId: Long): Flow<List<CommercialOrder>> {
        return repository.getCustomerOrders(customerId)
    }

    fun postManualJournalEntry(
        memo: String,
        sourceRef: String,
        lines: List<JournalLine>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.postManualJournalEntry(memo, sourceRef, lines)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to post journal entry")
            }
        }
    }
}
