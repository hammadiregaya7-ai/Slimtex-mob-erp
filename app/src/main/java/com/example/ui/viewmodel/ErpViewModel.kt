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
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.StockTransaction
import com.example.data.entity.TransactionType
import com.example.data.entity.UnitOfMeasure
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val accountsReceivable: Double = 0.0,
    val accountsPayable: Double = 0.0,
    val cashAndBankBalance: Double = 0.0,
    val activeBatchesCount: Int = 0,
    val lowStockCount: Int = 0
)

class ErpViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ErpRepository = ErpRepository(AppDatabase.getInstance(application))

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<InventoryItem>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockTransactions: StateFlow<List<StockTransaction>> = repository.stockTransactions
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

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Financial Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        inventoryItems,
        accounts,
        productionOrders,
        lowStockItems
    ) { items, accs, orders, lowStock ->
        val totalInv = items.sumOf { it.currentStock * it.averageCost }
        val rawInv = items.filter { it.category == ItemCategory.RAW_MATERIAL || it.category == ItemCategory.TRIM || it.category == ItemCategory.DYE_CHEMICAL }
            .sumOf { it.currentStock * it.averageCost }
        val fgInv = items.filter { it.category == ItemCategory.FINISHED_GOOD }
            .sumOf { it.currentStock * it.averageCost }

        val ar = accs.find { it.accountCode == "1100" }?.currentBalance ?: 0.0
        val ap = accs.find { it.accountCode == "2000" }?.currentBalance ?: 0.0
        val rev = accs.find { it.accountCode == "4000" }?.currentBalance ?: 0.0
        val cash = (accs.find { it.accountCode == "1010" }?.currentBalance ?: 0.0) +
                (accs.find { it.accountCode == "1020" }?.currentBalance ?: 0.0)
        val activeBatches = orders.count { it.status == com.example.data.entity.ProductionStatus.IN_PROGRESS }

        DashboardMetrics(
            totalInventoryValuation = totalInv,
            rawMaterialValuation = rawInv,
            finishedGoodsValuation = fgInv,
            totalRevenue = rev,
            accountsReceivable = ar,
            accountsPayable = ap,
            cashAndBankBalance = cash,
            activeBatchesCount = activeBatches,
            lowStockCount = lowStock.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun selectProductionOrder(orderId: Long?) {
        _selectedOrderId.value = orderId
    }

    // --- Action Methods ---
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
        description: String = ""
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
                    description = description
                )
            )
        }
    }

    fun deleteInventoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
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

    fun createCommercialOrder(
        orderNumber: String,
        type: CommercialType,
        partyName: String,
        notes: String,
        items: List<CommercialOrderItem>,
        customerId: Long = 0L
    ) {
        viewModelScope.launch {
            repository.createCommercialOrder(
                CommercialOrder(
                    orderNumber = orderNumber,
                    type = type,
                    partyName = partyName,
                    customerId = customerId,
                    notes = notes
                ),
                items
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

    fun getCustomerOrders(customerId: Long): kotlinx.coroutines.flow.Flow<List<CommercialOrder>> {
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
