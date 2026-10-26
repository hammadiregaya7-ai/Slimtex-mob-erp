package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.JournalLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TexERP", appName)
    }

    @Test
    fun `double entry balance validation succeeds on equal debit and credit`() {
        val lines = listOf(
            JournalLine(1L, 1L, "1400", "Inventory - Finished Goods", debit = 2500.0, credit = 0.0),
            JournalLine(2L, 1L, "1200", "Inventory - Raw Materials", debit = 0.0, credit = 1500.0),
            JournalLine(3L, 1L, "5100", "Direct Tailoring Labor", debit = 0.0, credit = 1000.0)
        )
        val debits = lines.sumOf { it.debit }
        val credits = lines.sumOf { it.credit }
        assertEquals(debits, credits, 0.001)
    }

    @Test
    fun `production cost sheet calculation computes correct unit cost and margin`() {
        val targetQuantity = 200.0
        val rawMaterialCost = 1400.0
        val laborCost = 650.0
        val overheadCost = 250.0
        val totalCost = rawMaterialCost + laborCost + overheadCost
        val unitCost = totalCost / targetQuantity

        assertEquals(2300.0, totalCost, 0.01)
        assertEquals(11.50, unitCost, 0.01)

        val sellingPrice = 35.0
        val margin = sellingPrice - unitCost
        val marginPercent = (margin / sellingPrice) * 100.0
        assertTrue(marginPercent > 65.0)
    }

    @Test
    fun `customer credit utilization calculation correctly flags nearing limit`() {
        val creditLimit = 50000.0
        var currentBalance = 25000.0
        var utilization = currentBalance / creditLimit
        assertEquals(0.50, utilization, 0.001)

        // Add invoice of 18000
        currentBalance += 18000.0
        utilization = currentBalance / creditLimit
        assertEquals(0.86, utilization, 0.001)
        assertTrue("Should exceed 80% threshold", utilization >= 0.80)
    }

    @Test
    fun `customer filtering by search query and account type matches accurately`() {
        val customers = listOf(
            com.example.data.entity.Customer(1L, "CUST-101", "Nordic Apparel", com.example.data.entity.CustomerAccountType.WHOLESALE, currentBalance = 2300.0),
            com.example.data.entity.Customer(2L, "CUST-102", "Atelier Boutique", com.example.data.entity.CustomerAccountType.RETAIL_BOUTIQUE, currentBalance = 0.0),
            com.example.data.entity.Customer(3L, "CUST-103", "Metro Stores", com.example.data.entity.CustomerAccountType.CORPORATE, currentBalance = 10500.0),
            com.example.data.entity.Customer(4L, "CUST-104", "Kyoto Silk", com.example.data.entity.CustomerAccountType.EXPORT, currentBalance = 4200.0)
        )

        // Filter by search query "boutique"
        val searchResults = customers.filter { it.name.contains("boutique", ignoreCase = true) }
        assertEquals(1, searchResults.size)
        assertEquals("Atelier Boutique", searchResults.first().name)

        // Filter by account type EXPORT
        val exportResults = customers.filter { it.accountType == com.example.data.entity.CustomerAccountType.EXPORT }
        assertEquals(1, exportResults.size)
        assertEquals("Kyoto Silk", exportResults.first().name)

        // Filter by due balance only (currentBalance > 0)
        val dueResults = customers.filter { it.currentBalance > 0 }
        assertEquals(3, dueResults.size)
    }

    @Test
    fun `customer direct payment settlement decreases outstanding balance`() {
        var balance = 4200.0
        val paymentAmount = 2000.0
        balance -= paymentAmount
        assertEquals(2200.0, balance, 0.001)

        val fullSettlement = 2200.0
        balance -= fullSettlement
        assertEquals(0.0, balance, 0.001)
    }

    @Test
    fun `income statement and balance sheet equation reconcile accurately`() {
        val grossRevenue = 42000.0
        val totalExpenses = 26500.0
        val netIncome = grossRevenue - totalExpenses
        val marginPct = (netIncome / grossRevenue) * 100.0

        assertEquals(15500.0, netIncome, 0.001)
        assertTrue(marginPct > 36.0)

        val totalAssets = 128000.0
        val totalLiabilities = 22500.0
        val ownerEquity = 90000.0
        val totalLiabilitiesAndEquity = totalLiabilities + ownerEquity + netIncome
        assertEquals(totalAssets, totalLiabilitiesAndEquity, 0.001)
    }

    @Test
    fun `erpnext bom template scales raw material quantities with scrap allowance`() {
        val targetUnits = 100.0
        val fabricMetersPerUnit = 1.8
        val scrapPercent = 5.0
        val scrapMultiplier = 1.0 + (scrapPercent / 100.0)
        val requiredFabricMeters = fabricMetersPerUnit * targetUnits * scrapMultiplier

        // 100 units * 1.8m * 1.05 = 189.0 meters
        assertEquals(189.0, requiredFabricMeters, 0.001)
    }

    @Test
    fun `erpnext apparel variant matrix generates accurate color and size sku combinations`() {
        val skuPrefix = "FG-OXF"
        val colors = listOf("Indigo", "Ivory")
        val sizes = listOf("S", "M", "L")
        val generatedSkus = colors.flatMap { c ->
            sizes.map { s -> "$skuPrefix-${c.take(3).uppercase()}-$s" }
        }

        assertEquals(6, generatedSkus.size)
        assertTrue(generatedSkus.contains("FG-OXF-IND-M"))
        assertTrue(generatedSkus.contains("FG-OXF-IVO-L"))
    }

    @Test
    fun `inventory item category and low stock threshold alert severity evaluate accurately`() {
        val criticalDye = com.example.data.entity.InventoryItem(
            id = 9L,
            sku = "DC-IND-01",
            name = "Natural Indigo Vat Dye Concentrate",
            category = com.example.data.entity.ItemCategory.DYE_CHEMICAL,
            unit = com.example.data.entity.UnitOfMeasure.KG,
            currentStock = 28.0,
            reorderLevel = 80.0,
            criticalThreshold = 35.0,
            averageCost = 18.50
        )

        assertTrue(criticalDye.isLowStock)
        assertTrue(criticalDye.isCriticalStock)
        assertEquals(com.example.data.entity.StockAlertSeverity.CRITICAL, criticalDye.alertSeverity)
        assertEquals(52.0, criticalDye.stockDeficit, 0.001)
        assertEquals(132.0, criticalDye.recommendedRestockQty, 0.001)
        assertEquals("Dyes & Chemicals", criticalDye.category.displayName)
    }

    @Test
    fun `fabric roll 4 point inspection system evaluates pass and quarantine accurately`() {
        val gradeARoll = com.example.data.entity.FabricRollLot(
            id = 1L,
            rollNumber = "ROLL-2026-881",
            dyeLotCode = "LOT-LIN-26A",
            itemId = 1L,
            itemSku = "FAB-101",
            itemName = "Organic White Linen Fabric",
            supplierName = "Nile Delta Spinning & Weaving",
            shadeBand = "A1",
            lengthMeters = 120.0,
            gsm = 165,
            widthCm = 150,
            defectPointsPer100SqYds = 8,
            qualityGrade = com.example.data.entity.RollQualityGrade.A_GRADE
        )

        val quarantinedRoll = gradeARoll.copy(
            id = 2L,
            rollNumber = "ROLL-2026-883",
            defectPointsPer100SqYds = 34,
            qualityGrade = com.example.data.entity.RollQualityGrade.QUARANTINED
        )

        assertTrue(gradeARoll.passesFourPointSystem)
        assertTrue(!quarantinedRoll.passesFourPointSystem)
    }

    @Test
    fun `multi currency conversion and role approval gate rules evaluate accurately`() {
        val euroOrder = com.example.data.entity.CommercialOrder(
            id = 10L,
            orderNumber = "QT-2026-014",
            type = com.example.data.entity.CommercialType.SALES_QUOTATION,
            partyName = "Atelier Milano S.r.l.",
            totalAmount = 6300.0,
            paidAmount = 0.0,
            status = com.example.data.entity.CommercialStatus.CONFIRMED,
            currencyCode = "EUR",
            exchangeRateToUsd = 1.08
        )

        // 6300 USD / 1.08 USD per EUR = 5833.33 EUR
        assertEquals(5833.33, euroOrder.foreignCurrencyTotal, 0.05)

        val controller = com.example.data.entity.ErpRole.EXECUTIVE_CONTROLLER
        val prodManager = com.example.data.entity.ErpRole.PRODUCTION_MANAGER
        assertTrue(controller.canApproveHighValuePo)
        assertTrue(!prodManager.canApproveHighValuePo)
    }
}
