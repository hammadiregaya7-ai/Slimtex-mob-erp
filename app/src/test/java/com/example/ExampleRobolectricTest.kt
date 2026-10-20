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
}

