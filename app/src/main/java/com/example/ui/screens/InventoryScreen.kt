package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.StockTransaction
import com.example.data.entity.TransactionType
import com.example.data.entity.UnitOfMeasure
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.ErpViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class InventorySortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    VALUATION_DESC("Highest Value"),
    STOCK_LOW_FIRST("Low Stock First")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: ErpViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val stockTransactions by viewModel.stockTransactions.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(InventorySortOption.NAME_ASC) }

    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showStockMovementDialog by remember { mutableStateOf(false) }
    var selectedItemForMovement by remember { mutableStateOf<InventoryItem?>(null) }
    var inspectedItemForHistory by remember { mutableStateOf<InventoryItem?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    val totalValuation = items.sumOf { it.currentStock * it.averageCost }
    val lowStockCount = items.count { it.currentStock <= it.reorderLevel }

    val filteredItems = remember(items, searchQuery, selectedCategory, filterLowStockOnly, selectedSort) {
        items.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true) ||
                    item.storageLocation.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesLowStock = !filterLowStockOnly || item.currentStock <= item.reorderLevel
            matchesSearch && matchesCategory && matchesLowStock
        }.sortedWith { a, b ->
            when (selectedSort) {
                InventorySortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                InventorySortOption.VALUATION_DESC -> (b.currentStock * b.averageCost).compareTo(a.currentStock * a.averageCost)
                InventorySortOption.STOCK_LOW_FIRST -> {
                    val ratioA = if (a.reorderLevel > 0) a.currentStock / a.reorderLevel else 999.0
                    val ratioB = if (b.reorderLevel > 0) b.currentStock / b.reorderLevel else 999.0
                    ratioA.compareTo(ratioB)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("inventory_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventory & Materials",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${items.size} catalog SKUs • Total Value: ${currencyFormat.format(totalValuation)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedItemForMovement = items.firstOrNull()
                            showStockMovementDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        modifier = Modifier.testTag("btn_stock_in_out")
                    ) {
                        Text("Stock Movement")
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_item")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                        Spacer(Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input"),
                placeholder = { Text("Search fabrics, yarn, finished apparel, trims by SKU, name, or aisle...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Category, Low Stock & Sort Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !filterLowStockOnly,
                        onClick = {
                            selectedCategory = null
                            filterLowStockOnly = false
                        },
                        label = { Text("All (${items.size})") },
                        leadingIcon = if (selectedCategory == null && !filterLowStockOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                item {
                    FilterChip(
                        selected = filterLowStockOnly,
                        onClick = { filterLowStockOnly = !filterLowStockOnly },
                        label = { Text("Low Stock ($lowStockCount)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (filterLowStockOnly) AmberDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_low_stock_chip")
                    )
                }
                items(ItemCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(category.name.replace('_', ' ')) },
                        leadingIcon = if (selectedCategory == category) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                item {
                    FilterChip(
                        selected = selectedSort != InventorySortOption.NAME_ASC,
                        onClick = {
                            val nextIdx = (selectedSort.ordinal + 1) % InventorySortOption.values().size
                            selectedSort = InventorySortOption.values()[nextIdx]
                        },
                        label = { Text("Sort: ${selectedSort.label}") },
                        leadingIcon = {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("inventory_sort_chip")
                    )
                }
            }

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No inventory items found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onInspectHistory = { inspectedItemForHistory = item },
                            onStockMovement = {
                                selectedItemForMovement = item
                                showStockMovementDialog = true
                            },
                            onEdit = { itemToEdit = item },
                            onDelete = { viewModel.deleteInventoryItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Item Stock History & Unit Economics Modal
    if (inspectedItemForHistory != null) {
        val itemHistory = stockTransactions.filter { it.itemId == inspectedItemForHistory!!.id }
        ItemStockHistoryDialog(
            item = inspectedItemForHistory!!,
            transactions = itemHistory,
            currencyFormat = currencyFormat,
            onDismiss = { inspectedItemForHistory = null },
            onRecordMovement = {
                val target = inspectedItemForHistory
                inspectedItemForHistory = null
                selectedItemForMovement = target
                showStockMovementDialog = true
            }
        )
    }

    // Add / Edit Item Dialog
    if (showAddDialog || itemToEdit != null) {
        ItemFormDialog(
            item = itemToEdit,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { sku, name, category, unit, stock, reorder, cost, selling, location, desc ->
                viewModel.saveInventoryItem(
                    id = itemToEdit?.id ?: 0L,
                    sku = sku,
                    name = name,
                    category = category,
                    unit = unit,
                    currentStock = stock,
                    reorderLevel = reorder,
                    averageCost = cost,
                    sellingPrice = selling,
                    location = location,
                    description = desc
                )
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // Stock Movement Dialog
    if (showStockMovementDialog && selectedItemForMovement != null) {
        StockMovementDialog(
            item = selectedItemForMovement!!,
            allItems = items,
            onDismiss = { showStockMovementDialog = false },
            onConfirm = { itemId, delta, cost, type, refNumber, notes ->
                viewModel.recordStockMovement(itemId, delta, cost, type, refNumber, notes)
                showStockMovementDialog = false
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    currencyFormat: NumberFormat,
    onInspectHistory: () -> Unit = {},
    onStockMovement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.currentStock <= item.reorderLevel
    val totalValuation = item.currentStock * item.averageCost
    val stockHealthProgress = if (item.reorderLevel > 0) {
        (item.currentStock / (item.reorderLevel * 2.5)).coerceIn(0.05, 1.0).toFloat()
    } else 1f
    val grossMarginPct = if (item.sellingPrice > 0.0 && item.averageCost > 0.0) {
        ((item.sellingPrice - item.averageCost) / item.sellingPrice) * 100.0
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspectHistory() }
            .testTag("item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.sku,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (item.category == ItemCategory.FINISHED_GOOD) EmeraldLight else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.category.name.replace('_', ' '),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (item.category == ItemCategory.FINISHED_GOOD) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (grossMarginPct > 0.0) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = EmeraldLight.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f%% Margin", grossMarginPct),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (isLowStock) {
                    Surface(
                        color = AmberLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = AmberDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "LOW STOCK",
                                color = AmberDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Location: ${item.storageLocation} • Reorder Point: ${item.reorderLevel} ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            // Stock & Valuation metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.currentStock} ${item.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) AmberDark else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Avg Unit Cost",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(item.averageCost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Valuation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalValuation),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Stock Health Progress Bar
            LinearProgressIndicator(
                progress = { stockHealthProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isLowStock) AmberDark else EmeraldAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onInspectHistory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Stock Ledger →", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onStockMovement,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (isLowStock) "Restock Now" else "Stock In/Out", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormDialog(
    item: InventoryItem?,
    onDismiss: () -> Unit,
    onSave: (
        sku: String,
        name: String,
        category: ItemCategory,
        unit: UnitOfMeasure,
        stock: Double,
        reorder: Double,
        cost: Double,
        selling: Double,
        location: String,
        desc: String
    ) -> Unit
) {
    var sku by remember { mutableStateOf(item?.sku ?: "") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: ItemCategory.RAW_MATERIAL) }
    var unit by remember { mutableStateOf(item?.unit ?: UnitOfMeasure.METERS) }
    var currentStock by remember { mutableStateOf(item?.currentStock?.toString() ?: "0") }
    var reorderLevel by remember { mutableStateOf(item?.reorderLevel?.toString() ?: "100") }
    var averageCost by remember { mutableStateOf(item?.averageCost?.toString() ?: "0.0") }
    var sellingPrice by remember { mutableStateOf(item?.sellingPrice?.toString() ?: "0.0") }
    var location by remember { mutableStateOf(item?.storageLocation ?: "Aisle 1 - Bay A") }
    var desc by remember { mutableStateOf(item?.description ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "New Inventory Item" else "Edit Item") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Barcode Code") },
                        modifier = Modifier.fillMaxWidth().testTag("input_sku")
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_name")
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category.name.replace('_', ' '),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            ItemCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name.replace('_', ' ')) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = unit.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit of Measure") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            UnitOfMeasure.values().forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.name) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("Current Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = reorderLevel,
                            onValueChange = { reorderLevel = it },
                            label = { Text("Reorder Level") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = averageCost,
                            onValueChange = { averageCost = it },
                            label = { Text("Unit Cost ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("Selling Price ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Warehouse Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        sku,
                        name,
                        category,
                        unit,
                        currentStock.toDoubleOrNull() ?: 0.0,
                        reorderLevel.toDoubleOrNull() ?: 0.0,
                        averageCost.toDoubleOrNull() ?: 0.0,
                        sellingPrice.toDoubleOrNull() ?: 0.0,
                        location,
                        desc
                    )
                },
                enabled = sku.isNotBlank() && name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementDialog(
    item: InventoryItem,
    allItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (itemId: Long, delta: Double, unitCost: Double, type: TransactionType, refNumber: String, notes: String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(item) }
    var isStockIn by remember { mutableStateOf(true) }
    var quantityText by remember { mutableStateOf("") }
    var unitCostText by remember { mutableStateOf(item.averageCost.toString()) }
    var referenceNumber by remember { mutableStateOf("PO-${System.currentTimeMillis() % 10000}") }
    var notes by remember { mutableStateOf("") }

    var itemDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Stock Movement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Item Selector
                ExposedDropdownMenuBox(
                    expanded = itemDropdownExpanded,
                    onExpandedChange = { itemDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedItem.sku} - ${selectedItem.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Material / Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = itemDropdownExpanded,
                        onDismissRequest = { itemDropdownExpanded = false }
                    ) {
                        allItems.forEach { itm ->
                            DropdownMenuItem(
                                text = { Text("${itm.sku} - ${itm.name} (${itm.currentStock} ${itm.unit})") },
                                onClick = {
                                    selectedItem = itm
                                    unitCostText = itm.averageCost.toString()
                                    itemDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // In / Out Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isStockIn = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStockIn) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isStockIn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stock In (Receipt)")
                    }

                    Button(
                        onClick = { isStockIn = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isStockIn) Color(0xFFDC2626) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isStockIn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stock Out")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity (${selectedItem.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("input_stock_qty")
                    )
                    OutlinedTextField(
                        value = unitCostText,
                        onValueChange = { unitCostText = it },
                        label = { Text("Unit Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Reference (PO #, Batch #, or Invoice #)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val cost = unitCostText.toDoubleOrNull() ?: selectedItem.averageCost
                    val delta = if (isStockIn) qty else -qty
                    val type = if (isStockIn) TransactionType.PURCHASE_RECEIPT else TransactionType.ADJUSTMENT
                    onConfirm(selectedItem.id, delta, cost, type, referenceNumber, notes)
                },
                enabled = (quantityText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ItemStockHistoryDialog(
    item: InventoryItem,
    transactions: List<StockTransaction>,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onRecordMovement: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val totalValuation = item.currentStock * item.averageCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${item.sku} • ${item.name}", fontWeight = FontWeight.Bold)
                Text(
                    text = "Stock Ledger & Unit Economics (${transactions.size} entries)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("On-Hand Stock", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${item.currentStock} ${item.unit}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avg Cost / Retail", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${currencyFormat.format(item.averageCost)} / ${currencyFormat.format(item.sellingPrice)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Value", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(totalValuation),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "No stock movement transactions recorded yet for this SKU.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        val isPositive = tx.quantityDelta >= 0
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.type.name.replace('_', ' '),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${tx.referenceNumber} • ${dateFormat.format(Date(tx.timestamp))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${tx.quantityDelta} ${item.unit}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) EmeraldDark else Color(0xFFDC2626),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = currencyFormat.format(tx.totalCost),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRecordMovement,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("Record Movement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

