package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CommercialStatus
import com.example.data.entity.ErpCurrency
import com.example.data.entity.ErpRole
import com.example.data.entity.FabricRollLot
import com.example.data.entity.InventoryItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.Supplier
import com.example.data.entity.TransactionType
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SlateNavyPrimary
import com.example.ui.theme.SlateNavySecondary
import com.example.ui.viewmodel.ErpViewModel

enum class ErpDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    INVENTORY("inventory", "Inventory", Icons.Default.Inventory2, "nav_inventory"),
    PRODUCTION("production", "Production", Icons.Default.PrecisionManufacturing, "nav_production"),
    CLIENTS("clients", "Clients", Icons.Default.People, "nav_clients"),
    ACCOUNTING("accounting", "Accounting", Icons.Default.AccountBalance, "nav_accounting")
}

data class AwesomebarResult(
    val category: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val destination: ErpDestination,
    val actionHint: String
)

@Composable
fun ErpAppShell(
    viewModel: ErpViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(ErpDestination.DASHBOARD) }
    var showAwesomebarModal by remember { mutableStateOf(false) }
    var showBarcodeScannerModal by remember { mutableStateOf(false) }

    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val orders by viewModel.productionOrders.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val commercialOrders by viewModel.commercialOrders.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val fabricRolls by viewModel.fabricRollLots.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    val lowStockCount = items.count { it.isLowStock }
    val pendingApprovalCount = commercialOrders.count { it.status == CommercialStatus.PENDING_APPROVAL }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        val isMediumOrExpanded = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                ErpNextAwesomebarBar(
                    currentDestination = currentDestination,
                    activeRole = activeRole,
                    selectedCurrency = selectedCurrency,
                    pendingApprovalCount = pendingApprovalCount,
                    onCycleRole = {
                        val nextRole = ErpRole.values()[(activeRole.ordinal + 1) % ErpRole.values().size]
                        viewModel.setActiveRole(nextRole)
                    },
                    onCycleCurrency = {
                        val nextCur = ErpCurrency.values()[(selectedCurrency.ordinal + 1) % ErpCurrency.values().size]
                        viewModel.setSelectedCurrency(nextCur)
                    },
                    onOpenAwesomebar = { showAwesomebarModal = true },
                    onOpenBarcodeScanner = { showBarcodeScannerModal = true }
                )
            },
            bottomBar = {
                if (!isMediumOrExpanded) {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 10.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                ErpDestination.values().forEach { dest ->
                                    val selected = currentDestination == dest
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentDestination = dest },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = EmeraldDark,
                                            selectedTextColor = EmeraldDark,
                                            indicatorColor = EmeraldLight.copy(alpha = 0.75f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        icon = {
                                            if (dest == ErpDestination.INVENTORY && lowStockCount > 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = AmberDark,
                                                            contentColor = Color.White
                                                        ) {
                                                            Text("$lowStockCount")
                                                        }
                                                    }
                                                ) {
                                                    Icon(dest.icon, contentDescription = dest.label)
                                                }
                                            } else if (dest == ErpDestination.ACCOUNTING && pendingApprovalCount > 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = Color(0xFFDC2626),
                                                            contentColor = Color.White
                                                        ) {
                                                            Text("$pendingApprovalCount")
                                                        }
                                                    }
                                                ) {
                                                    Icon(dest.icon, contentDescription = dest.label)
                                                }
                                            } else {
                                                Icon(dest.icon, contentDescription = dest.label)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = dest.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isMediumOrExpanded) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_nav_rail"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SlateNavyPrimary, EmeraldDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TX",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    ) {
                        Spacer(Modifier.height(8.dp))
                        ErpDestination.values().forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    if (dest == ErpDestination.INVENTORY && lowStockCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = AmberDark, contentColor = Color.White) {
                                                    Text("$lowStockCount")
                                                }
                                            }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                // Active Workspace Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentDestination) {
                        ErpDestination.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToInventory = { currentDestination = ErpDestination.INVENTORY },
                            onNavigateToProduction = { currentDestination = ErpDestination.PRODUCTION },
                            onNavigateToClients = { currentDestination = ErpDestination.CLIENTS },
                            onNavigateToAccounting = { currentDestination = ErpDestination.ACCOUNTING }
                        )
                        ErpDestination.INVENTORY -> InventoryScreen(viewModel = viewModel)
                        ErpDestination.PRODUCTION -> ProductionScreen(
                            viewModel = viewModel,
                            isExpandedScreen = isExpanded
                        )
                        ErpDestination.CLIENTS -> ClientsScreen(viewModel = viewModel)
                        ErpDestination.ACCOUNTING -> AccountingScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showAwesomebarModal) {
        ErpNextAwesomebarDialog(
            items = items,
            orders = orders,
            customers = customers,
            commercialOrders = commercialOrders,
            accounts = accounts,
            onDismiss = { showAwesomebarModal = false },
            onSelectDestination = { dest ->
                currentDestination = dest
                showAwesomebarModal = false
            }
        )
    }

    if (showBarcodeScannerModal) {
        BarcodeQrScannerLookupDialog(
            viewModel = viewModel,
            items = items,
            fabricRolls = fabricRolls,
            productionOrders = orders,
            suppliers = suppliers,
            onDismiss = { showBarcodeScannerModal = false },
            onNavigateToDestination = { dest ->
                currentDestination = dest
                showBarcodeScannerModal = false
            }
        )
    }
}

@Composable
fun ErpNextAwesomebarBar(
    currentDestination: ErpDestination,
    activeRole: ErpRole,
    selectedCurrency: ErpCurrency,
    pendingApprovalCount: Int,
    onCycleRole: () -> Unit,
    onCycleCurrency: () -> Unit,
    onOpenAwesomebar: () -> Unit,
    onOpenBarcodeScanner: () -> Unit
) {
    Surface(
        color = SlateNavyPrimary,
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(SlateNavyPrimary, SlateNavySecondary)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Brand Monogram + Module Breadcrumb + Operational Pills (Role, Currency, Scanner)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldAccent, EmeraldDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TX",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TexERP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "  /  ${currentDestination.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Textile Manufacturing & Ledger Suite",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Controls: Role Switcher, Multi-Currency Pill, Barcode/QR Scanner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Role Switcher Chip
                    val isApprover = activeRole.canApproveHighValuePo
                    Surface(
                        color = if (isApprover) EmeraldDark.copy(alpha = 0.35f) else Color(0xFF334155),
                        border = BorderStroke(
                            1.dp,
                            if (isApprover) EmeraldAccent.copy(alpha = 0.6f) else Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCycleRole() }
                            .testTag("role_switcher_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = "Active Role",
                                modifier = Modifier.size(13.dp),
                                tint = if (isApprover) EmeraldAccent else AmberLight
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = activeRole.badgeCode,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (pendingApprovalCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }
                    }

                    // Currency Switcher Chip
                    Surface(
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCycleCurrency() }
                            .testTag("currency_switcher_chip")
                    ) {
                        Text(
                            text = "${selectedCurrency.symbol} ${selectedCurrency.code}",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // Barcode / QR Scanner Lookup Button
                    Surface(
                        color = EmeraldDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenBarcodeScanner() }
                            .testTag("btn_barcode_qr_scanner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Barcode & QR Scanner",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Scan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Second Row: Full-Width Tactile ERPNext Awesomebar Search & Command Bar
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenAwesomebar() }
                    .testTag("erpnext_awesomebar_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Awesomebar Search",
                            modifier = Modifier.size(16.dp),
                            tint = EmeraldAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Search SKUs, dye lots, work orders, clients, or commands...",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "⌘K",
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeQrScannerLookupDialog(
    viewModel: ErpViewModel,
    items: List<InventoryItem>,
    fabricRolls: List<FabricRollLot>,
    productionOrders: List<ProductionOrder>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onNavigateToDestination: (ErpDestination) -> Unit
) {
    var scannedCode by remember { mutableStateOf("") }

    val quickSampleTags = remember(items, fabricRolls, productionOrders, suppliers) {
        buildList {
            items.firstOrNull()?.let { add(it.sku to "SKU: ${it.sku}") }
            fabricRolls.firstOrNull()?.let { add(it.rollNumber to "Roll: ${it.rollNumber}") }
            fabricRolls.getOrNull(1)?.let { add(it.dyeLotCode to "Dye Lot: ${it.dyeLotCode}") }
            productionOrders.firstOrNull()?.let { add(it.batchNumber to "Batch: ${it.batchNumber}") }
            suppliers.firstOrNull()?.let { add(it.code to "Mill: ${it.code}") }
        }
    }

    val matchedItem = remember(scannedCode, items) {
        val q = scannedCode.trim()
        if (q.isEmpty()) null else items.find {
            it.sku.equals(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
        }
    }
    val matchedRoll = remember(scannedCode, fabricRolls) {
        val q = scannedCode.trim()
        if (q.isEmpty()) null else fabricRolls.find {
            it.rollNumber.equals(q, ignoreCase = true) || it.dyeLotCode.equals(q, ignoreCase = true)
        }
    }
    val matchedBatch = remember(scannedCode, productionOrders) {
        val q = scannedCode.trim()
        if (q.isEmpty()) null else productionOrders.find {
            it.batchNumber.equals(q, ignoreCase = true)
        }
    }
    val matchedSupplier = remember(scannedCode, suppliers) {
        val q = scannedCode.trim()
        if (q.isEmpty()) null else suppliers.find {
            it.code.equals(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Warehouse Barcode & QR Lookup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("barcode_scanner_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Scan or enter any SKU barcode, Fabric Roll QR tag, Dye-Lot number, or Production Batch ID:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = scannedCode,
                    onValueChange = { scannedCode = it },
                    placeholder = { Text("e.g. FAB-101, ROLL-2026-01, LOT-IND-88A...") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    trailingIcon = {
                        if (scannedCode.isNotEmpty()) {
                            IconButton(onClick = { scannedCode = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("barcode_scanner_input")
                )

                Text(
                    text = "Quick Warehouse Scan Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickSampleTags) { (code, label) ->
                        FilterChip(
                            selected = scannedCode.equals(code, ignoreCase = true),
                            onClick = { scannedCode = code },
                            label = { Text(label, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily) },
                            modifier = Modifier.testTag("preset_scan_$code")
                        )
                    }
                }

                when {
                    matchedItem != null -> {
                        val item = matchedItem
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_result_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("MATCHED INVENTORY SKU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                Text("${item.sku} • ${item.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "On Hand: ${item.currentStock} ${item.unit} • Bin: ${item.storageLocation} • Cost: $${item.averageCost}",
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.recordStockMovement(
                                                itemId = item.id,
                                                delta = 25.0,
                                                unitCost = item.averageCost,
                                                type = TransactionType.PURCHASE_RECEIPT,
                                                refNumber = "SCAN-IN-${System.currentTimeMillis() % 1000}",
                                                notes = "Barcode scan quick receive +25 ${item.unit}"
                                            )
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("btn_scan_quick_stock_in")
                                    ) {
                                        Text("+25 Quick Receive", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onNavigateToDestination(ErpDestination.INVENTORY) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Open in Inventory", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    matchedRoll != null -> {
                        val roll = matchedRoll
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_result_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("MATCHED FABRIC ROLL & DYE LOT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                Text("${roll.rollNumber} (Dye Lot: ${roll.dyeLotCode})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("${roll.itemName} • Mill: ${roll.supplierName}", fontSize = 12.sp)
                                Text(
                                    "Shade: ${roll.shadeBand} • Length: ${roll.lengthMeters}m • 4-Pt Score: ${roll.defectPointsPer100SqYds} pts (${roll.qualityGrade.displayName})",
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Button(
                                    onClick = { onNavigateToDestination(ErpDestination.INVENTORY) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .height(32.dp)
                                ) {
                                    Text("Inspect Roll QC", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    matchedBatch != null -> {
                        val order = matchedBatch
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_result_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("MATCHED WORK ORDER BATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                Text("${order.batchNumber} • ${order.finishedItemName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Target: ${order.targetQuantity.toInt()} units • Status: ${order.status} • Unit Cost: $${String.format("%.2f", order.unitProductionCost)}",
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Button(
                                    onClick = {
                                        viewModel.selectProductionOrder(order.id)
                                        onNavigateToDestination(ErpDestination.PRODUCTION)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .height(32.dp)
                                ) {
                                    Text("Open Cost Sheet", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    matchedSupplier != null -> {
                        val sup = matchedSupplier
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_result_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("MATCHED SUPPLIER MILL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                Text("${sup.code} • ${sup.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${sup.specialty.displayName} • Lead Time: ${sup.leadTimeDays}d • Currency: ${sup.currencyCode}",
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFamily
                                )
                            }
                        }
                    }
                    scannedCode.isNotBlank() -> {
                        Surface(
                            color = AmberLight,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberDark.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No SKU, Fabric Roll, Dye Lot, or Batch matched \"${scannedCode.trim()}\".",
                                color = AmberDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ErpNextAwesomebarDialog(
    items: List<InventoryItem>,
    orders: List<ProductionOrder>,
    customers: List<com.example.data.entity.Customer>,
    commercialOrders: List<com.example.data.entity.CommercialOrder>,
    accounts: List<com.example.data.entity.Account>,
    onDismiss: () -> Unit,
    onSelectDestination: (ErpDestination) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val allRecords = remember(items, orders, customers, commercialOrders, accounts) {
        val list = mutableListOf<AwesomebarResult>()

        list.add(
            AwesomebarResult(
                "Action",
                "CMD-BOM",
                "New Production Work Order (BOM)",
                "Launch multi-level Bill of Materials batch",
                ErpDestination.PRODUCTION,
                "Open Production"
            )
        )
        list.add(
            AwesomebarResult(
                "Action",
                "CMD-PL",
                "Financial Statements (P&L & Balance Sheet)",
                "View real-time Profit & Loss and AR Aging",
                ErpDestination.ACCOUNTING,
                "Open Accounting"
            )
        )
        list.add(
            AwesomebarResult(
                "Action",
                "CMD-VAR",
                "Apparel Item Variant Matrix (Size × Shade)",
                "Generate SKU variants across sizes & colors",
                ErpDestination.INVENTORY,
                "Open Inventory"
            )
        )

        items.forEach { item ->
            list.add(
                AwesomebarResult(
                    category = "Stock SKU",
                    code = item.sku,
                    title = item.name,
                    subtitle = "${item.currentStock} ${item.unit} on hand • Bin: ${item.storageLocation}",
                    destination = ErpDestination.INVENTORY,
                    actionHint = "Inspect SKU"
                )
            )
        }
        orders.forEach { order ->
            list.add(
                AwesomebarResult(
                    category = "Work Order",
                    code = order.batchNumber,
                    title = order.finishedItemName,
                    subtitle = "Status: ${order.status} • Target: ${order.targetQuantity.toInt()} units",
                    destination = ErpDestination.PRODUCTION,
                    actionHint = "Cost Sheet"
                )
            )
        }
        customers.forEach { cust ->
            list.add(
                AwesomebarResult(
                    category = "Client",
                    code = cust.code,
                    title = cust.name,
                    subtitle = "${cust.accountType} • Balance Due: $${String.format("%.2f", cust.currentBalance)}",
                    destination = ErpDestination.CLIENTS,
                    actionHint = "Client Dossier"
                )
            )
        }
        commercialOrders.forEach { doc ->
            list.add(
                AwesomebarResult(
                    category = if (doc.type == com.example.data.entity.CommercialType.COMMERCIAL_INVOICE) "Sales Invoice" else "Purchase Order",
                    code = doc.orderNumber,
                    title = doc.partyName,
                    subtitle = "Total: $${String.format("%.2f", doc.totalAmount)} • Status: ${doc.status}",
                    destination = ErpDestination.ACCOUNTING,
                    actionHint = "Open Ledger"
                )
            )
        }
        accounts.forEach { acc ->
            list.add(
                AwesomebarResult(
                    category = "GL Account",
                    code = acc.accountCode,
                    title = acc.accountName,
                    subtitle = "${acc.type} • Balance: $${String.format("%.2f", acc.currentBalance)}",
                    destination = ErpDestination.ACCOUNTING,
                    actionHint = "Chart of Accounts"
                )
            )
        }
        list
    }

    val filtered = remember(query, allRecords) {
        if (query.isBlank()) {
            allRecords.take(12)
        } else {
            allRecords.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.code.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true)
            }.take(15)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ERPNext Awesomebar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instant global search across SKUs, Batches, Clients, Invoices & GL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Type SKU, batch #, client name, or command...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("awesomebar_search_input")
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { "${it.category}_${it.code}" }) { res ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDestination(res.destination) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = EmeraldLight,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = res.category,
                                                color = EmeraldDark,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = res.code,
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = res.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = res.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${res.actionHint} →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
