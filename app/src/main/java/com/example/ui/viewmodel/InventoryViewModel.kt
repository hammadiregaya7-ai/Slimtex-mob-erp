package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ErpRepository
import com.example.data.entity.CategoryStockSummary
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.StockThresholdAlert
import com.example.data.entity.StockTransaction
import com.example.data.entity.TransactionType
import com.example.data.entity.UnitOfMeasure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryStockUiState(
    val allItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val selectedCategory: ItemCategory? = null,
    val searchQuery: String = "",
    val onlyLowStockAlerts: Boolean = false,
    val categorySummaries: List<CategoryStockSummary> = emptyList(),
    val lowStockItems: List<InventoryItem> = emptyList(),
    val criticalStockItems: List<InventoryItem> = emptyList(),
    val activeThresholdAlerts: List<StockThresholdAlert> = emptyList(),
    val totalValuation: Double = 0.0,
    val lowStockAlertCount: Int = 0,
    val criticalAlertCount: Int = 0
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ErpRepository(AppDatabase.getInstance(application))

    private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
    val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlyLowStockAlerts = MutableStateFlow(false)
    val onlyLowStockAlerts: StateFlow<Boolean> = _onlyLowStockAlerts.asStateFlow()

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<InventoryItem>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val criticalStockItems: StateFlow<List<InventoryItem>> = repository.criticalStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySummaries: StateFlow<List<CategoryStockSummary>> = repository.categoryStockSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeThresholdAlerts: StateFlow<List<StockThresholdAlert>> = repository.activeThresholdAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockTransactions: StateFlow<List<StockTransaction>> = repository.stockTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<InventoryStockUiState> = combine(
        inventoryItems,
        categorySummaries,
        activeThresholdAlerts,
        _selectedCategory,
        combine(_searchQuery, _onlyLowStockAlerts) { q, lowOnly -> q to lowOnly }
    ) { items, summaries, alerts, category, (query, lowOnly) ->
        val filtered = items.filter { item ->
            val matchesCategory = category == null || item.category == category
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.sku.contains(query, ignoreCase = true) ||
                item.storageLocation.contains(query, ignoreCase = true)
            val matchesLowStock = !lowOnly || item.isLowStock
            matchesCategory && matchesQuery && matchesLowStock
        }
        val lowStock = items.filter { it.isLowStock }
        val criticalStock = items.filter { it.isCriticalStock }

        InventoryStockUiState(
            allItems = items,
            filteredItems = filtered,
            selectedCategory = category,
            searchQuery = query,
            onlyLowStockAlerts = lowOnly,
            categorySummaries = summaries,
            lowStockItems = lowStock,
            criticalStockItems = criticalStock,
            activeThresholdAlerts = alerts,
            totalValuation = items.sumOf { it.currentStock * it.averageCost },
            lowStockAlertCount = lowStock.size,
            criticalAlertCount = criticalStock.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryStockUiState())

    fun selectCategory(category: ItemCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLowStockFilter(enabled: Boolean) {
        _onlyLowStockAlerts.value = enabled
    }

    fun saveInventoryItem(
        id: Long = 0L,
        sku: String,
        name: String,
        category: ItemCategory,
        unit: UnitOfMeasure,
        currentStock: Double,
        reorderLevel: Double,
        criticalThreshold: Double = (reorderLevel * 0.4).coerceAtLeast(1.0),
        averageCost: Double,
        sellingPrice: Double = 0.0,
        location: String = "Main Warehouse",
        description: String = "",
        autoAlertEnabled: Boolean = true,
        preferredSupplier: String = "Primary Textile Mill",
        leadTimeDays: Int = 7
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
                    criticalThreshold = criticalThreshold,
                    averageCost = averageCost,
                    sellingPrice = sellingPrice,
                    storageLocation = location,
                    description = description,
                    autoAlertEnabled = autoAlertEnabled,
                    preferredSupplier = preferredSupplier,
                    leadTimeDays = leadTimeDays
                )
            )
        }
    }

    fun adjustStockLevel(
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

    fun updateThresholdAlertConfig(
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

    fun deleteInventoryItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }
}
