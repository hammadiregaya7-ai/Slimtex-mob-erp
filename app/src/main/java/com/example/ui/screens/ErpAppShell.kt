package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ErpViewModel

enum class ErpScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        "Dashboard",
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard,
        "nav_dashboard"
    ),
    INVENTORY(
        "Inventory",
        Icons.Filled.Inventory2,
        Icons.Outlined.Inventory2,
        "nav_inventory"
    ),
    PRODUCTION(
        "Production",
        Icons.Filled.PrecisionManufacturing,
        Icons.Outlined.PrecisionManufacturing,
        "nav_production"
    ),
    CLIENTS(
        "Clients",
        Icons.Filled.People,
        Icons.Outlined.People,
        "nav_clients"
    ),
    ACCOUNTING(
        "Accounting",
        Icons.Filled.AccountBalance,
        Icons.Outlined.AccountBalance,
        "nav_accounting"
    )
}

@Composable
fun ErpAppShell(
    viewModel: ErpViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(ErpScreen.DASHBOARD) }
    var showGlobalStockMovementDialog by remember { mutableStateOf(false) }

    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()

    // Handle back button: return to dashboard if on sub-screen
    BackHandler(enabled = currentScreen != ErpScreen.DASHBOARD) {
        currentScreen = ErpScreen.DASHBOARD
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Adaptive Tablet / Expanded Layout (NavigationRail on Left)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("tablet_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(Modifier.height(16.dp))

                    ErpScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            modifier = Modifier.testTag(screen.testTag)
                        )
                    }
                }

                // Main Content Pane
                when (currentScreen) {
                    ErpScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToInventory = { currentScreen = ErpScreen.INVENTORY },
                        onNavigateToProduction = { currentScreen = ErpScreen.PRODUCTION },
                        onNavigateToAccounting = { currentScreen = ErpScreen.ACCOUNTING },
                        onNavigateToClients = { currentScreen = ErpScreen.CLIENTS },
                        onOpenStockMovementDialog = { showGlobalStockMovementDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    ErpScreen.INVENTORY -> InventoryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                    ErpScreen.PRODUCTION -> ProductionScreen(
                        viewModel = viewModel,
                        isExpandedScreen = true,
                        modifier = Modifier.weight(1f)
                    )
                    ErpScreen.CLIENTS -> ClientsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                    ErpScreen.ACCOUNTING -> AccountingScreen(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Mobile / Handheld Layout (Bottom NavigationBar)
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("mobile_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ErpScreen.values().forEach { screen ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title, fontSize = 11.sp) },
                                modifier = Modifier.testTag(screen.testTag)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                when (currentScreen) {
                    ErpScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToInventory = { currentScreen = ErpScreen.INVENTORY },
                        onNavigateToProduction = { currentScreen = ErpScreen.PRODUCTION },
                        onNavigateToAccounting = { currentScreen = ErpScreen.ACCOUNTING },
                        onNavigateToClients = { currentScreen = ErpScreen.CLIENTS },
                        onOpenStockMovementDialog = { showGlobalStockMovementDialog = true },
                        modifier = Modifier.padding(paddingValues)
                    )
                    ErpScreen.INVENTORY -> InventoryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                    ErpScreen.PRODUCTION -> ProductionScreen(
                        viewModel = viewModel,
                        isExpandedScreen = false,
                        modifier = Modifier.padding(paddingValues)
                    )
                    ErpScreen.CLIENTS -> ClientsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                    ErpScreen.ACCOUNTING -> AccountingScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // Global Stock Movement Dialog
    if (showGlobalStockMovementDialog && inventoryItems.isNotEmpty()) {
        StockMovementDialog(
            item = inventoryItems.first(),
            allItems = inventoryItems,
            onDismiss = { showGlobalStockMovementDialog = false },
            onConfirm = { itemId, delta, cost, type, refNumber, notes ->
                viewModel.recordStockMovement(itemId, delta, cost, type, refNumber, notes)
                showGlobalStockMovementDialog = false
            }
        )
    }
}
