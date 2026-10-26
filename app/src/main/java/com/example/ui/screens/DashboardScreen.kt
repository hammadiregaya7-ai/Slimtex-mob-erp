package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AccountType
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import com.example.data.entity.ItemCategory
import com.example.data.entity.ProductionStatus
import com.example.data.entity.RollQualityGrade
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SlateNavyPrimary
import com.example.ui.theme.SlateNavySecondary
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
    onNavigateToClients: () -> Unit = {},
    onNavigateToAccounting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val orders by viewModel.productionOrders.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val entries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val commercialOrders by viewModel.commercialOrders.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val fabricRolls by viewModel.fabricRollLots.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    // Key Financial & Operational Metrics
    val totalStockValuation = items.sumOf { it.currentStock * it.averageCost }
    val lowStockItems = items.filter { it.isLowStock }
    val activeBatches = orders.filter { it.status == ProductionStatus.IN_PROGRESS || it.status == ProductionStatus.PLANNED }
    val completedBatches = orders.filter { it.status == ProductionStatus.COMPLETED }

    val cashBalance = accounts.find { it.accountCode == "1010" }?.currentBalance ?: 0.0
    val receivablesBalance = accounts.find { it.accountCode == "1100" }?.currentBalance ?: 0.0
    val revenueBalance = accounts.find { it.accountCode == "4000" }?.currentBalance ?: 0.0
    val cogsBalance = accounts.find { it.accountCode == "5000" }?.currentBalance ?: 0.0
    val payablesBalance = accounts.find { it.accountCode == "2000" }?.currentBalance ?: 0.0
    val totalAssets = accounts.filter { it.type == AccountType.ASSET }.sumOf { it.currentBalance }
    val totalLiabilities = accounts.filter { it.type == AccountType.LIABILITY }.sumOf { it.currentBalance }
    val netWorth = totalAssets - totalLiabilities
    val grossProfit = revenueBalance - cogsBalance
    val grossMarginPercent = if (revenueBalance > 0) (grossProfit / revenueBalance) * 100.0 else 0.0

    val clientsWithBalance = customers.filter { it.currentBalance > 0.01 }
    val totalCustomerCreditLimit = customers.sumOf { it.creditLimit }

    // Valuation by Category
    val rawMaterialValue = items.filter { it.category == ItemCategory.RAW_MATERIAL }.sumOf { it.currentStock * it.averageCost }
    val trimValue = items.filter { it.category == ItemCategory.TRIM || it.category == ItemCategory.DYE_CHEMICAL }.sumOf { it.currentStock * it.averageCost }
    val finishedGoodsValue = items.filter { it.category == ItemCategory.FINISHED_GOOD }.sumOf { it.currentStock * it.averageCost }

    val pendingApprovalOrders = commercialOrders.filter { it.status == CommercialStatus.PENDING_APPROVAL }
    val openQuotations = commercialOrders.filter { it.type == CommercialType.SALES_QUOTATION }
    val quarantinedRolls = fabricRolls.filter { it.qualityGrade == RollQualityGrade.QUARANTINED }

    val convertedRevenue = if (selectedCurrency.rateToUsd > 0) revenueBalance / selectedCurrency.rateToUsd else revenueBalance
    val convertedNetWorth = if (selectedCurrency.rateToUsd > 0) netWorth / selectedCurrency.rateToUsd else netWorth

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Executive Textile Atelier Hero Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SlateNavyPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF132A3E),
                                    Color(0xFF064E3B)
                                )
                            )
                        )
                ) {
                    // Decorative Architectural Textile Loom Geometric Canvas Overlay
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                    ) {
                        val step = 36.dp.toPx()
                        var x = size.width * 0.45f
                        while (x < size.width + size.height) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.04f),
                                start = Offset(x, 0f),
                                end = Offset(x - size.height * 0.6f, size.height),
                                strokeWidth = 1.5f
                            )
                            x += step
                        }
                        drawCircle(
                            color = EmeraldAccent.copy(alpha = 0.12f),
                            radius = size.height * 0.65f,
                            center = Offset(size.width * 0.88f, size.height * 0.25f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = "EXECUTIVE COMMAND • ${activeRole.displayName.uppercase()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "TexERP Operations Hub",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Real-time Inventory Valuation, BOM Costing & Double-Entry GL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        // Live Net Worth, Gross Margin, & Working Capital Hero Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Enterprise Net Worth",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = currencyFormat.format(netWorth),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                if (selectedCurrency.code != "USD") {
                                    Text(
                                        text = "≈ ${selectedCurrency.symbol}${String.format(Locale.US, "%,.0f", convertedNetWorth)} ${selectedCurrency.code}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 10.sp,
                                        color = EmeraldAccent
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Gross Margin",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", grossMarginPercent)}%",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = EmeraldAccent
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Active Mills & Rolls",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${suppliers.size} Mills • ${fabricRolls.size} Lots",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFF8FAFC)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Role-Based Approval Gate & Low-Stock Alert Banner (if any)
        if (pendingApprovalOrders.isNotEmpty()) {
            item {
                val topPending = pendingApprovalOrders.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = CrimsonLight.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("approval_gate_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = CrimsonDanger.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Approval Gate",
                                        tint = CrimsonDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${pendingApprovalOrders.size} High-Value Order(s) Awaiting Approval Gate",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "${topPending.orderNumber} (${topPending.partyName}) • ${currencyFormat.format(topPending.totalAmount)} • Active Role: ${activeRole.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToAccounting,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Inspect Order", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.approveCommercialOrder(topPending.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeRole.canApproveHighValuePo) CrimsonDanger else AmberDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_dashboard_approve_order")
                            ) {
                                Text(
                                    text = if (activeRole.canApproveHighValuePo) "Approve & Post GL" else "Requires Controller Role",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 2x2 Executive KPI Grid with Crisp Borders & Accent Badges
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiSummaryCard(
                        title = "Inventory Valuation",
                        value = currencyFormat.format(totalStockValuation),
                        subtitle = "${items.size} SKUs • ${lowStockItems.size} Low Stock",
                        icon = Icons.Default.Inventory2,
                        accentColor = IndigoAccent,
                        accentBg = IndigoLight,
                        onClick = onNavigateToInventory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_inventory_value")
                    )

                    KpiSummaryCard(
                        title = "Work in Progress",
                        value = "${activeBatches.size} Batches",
                        subtitle = "${completedBatches.size} Runs Completed",
                        icon = Icons.Default.PrecisionManufacturing,
                        accentColor = AmberDark,
                        accentBg = AmberLight,
                        onClick = onNavigateToProduction,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_active_batches")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiSummaryCard(
                        title = "Clients & Due AR",
                        value = currencyFormat.format(receivablesBalance),
                        subtitle = "${customers.size} Clients (${clientsWithBalance.size} Due)",
                        icon = Icons.Default.People,
                        accentColor = Color(0xFF7C3AED),
                        accentBg = Color(0xFFEDE9FE),
                        onClick = onNavigateToClients,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_clients_ar")
                    )

                    KpiSummaryCard(
                        title = "Revenue (${selectedCurrency.code})",
                        value = "${selectedCurrency.symbol}${String.format(Locale.US, "%,.0f", convertedRevenue)}",
                        subtitle = "Cash: ${currencyFormat.format(cashBalance)}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = EmeraldDark,
                        accentBg = EmeraldLight,
                        onClick = onNavigateToAccounting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_revenue")
                    )
                }
            }
        }

        // 4. Interactive Visual Analytics: Revenue vs. COGS & Gross Profit Trend + Stock Health
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interactive_analytics_chart_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(16.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Financial & Stock Trend Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Revenue vs. Manufacturing COGS & Gross Profit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = EmeraldLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${openQuotations.size} Quotes • ${quarantinedRolls.size} Quarantined",
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Custom Canvas Multi-Bar + Trend Line Chart
                    val months = listOf("May", "Jun", "Jul", "Aug", "Sep", "Current")
                    val revSeries = listOf(
                        (revenueBalance * 0.58).coerceAtLeast(18000.0),
                        (revenueBalance * 0.67).coerceAtLeast(22000.0),
                        (revenueBalance * 0.74).coerceAtLeast(25000.0),
                        (revenueBalance * 0.85).coerceAtLeast(29000.0),
                        (revenueBalance * 0.92).coerceAtLeast(33000.0),
                        revenueBalance.coerceAtLeast(36000.0)
                    )
                    val cogsSeries = listOf(
                        (cogsBalance * 0.60).coerceAtLeast(9500.0),
                        (cogsBalance * 0.68).coerceAtLeast(11200.0),
                        (cogsBalance * 0.72).coerceAtLeast(12400.0),
                        (cogsBalance * 0.81).coerceAtLeast(14500.0),
                        (cogsBalance * 0.89).coerceAtLeast(16100.0),
                        cogsBalance.coerceAtLeast(17500.0)
                    )
                    val maxChartVal = (revSeries.maxOrNull() ?: 50000.0).coerceAtLeast(1.0)

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val count = revSeries.size
                                    val groupWidth = size.width / count
                                    val barWidth = (groupWidth * 0.26f).coerceAtLeast(8f)
                                    val chartHeight = size.height - 14f

                                    // Subtle horizontal grid lines
                                    for (i in 1..3) {
                                        val y = chartHeight * (i / 4f)
                                        drawLine(
                                            color = Color(0xFF94A3B8).copy(alpha = 0.2f),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1f
                                        )
                                    }

                                    val profitPath = Path()

                                    for (i in 0 until count) {
                                        val centerX = i * groupWidth + groupWidth / 2f
                                        val revH = ((revSeries[i] / maxChartVal).toFloat() * (chartHeight - 16f)).coerceAtLeast(8f)
                                        val cogsH = ((cogsSeries[i] / maxChartVal).toFloat() * (chartHeight - 16f)).coerceAtLeast(6f)

                                        // Revenue bar (Emerald)
                                        drawRoundRect(
                                            color = Color(0xFF059669),
                                            topLeft = Offset(centerX - barWidth - 2f, chartHeight - revH),
                                            size = androidx.compose.ui.geometry.Size(barWidth, revH),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                        )

                                        // COGS bar (Warm Amber)
                                        drawRoundRect(
                                            color = Color(0xFFD97706),
                                            topLeft = Offset(centerX + 2f, chartHeight - cogsH),
                                            size = androidx.compose.ui.geometry.Size(barWidth, cogsH),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                        )

                                        // Gross Profit trend line point
                                        val profitVal = (revSeries[i] - cogsSeries[i]).coerceAtLeast(0.0)
                                        val profitY = chartHeight - ((profitVal / maxChartVal).toFloat() * (chartHeight - 16f))
                                        if (i == 0) {
                                            profitPath.moveTo(centerX, profitY)
                                        } else {
                                            profitPath.lineTo(centerX, profitY)
                                        }
                                        drawCircle(
                                            color = Color(0xFF4F46E5),
                                            radius = 4.5f,
                                            center = Offset(centerX, profitY)
                                        )
                                    }

                                    drawPath(
                                        path = profitPath,
                                        color = Color(0xFF4F46E5),
                                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                months.forEach { m ->
                                    Text(
                                        text = m,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = Color(0xFF059669), label = "Revenue (${currencyFormat.format(revenueBalance)})")
                        LegendDot(color = Color(0xFFD97706), label = "COGS (${currencyFormat.format(cogsBalance)})")
                        LegendDot(color = Color(0xFF4F46E5), label = "Profit (${currencyFormat.format(grossProfit)})")
                    }
                }
            }
        }

        // 5. Inventory Valuation Breakdown & Financial Health Gauges
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(16.dp)
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
                        Column {
                            Text(
                                text = "Stock Composition & Liquidity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Capital allocation across fabrics, trims & finished apparel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = EmeraldLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Margin: ${String.format(Locale.US, "%.1f", grossMarginPercent)}%",
                                color = EmeraldDark,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Stacked visual bar
                    val safeTotal = totalStockValuation.coerceAtLeast(1.0)
                    val rawWeight = (rawMaterialValue / safeTotal).toFloat().coerceIn(0.05f, 1f)
                    val trimWeight = (trimValue / safeTotal).toFloat().coerceIn(0.05f, 1f)
                    val fgWeight = (finishedGoodsValue / safeTotal).toFloat().coerceIn(0.05f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(rawWeight)
                                .fillMaxHeight()
                                .background(IndigoAccent)
                        )
                        Box(
                            modifier = Modifier
                                .weight(trimWeight)
                                .fillMaxHeight()
                                .background(AmberWarning)
                        )
                        Box(
                            modifier = Modifier
                                .weight(fgWeight)
                                .fillMaxHeight()
                                .background(EmeraldAccent)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendDot(
                            color = IndigoAccent,
                            label = "Fabrics: ${currencyFormat.format(rawMaterialValue)}"
                        )
                        LegendDot(
                            color = AmberWarning,
                            label = "Trims/Dyes: ${currencyFormat.format(trimValue)}"
                        )
                        LegendDot(
                            color = EmeraldAccent,
                            label = "Apparel: ${currencyFormat.format(finishedGoodsValue)}"
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Working Capital Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FinancialMiniMetric(
                            label = "Cash on Hand",
                            amount = currencyFormat.format(cashBalance),
                            valueColor = EmeraldDark
                        )
                        FinancialMiniMetric(
                            label = "Receivables (AR)",
                            amount = currencyFormat.format(receivablesBalance),
                            valueColor = IndigoAccent
                        )
                        FinancialMiniMetric(
                            label = "Payables (AP)",
                            amount = currencyFormat.format(payablesBalance),
                            valueColor = CrimsonDanger
                        )
                        FinancialMiniMetric(
                            label = "Credit Facility",
                            amount = currencyFormat.format(totalCustomerCreditLimit),
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 6. Low Stock Alerts Section
        item {
            SectionCard(
                title = "Warehouse Reorder Alerts",
                actionLabel = "Manage Stock",
                onActionClick = onNavigateToInventory
            ) {
                if (lowStockItems.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "All textile materials and trims are above reorder thresholds.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        lowStockItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberLight.copy(alpha = 0.65f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Low Stock",
                                        tint = AmberDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${item.sku} • ${item.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF78350F)
                                        )
                                        Text(
                                            text = "Bin: ${item.storageLocation} • Reorder Point: ${item.reorderLevel} ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AmberDark
                                        )
                                    }
                                }

                                Surface(
                                    color = AmberDark,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${item.currentStock} ${item.unit}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Active Production Cost Sheets Summary
        item {
            SectionCard(
                title = "Active Manufacturing Batches",
                actionLabel = "Cost Sheets",
                onActionClick = onNavigateToProduction
            ) {
                if (orders.isEmpty()) {
                    Text(
                        text = "No production batches scheduled.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        orders.take(3).forEach { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable {
                                        viewModel.selectProductionOrder(order.id)
                                        onNavigateToProduction()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = order.batchNumber,
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        StatusBadge(status = order.status)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${order.finishedItemName} • Target: ${order.targetQuantity.toInt()} pcs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${currencyFormat.format(order.unitProductionCost)} / unit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "Total: ${currencyFormat.format(order.totalBatchCost)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Recent Double-Entry Journal Postings
        item {
            SectionCard(
                title = "Recent General Ledger Postings",
                actionLabel = "Full Ledger",
                onActionClick = onNavigateToAccounting
            ) {
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.take(4).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = entry.entryNumber,
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = entry.sourceReference.ifBlank { "Manual Entry" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = entry.memo,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencyFormat.format(entry.totalAmount),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = dateFormat.format(Date(entry.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    accentBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onActionClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, color = EmeraldDark)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun StatusBadge(status: ProductionStatus) {
    val (bgColor, textColor, label) = when (status) {
        ProductionStatus.PLANNED -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "PLANNED")
        ProductionStatus.IN_PROGRESS -> Triple(AmberLight, AmberDark, "IN PROGRESS")
        ProductionStatus.COMPLETED -> Triple(EmeraldLight, EmeraldDark, "COMPLETED")
        ProductionStatus.CANCELLED -> Triple(CrimsonLight, CrimsonDanger, "CANCELLED")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FinancialMiniMetric(
    label: String,
    amount: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
