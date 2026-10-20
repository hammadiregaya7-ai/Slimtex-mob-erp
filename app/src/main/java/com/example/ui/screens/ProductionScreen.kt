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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.ProductionStatus
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

@Composable
fun ProductionScreen(
    viewModel: ErpViewModel,
    isExpandedScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.productionOrders.collectAsStateWithLifecycle()
    val selectedOrderId by viewModel.selectedOrderId.collectAsStateWithLifecycle()
    val materials by viewModel.selectedOrderMaterials.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()

    var statusFilter by remember { mutableStateOf<ProductionStatus?>(null) }
    var showCreateBatchDialog by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var showEditCostDialog by remember { mutableStateOf(false) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    // Select first order by default if none selected
    val activeOrder = orders.find { it.id == selectedOrderId } ?: orders.firstOrNull()

    val filteredOrders = orders.filter { order ->
        statusFilter == null || order.status == statusFilter
    }

    Box(modifier = modifier.fillMaxSize().testTag("production_screen")) {
        if (isExpandedScreen) {
            // Tablet / Dual-Pane Layout (Master on Left, Detail Cost Sheet on Right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Batches list
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductionListHeader(
                        onCreateBatch = { showCreateBatchDialog = true },
                        statusFilter = statusFilter,
                        onStatusFilterChange = { statusFilter = it }
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            ProductionOrderCard(
                                order = order,
                                isSelected = order.id == activeOrder?.id,
                                currencyFormat = currencyFormat,
                                onClick = { viewModel.selectProductionOrder(order.id) }
                            )
                        }
                    }
                }

                // Right Pane: Active Cost Sheet
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxSize()
                ) {
                    if (activeOrder != null) {
                        CostSheetDetailView(
                            order = activeOrder,
                            materials = materials,
                            currencyFormat = currencyFormat,
                            onAddMaterial = { showAddMaterialDialog = true },
                            onEditCosts = { showEditCostDialog = true },
                            onCompleteBatch = { viewModel.completeProductionOrder(activeOrder.id) },
                            finishedItem = inventoryItems.find { it.id == activeOrder.finishedItemId }
                        )
                    } else {
                        EmptyCostSheetState()
                    }
                }
            }
        } else {
            // Phone / Compact Layout (Tabs or Stack)
            var currentTab by remember { mutableStateOf(0) } // 0 = Batches, 1 = Cost Sheet

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            label = { Text("Batches (${orders.size})") }
                        )
                        FilterChip(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            label = { Text("Cost Sheet") }
                        )
                    }

                    Button(
                        onClick = { showCreateBatchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        modifier = Modifier.testTag("btn_create_batch")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Batch")
                    }
                }

                if (currentTab == 0) {
                    ProductionListHeader(
                        onCreateBatch = { showCreateBatchDialog = true },
                        statusFilter = statusFilter,
                        onStatusFilterChange = { statusFilter = it }
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            ProductionOrderCard(
                                order = order,
                                isSelected = order.id == activeOrder?.id,
                                currencyFormat = currencyFormat,
                                onClick = {
                                    viewModel.selectProductionOrder(order.id)
                                    currentTab = 1 // Switch to Cost Sheet
                                }
                            )
                        }
                    }
                } else {
                    if (activeOrder != null) {
                        CostSheetDetailView(
                            order = activeOrder,
                            materials = materials,
                            currencyFormat = currencyFormat,
                            onAddMaterial = { showAddMaterialDialog = true },
                            onEditCosts = { showEditCostDialog = true },
                            onCompleteBatch = { viewModel.completeProductionOrder(activeOrder.id) },
                            finishedItem = inventoryItems.find { it.id == activeOrder.finishedItemId }
                        )
                    } else {
                        EmptyCostSheetState()
                    }
                }
            }
        }
    }

    // Dialog: Create New Batch
    if (showCreateBatchDialog) {
        CreateBatchDialog(
            inventoryItems = inventoryItems,
            onDismiss = { showCreateBatchDialog = false },
            onConfirm = { batchNumber, finishedItemId, finishedItemName, qty, labor, overhead, notes, boms ->
                viewModel.createProductionOrder(
                    batchNumber,
                    finishedItemId,
                    finishedItemName,
                    qty,
                    labor,
                    overhead,
                    notes,
                    boms
                )
                showCreateBatchDialog = false
            }
        )
    }

    // Dialog: Add Material to active order
    if (showAddMaterialDialog && activeOrder != null) {
        val rawMaterials = inventoryItems.filter { it.category != ItemCategory.FINISHED_GOOD }
        AddMaterialDialog(
            orderId = activeOrder.id,
            rawMaterials = rawMaterials,
            onDismiss = { showAddMaterialDialog = false },
            onConfirm = { material ->
                viewModel.addMaterialToOrder(material)
                showAddMaterialDialog = false
            }
        )
    }

    // Dialog: Edit Labor & Overhead
    if (showEditCostDialog && activeOrder != null) {
        EditCostsDialog(
            order = activeOrder,
            onDismiss = { showEditCostDialog = false },
            onConfirm = { labor, overhead ->
                viewModel.updateProductionOrderCosts(activeOrder.id, labor, overhead)
                showEditCostDialog = false
            }
        )
    }
}

@Composable
fun ProductionListHeader(
    onCreateBatch: () -> Unit,
    statusFilter: ProductionStatus?,
    onStatusFilterChange: (ProductionStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("All", fontSize = 12.sp) }
                )
            }
            items(ProductionStatus.values()) { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(if (statusFilter == status) null else status) },
                    label = { Text(status.name.replace('_', ' '), fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
fun ProductionOrderCard(
    order: ProductionOrder,
    isSelected: Boolean,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("batch_card_${order.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.batchNumber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                ProductionStatusBadge(status = order.status)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = order.finishedItemName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target: ${order.targetQuantity.toInt()} units",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Unit Cost: ${currencyFormat.format(order.unitProductionCost)}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) EmeraldAccent else EmeraldDark
                )
            }
        }
    }
}

@Composable
fun CostSheetDetailView(
    order: ProductionOrder,
    materials: List<ProductionMaterialItem>,
    currencyFormat: NumberFormat,
    onAddMaterial: () -> Unit,
    onEditCosts: () -> Unit,
    onCompleteBatch: () -> Unit,
    finishedItem: InventoryItem?
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Calculations
    val materialSubtotal = materials.sumOf { it.totalCost }
    val totalCost = materialSubtotal + order.directLaborCost + order.overheadCost
    val unitCost = if (order.targetQuantity > 0) totalCost / order.targetQuantity else 0.0

    val sellingPrice = finishedItem?.sellingPrice ?: 0.0
    val marginPerUnit = sellingPrice - unitCost
    val marginPercent = if (sellingPrice > 0) (marginPerUnit / sellingPrice) * 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cost_sheet_detail"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cost Sheet: ${order.batchNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            ProductionStatusBadge(status = order.status)
                        }
                        Text(
                            text = "Product: ${order.finishedItemName} (${order.targetQuantity.toInt()} units target)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (order.status != ProductionStatus.COMPLETED) {
                        Button(
                            onClick = onCompleteBatch,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier.testTag("btn_complete_production")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Complete & Stock In")
                        }
                    }
                }
            }

            // Summary Financial Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Batch Cost", style = MaterialTheme.typography.labelSmall)
                            Text(
                                currencyFormat.format(totalCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Cost Per Unit", style = MaterialTheme.typography.labelSmall)
                            Text(
                                currencyFormat.format(unitCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldDark
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Projected Margin", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format(Locale.US, "%.1f", marginPercent)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (marginPercent >= 25.0) EmeraldDark else AmberDark
                            )
                        }
                    }
                }
            }

            // Section 1: Bill of Materials (BOM)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Bill of Materials (Raw Material Requisition)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (order.status != ProductionStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = onAddMaterial,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Material", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (materials.isEmpty()) {
                item {
                    Text(
                        text = "No materials added to this BOM yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(materials) { mat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(mat.itemName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${mat.quantityRequired} ${mat.unit.name} @ ${currencyFormat.format(mat.unitCost)} / unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            currencyFormat.format(mat.totalCost),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Section 2: Direct Labor & Overhead Expenses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Conversion Costs (Labor & Factory Overhead)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (order.status != ProductionStatus.COMPLETED) {
                        IconButton(onClick = onEditCosts, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Costs", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Direct Tailoring & Cutting Labor", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            currencyFormat.format(order.directLaborCost),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Factory Machinery & Power Overhead", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            currencyFormat.format(order.overheadCost),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Divider(Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Manufacturing Batch Cost",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            currencyFormat.format(totalCost),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldDark,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Completion notes or status
            item {
                if (order.status == ProductionStatus.COMPLETED) {
                    Surface(
                        color = EmeraldLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Production run completed. ${order.targetQuantity.toInt()} units added to Finished Goods inventory and double-entry General Ledger entry posted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionStatusBadge(status: ProductionStatus) {
    val (bg, fg, label) = when (status) {
        ProductionStatus.PLANNED -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "PLANNED")
        ProductionStatus.IN_PROGRESS -> Triple(AmberLight, AmberDark, "IN PROGRESS")
        ProductionStatus.COMPLETED -> Triple(EmeraldLight, EmeraldDark, "COMPLETED")
        ProductionStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "CANCELLED")
    }

    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyCostSheetState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PrecisionManufacturing,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Select a production batch to view or edit its Cost Sheet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchDialog(
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (
        batchNumber: String,
        finishedItemId: Long,
        finishedItemName: String,
        qty: Double,
        labor: Double,
        overhead: Double,
        notes: String,
        boms: List<ProductionMaterialItem>
    ) -> Unit
) {
    val finishedGoods = inventoryItems.filter { it.category == ItemCategory.FINISHED_GOOD }
    val rawMaterials = inventoryItems.filter { it.category != ItemCategory.FINISHED_GOOD }

    var batchNumber by remember { mutableStateOf("BAT-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(100..999).random()}") }
    var selectedFinishedItem by remember { mutableStateOf(finishedGoods.firstOrNull()) }
    var targetQuantityText by remember { mutableStateOf("250") }
    var directLaborText by remember { mutableStateOf("500") }
    var overheadText by remember { mutableStateOf("250") }
    var notes by remember { mutableStateOf("") }

    var finishedItemDropdownExpanded by remember { mutableStateOf(false) }

    // Selected raw materials BOM for this batch
    val bomList = remember { mutableStateListOf<ProductionMaterialItem>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Manufacturing Production Batch") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number") },
                        modifier = Modifier.fillMaxWidth().testTag("input_batch_number")
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = finishedItemDropdownExpanded,
                        onExpandedChange = { finishedItemDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFinishedItem?.name ?: "Select Finished Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Apparel / Finished Good") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = finishedItemDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = finishedItemDropdownExpanded,
                            onDismissRequest = { finishedItemDropdownExpanded = false }
                        ) {
                            finishedGoods.forEach { itm ->
                                DropdownMenuItem(
                                    text = { Text("${itm.sku} - ${itm.name}") },
                                    onClick = {
                                        selectedFinishedItem = itm
                                        finishedItemDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = targetQuantityText,
                        onValueChange = { targetQuantityText = it },
                        label = { Text("Target Units to Manufacture") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("input_batch_qty")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = directLaborText,
                            onValueChange = { directLaborText = it },
                            label = { Text("Tailoring Labor ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = overheadText,
                            onValueChange = { overheadText = it },
                            label = { Text("Overhead & Power ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Batch Notes / Client Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finished = selectedFinishedItem ?: return@Button
                    val qty = targetQuantityText.toDoubleOrNull() ?: 0.0
                    val labor = directLaborText.toDoubleOrNull() ?: 0.0
                    val overhead = overheadText.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        batchNumber,
                        finished.id,
                        finished.name,
                        qty,
                        labor,
                        overhead,
                        notes,
                        bomList.toList()
                    )
                },
                enabled = selectedFinishedItem != null && (targetQuantityText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Create Batch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialDialog(
    orderId: Long,
    rawMaterials: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (ProductionMaterialItem) -> Unit
) {
    var selectedRawItem by remember { mutableStateOf(rawMaterials.firstOrNull()) }
    var quantityText by remember { mutableStateOf("100") }
    var unitCostText by remember { mutableStateOf(selectedRawItem?.averageCost?.toString() ?: "0.0") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Raw Material to BOM") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRawItem?.name ?: "Select Material",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Raw Material / Trim") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        rawMaterials.forEach { mat ->
                            DropdownMenuItem(
                                text = { Text("${mat.sku} - ${mat.name} (${mat.currentStock} ${mat.unit})") },
                                onClick = {
                                    selectedRawItem = mat
                                    unitCostText = mat.averageCost.toString()
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity (${selectedRawItem?.unit?.name ?: "Units"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitCostText,
                        onValueChange = { unitCostText = it },
                        label = { Text("Unit Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedRawItem ?: return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val cost = unitCostText.toDoubleOrNull() ?: item.averageCost
                    onConfirm(
                        ProductionMaterialItem(
                            productionOrderId = orderId,
                            itemId = item.id,
                            itemName = item.name,
                            unit = item.unit,
                            quantityRequired = qty,
                            unitCost = cost,
                            totalCost = qty * cost
                        )
                    )
                },
                enabled = selectedRawItem != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Add to Batch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditCostsDialog(
    order: ProductionOrder,
    onDismiss: () -> Unit,
    onConfirm: (directLabor: Double, overhead: Double) -> Unit
) {
    var laborText by remember { mutableStateOf(order.directLaborCost.toString()) }
    var overheadText by remember { mutableStateOf(order.overheadCost.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Labor & Factory Overhead") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = laborText,
                    onValueChange = { laborText = it },
                    label = { Text("Direct Labor Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = overheadText,
                    onValueChange = { overheadText = it },
                    label = { Text("Factory Machinery & Power Overhead ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val labor = laborText.toDoubleOrNull() ?: 0.0
                    val overhead = overheadText.toDoubleOrNull() ?: 0.0
                    onConfirm(labor, overhead)
                }
            ) {
                Text("Update Costs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
