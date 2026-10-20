package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.StockTransaction
import com.example.data.entity.TransactionType
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.ErpViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ErpViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToProduction: () -> Unit,
    onNavigateToAccounting: () -> Unit,
    onOpenStockMovementDialog: () -> Unit,
    onNavigateToClients: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockItems.collectAsStateWithLifecycle()
    val transactions by viewModel.stockTransactions.collectAsStateWithLifecycle()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    var transactionFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedTransactionToInspect by remember { mutableStateOf<StockTransaction?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        val isWideLayout = maxWidth >= 760.dp

        if (isWideLayout) {
            // Tablet / Two-Column Responsive Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: KPIs, Inventory Health Gauges & Material Levels
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DashboardHeader(onOpenStockMovementDialog = onOpenStockMovementDialog)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            DashboardKpiSection(
                                metrics = metrics,
                                currencyFormat = currencyFormat,
                                onNavigateToInventory = onNavigateToInventory,
                                onNavigateToAccounting = onNavigateToAccounting,
                                onNavigateToClients = onNavigateToClients
                            )
                        }

                        item {
                            ExecutiveFinancialPulseCard(
                                metrics = metrics,
                                currencyFormat = currencyFormat,
                                onNavigateToClients = onNavigateToClients,
                                onNavigateToProduction = onNavigateToProduction,
                                onNavigateToAccounting = onNavigateToAccounting
                            )
                        }

                        item {
                            InventoryHealthOverviewCard(
                                items = items,
                                lowStockItems = lowStockItems,
                                currencyFormat = currencyFormat,
                                onNavigateToInventory = onNavigateToInventory
                            )
                        }

                        item {
                            InventoryLevelMonitorCard(
                                items = items,
                                onNavigateToInventory = onNavigateToInventory
                            )
                        }
                    }
                }

                // Right Column: Recent Transactions Feed & Quick Audit Log
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TransactionsSectionHeader(
                        transactionCount = transactions.size,
                        selectedFilter = transactionFilter,
                        onFilterChange = { transactionFilter = it },
                        onNavigateToAccounting = onNavigateToAccounting
                    )

                    val filteredTransactions = transactions.filter {
                        transactionFilter == null || it.type == transactionFilter
                    }

                    if (filteredTransactions.isEmpty()) {
                        EmptyTransactionsCard()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTransactions, key = { it.id }) { tx ->
                                TransactionAuditRow(
                                    tx = tx,
                                    currencyFormat = currencyFormat,
                                    onClick = { selectedTransactionToInspect = tx }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Handheld / Compact Single-Column Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DashboardHeader(onOpenStockMovementDialog = onOpenStockMovementDialog)
                }

                item {
                    DashboardKpiSection(
                        metrics = metrics,
                        currencyFormat = currencyFormat,
                        onNavigateToInventory = onNavigateToInventory,
                        onNavigateToAccounting = onNavigateToAccounting,
                        onNavigateToClients = onNavigateToClients
                    )
                }

                item {
                    ExecutiveFinancialPulseCard(
                        metrics = metrics,
                        currencyFormat = currencyFormat,
                        onNavigateToClients = onNavigateToClients,
                        onNavigateToProduction = onNavigateToProduction,
                        onNavigateToAccounting = onNavigateToAccounting
                    )
                }

                if (lowStockItems.isNotEmpty()) {
                    item {
                        LowStockAlertBanner(
                            lowStockItems = lowStockItems,
                            onNavigateToInventory = onNavigateToInventory
                        )
                    }
                }

                item {
                    InventoryHealthOverviewCard(
                        items = items,
                        lowStockItems = lowStockItems,
                        currencyFormat = currencyFormat,
                        onNavigateToInventory = onNavigateToInventory
                    )
                }

                item {
                    InventoryLevelMonitorCard(
                        items = items,
                        onNavigateToInventory = onNavigateToInventory
                    )
                }

                item {
                    TransactionsSectionHeader(
                        transactionCount = transactions.size,
                        selectedFilter = transactionFilter,
                        onFilterChange = { transactionFilter = it },
                        onNavigateToAccounting = onNavigateToAccounting
                    )
                }

                val filteredTransactions = transactions.filter {
                    transactionFilter == null || it.type == transactionFilter
                }

                if (filteredTransactions.isEmpty()) {
                    item { EmptyTransactionsCard() }
                } else {
                    items(filteredTransactions.take(8), key = { it.id }) { tx ->
                        TransactionAuditRow(
                            tx = tx,
                            currencyFormat = currencyFormat,
                            onClick = { selectedTransactionToInspect = tx }
                        )
                    }
                }
            }
        }
    }

    // Transaction Details Inspection Dialog
    if (selectedTransactionToInspect != null) {
        TransactionInspectDialog(
            tx = selectedTransactionToInspect!!,
            currencyFormat = currencyFormat,
            onDismiss = { selectedTransactionToInspect = null }
        )
    }
}

@Composable
fun DashboardHeader(onOpenStockMovementDialog: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Live Textile Inventory & Financial Ledger",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onOpenStockMovementDialog,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_quick_stock_movement")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Stock In/Out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DashboardKpiSection(
    metrics: com.example.ui.viewmodel.DashboardMetrics,
    currencyFormat: NumberFormat,
    onNavigateToInventory: () -> Unit = {},
    onNavigateToAccounting: () -> Unit = {},
    onNavigateToClients: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Total Inventory Value",
                value = currencyFormat.format(metrics.totalInventoryValuation),
                subtitle = "Raw: ${currencyFormat.format(metrics.rawMaterialValuation)} • FG: ${currencyFormat.format(metrics.finishedGoodsValuation)}",
                icon = Icons.Default.Inventory2,
                iconTint = EmeraldAccent,
                onClick = onNavigateToInventory,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Sales Revenue",
                value = currencyFormat.format(metrics.totalRevenue),
                subtitle = "Wholesale & retail clients",
                icon = Icons.Default.TrendingUp,
                iconTint = Color(0xFF3B82F6),
                onClick = onNavigateToClients,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Accounts Receivable",
                value = currencyFormat.format(metrics.accountsReceivable),
                subtitle = "Pending client payments",
                icon = Icons.Default.ArrowDownward,
                iconTint = Color(0xFF10B981),
                onClick = onNavigateToClients,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Accounts Payable",
                value = currencyFormat.format(metrics.accountsPayable),
                subtitle = "Owed to suppliers & mills",
                icon = Icons.Default.ArrowUpward,
                iconTint = Color(0xFFEF4444),
                onClick = onNavigateToAccounting,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun InventoryHealthOverviewCard(
    items: List<InventoryItem>,
    lowStockItems: List<InventoryItem>,
    currencyFormat: NumberFormat,
    onNavigateToInventory: () -> Unit
) {
    val totalItems = items.size
    val lowCount = lowStockItems.size
    val healthyCount = (totalItems - lowCount).coerceAtLeast(0)
    val healthRatio = if (totalItems > 0) healthyCount.toFloat() / totalItems.toFloat() else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmeraldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Warehouse Stock Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$healthyCount of $totalItems catalog items above reorder point",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (lowCount == 0) EmeraldLight else AmberLight,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (lowCount == 0) "100% HEALTHY" else "$lowCount TO REORDER",
                        color = if (lowCount == 0) EmeraldDark else AmberDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Health Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { healthRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (healthRatio >= 0.8f) EmeraldDark else AmberWarning,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

            // Category breakdown rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val rawMaterials = items.filter { it.category == ItemCategory.RAW_MATERIAL }
                val finishedGoods = items.filter { it.category == ItemCategory.FINISHED_GOOD }
                val trims = items.filter { it.category == ItemCategory.TRIM || it.category == ItemCategory.DYE_CHEMICAL }

                CategoryStatPill(
                    label = "Fabrics & Yarn",
                    count = rawMaterials.size,
                    valuation = currencyFormat.format(rawMaterials.sumOf { it.currentStock * it.averageCost }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                CategoryStatPill(
                    label = "Finished Goods",
                    count = finishedGoods.size,
                    valuation = currencyFormat.format(finishedGoods.sumOf { it.currentStock * it.averageCost }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                CategoryStatPill(
                    label = "Trims & Dyes",
                    count = trims.size,
                    valuation = currencyFormat.format(trims.sumOf { it.currentStock * it.averageCost }),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CategoryStatPill(
    label: String,
    count: Int,
    valuation: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$count items",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valuation,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = EmeraldDark
            )
        }
    }
}

@Composable
fun InventoryLevelMonitorCard(
    items: List<InventoryItem>,
    onNavigateToInventory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Material Stock Levels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToInventory) {
                    Text("View All Inventory", fontSize = 12.sp)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }

            // Key items gauges
            items.take(5).forEach { item ->
                val maxExpected = (item.reorderLevel * 4).coerceAtLeast(item.currentStock * 1.2).coerceAtLeast(1.0)
                val levelProgress = (item.currentStock / maxExpected).toFloat().coerceIn(0f, 1f)
                val isLow = item.currentStock <= item.reorderLevel

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (isLow) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = AmberLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "REORDER",
                                        color = AmberDark,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${item.currentStock.toInt()} / ${item.reorderLevel.toInt()} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isLow) AmberDark else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isLow) AmberWarning else EmeraldDark,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LowStockAlertBanner(
    lowStockItems: List<InventoryItem>,
    onNavigateToInventory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("low_stock_banner"),
        colors = CardDefaults.cardColors(containerColor = AmberLight.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AmberWarning),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Low Stock Warning (${lowStockItems.size} items)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = lowStockItems.joinToString(", ") { "${it.name} (${it.currentStock.toInt()} ${it.unit})" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onNavigateToInventory,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reorder", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TransactionsSectionHeader(
    transactionCount: Int,
    selectedFilter: TransactionType?,
    onFilterChange: (TransactionType?) -> Unit,
    onNavigateToAccounting: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$transactionCount audit records logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onNavigateToAccounting,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Ledger", fontSize = 12.sp)
            }
        }

        // Filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All (${transactionCount})", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionType.PURCHASE_RECEIPT,
                    onClick = {
                        onFilterChange(if (selectedFilter == TransactionType.PURCHASE_RECEIPT) null else TransactionType.PURCHASE_RECEIPT)
                    },
                    label = { Text("Stock In", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionType.SALES_DISPATCH,
                    onClick = {
                        onFilterChange(if (selectedFilter == TransactionType.SALES_DISPATCH) null else TransactionType.SALES_DISPATCH)
                    },
                    label = { Text("Sales Out", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionType.PRODUCTION_CONSUMPTION || selectedFilter == TransactionType.PRODUCTION_OUTPUT,
                    onClick = {
                        onFilterChange(if (selectedFilter == TransactionType.PRODUCTION_OUTPUT) null else TransactionType.PRODUCTION_OUTPUT)
                    },
                    label = { Text("Production", fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
fun TransactionAuditRow(
    tx: StockTransaction,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val isPositive = tx.quantityDelta >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_row_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) EmeraldLight else Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isPositive) EmeraldDark else Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = tx.itemName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tx.type.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        if (tx.referenceNumber.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "• Ref: ${tx.referenceNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}${tx.quantityDelta}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPositive) EmeraldDark else Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = currencyFormat.format(tx.totalCost),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No matching transactions recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransactionInspectDialog(
    tx: StockTransaction,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Transaction Audit Detail")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Item: ${tx.itemName}", fontWeight = FontWeight.Bold)
                Text("Type: ${tx.type.name.replace('_', ' ')}")
                Text("Quantity: ${tx.quantityDelta}")
                Text("Unit Cost: ${currencyFormat.format(tx.unitCost)}")
                Text("Total Transaction Value: ${currencyFormat.format(tx.totalCost)}", fontWeight = FontWeight.SemiBold, color = EmeraldDark)
                Text("Reference: ${tx.referenceNumber}")
                Text("Timestamp: ${dateFormat.format(Date(tx.timestamp))}")
                if (tx.notes.isNotBlank()) {
                    Text("Notes: ${tx.notes}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ExecutiveFinancialPulseCard(
    metrics: com.example.ui.viewmodel.DashboardMetrics,
    currencyFormat: NumberFormat,
    onNavigateToClients: () -> Unit,
    onNavigateToProduction: () -> Unit,
    onNavigateToAccounting: () -> Unit
) {
    val marginFraction = (metrics.profitMarginPercent / 100.0).coerceIn(0.0, 1.0).toFloat()
    val workingCapital = metrics.cashAndBankBalance + metrics.accountsReceivable + metrics.totalInventoryValuation - metrics.accountsPayable

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("executive_financial_pulse_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Profitability & Client Accounts Pulse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time operating P&L, net working capital & client credit status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (metrics.netProfit >= 0) EmeraldLight else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%% Margin", metrics.profitMarginPercent),
                        color = if (metrics.netProfit >= 0) EmeraldDark else Color(0xFFB91C1C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3-Column Financial Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Net Profit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(metrics.netProfit),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (metrics.netProfit >= 0) EmeraldDark else Color(0xFFDC2626)
                        )
                        Text(
                            text = "COGS/Exp: ${currencyFormat.format(metrics.totalExpenses)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Working Capital",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(workingCapital),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Net Liquid + Stock",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToClients() }
                        .testTag("pulse_clients_card"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Client Directory",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${metrics.totalCustomersCount} Clients",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${metrics.customersWithBalanceCount} with open AR →",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = AmberDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Margin bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Operating Profit Ratio (Net Income vs. Gross Revenue)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currencyFormat.format(metrics.netProfit)} / ${currencyFormat.format(metrics.totalRevenue)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { marginFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Quick Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToClients,
                    modifier = Modifier.weight(1f).testTag("btn_quick_clients"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Clients & AR", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onNavigateToProduction,
                    modifier = Modifier.weight(1f).testTag("btn_quick_production"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Cost Sheets (${metrics.activeBatchesCount})", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onNavigateToAccounting,
                    modifier = Modifier.weight(1f).testTag("btn_quick_accounting"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("P&L & Ledger", fontSize = 12.sp)
                }
            }
        }
    }
}

