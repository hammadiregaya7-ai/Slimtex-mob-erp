package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AccountingDao
import com.example.data.dao.CommercialDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.InventoryDao
import com.example.data.dao.ProductionDao
import com.example.data.entity.Account
import com.example.data.entity.AccountType
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import com.example.data.entity.Customer
import com.example.data.entity.CustomerAccountType
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import com.example.data.entity.NormalBalance
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.ProductionStatus
import com.example.data.entity.StockTransaction
import com.example.data.entity.TransactionType
import com.example.data.entity.UnitOfMeasure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InventoryItem::class,
        StockTransaction::class,
        ProductionOrder::class,
        ProductionMaterialItem::class,
        Account::class,
        JournalEntry::class,
        JournalLine::class,
        CommercialOrder::class,
        CommercialOrderItem::class,
        Customer::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun productionDao(): ProductionDao
    abstract fun accountingDao(): AccountingDao
    abstract fun commercialDao(): CommercialDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "texerp_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate sample data asynchronously
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val inventoryDao = db.inventoryDao()
            val accountingDao = db.accountingDao()
            val productionDao = db.productionDao()
            val commercialDao = db.commercialDao()
            val customerDao = db.customerDao()

            // 0. Seed Textile Clients & Wholesale Customers
            val cust1 = customerDao.insertCustomer(
                Customer(
                    code = "CUST-101",
                    name = "Nordic Apparel Wholesalers",
                    accountType = CustomerAccountType.WHOLESALE,
                    contactPerson = "Elena Rostova",
                    email = "elena@nordicapparel.eu",
                    phone = "+45 33 12 34 56",
                    address = "Copenhagen Logistics Hub, Dock 4",
                    creditLimit = 50000.0,
                    paymentTerms = "Net 30",
                    currentBalance = 2300.0,
                    notes = "Primary Scandinavian distributor for Classic Oxford shirts"
                )
            )
            val cust2 = customerDao.insertCustomer(
                Customer(
                    code = "CUST-102",
                    name = "Atelier & Co Boutique",
                    accountType = CustomerAccountType.RETAIL_BOUTIQUE,
                    contactPerson = "Marc Delacroix",
                    email = "orders@atelierparis.com",
                    phone = "+33 1 42 68 00 11",
                    address = "Rue Saint-Honoré, Paris",
                    creditLimit = 15000.0,
                    paymentTerms = "Net 15",
                    currentBalance = 0.0,
                    notes = "High-end bespoke boutique client for raw denim and linen shirts"
                )
            )
            val cust3 = customerDao.insertCustomer(
                Customer(
                    code = "CUST-103",
                    name = "Metro Department Stores",
                    accountType = CustomerAccountType.CORPORATE,
                    contactPerson = "David Miller",
                    email = "procurement@metrostores.com",
                    phone = "+1 (212) 555-0199",
                    address = "7th Ave, New York, NY",
                    creditLimit = 80000.0,
                    paymentTerms = "Net 60",
                    currentBalance = 10500.0,
                    notes = "National retail chain with seasonal bulk orders"
                )
            )
            val cust4 = customerDao.insertCustomer(
                Customer(
                    code = "CUST-104",
                    name = "Kyoto Silk & Garments Trading",
                    accountType = CustomerAccountType.EXPORT,
                    contactPerson = "Kenji Takahashi",
                    email = "kenji@kyototrading.jp",
                    phone = "+81 75 211 4400",
                    address = "Shimogyo-ku, Kyoto, Japan",
                    creditLimit = 120000.0,
                    paymentTerms = "Letter of Credit",
                    currentBalance = 4200.0,
                    notes = "International export client for premium silk garments and jacquard weaves"
                )
            )

            // 1. Chart of Accounts
            val accounts = listOf(
                Account("1010", "Cash on Hand", AccountType.ASSET, NormalBalance.DEBIT, 15400.0, "Vault cash"),
                Account("1020", "Operating Bank Account", AccountType.ASSET, NormalBalance.DEBIT, 48500.0, "Commercial checking"),
                Account("1100", "Accounts Receivable (AR)", AccountType.ASSET, NormalBalance.DEBIT, 12800.0, "Customer unpaid invoices"),
                Account("1200", "Inventory - Raw Materials", AccountType.ASSET, NormalBalance.DEBIT, 22150.0, "Fabrics, yarns, trims"),
                Account("1300", "Inventory - Work in Progress (WIP)", AccountType.ASSET, NormalBalance.DEBIT, 3420.0, "Active production batches"),
                Account("1400", "Inventory - Finished Goods", AccountType.ASSET, NormalBalance.DEBIT, 9970.0, "Ready-to-ship garments"),
                Account("2000", "Accounts Payable (AP)", AccountType.LIABILITY, NormalBalance.CREDIT, 14200.0, "Owed to mills and suppliers"),
                Account("3000", "Owner's Equity & Capital", AccountType.EQUITY, NormalBalance.CREDIT, 98040.0, "Retained earnings & initial equity"),
                Account("4000", "Textile Sales Revenue", AccountType.REVENUE, NormalBalance.CREDIT, 38600.0, "Wholesale & boutique sales"),
                Account("5000", "Cost of Goods Sold (COGS)", AccountType.EXPENSE, NormalBalance.DEBIT, 19400.0, "Material cost of fulfilled sales"),
                Account("5100", "Direct Tailoring Labor", AccountType.EXPENSE, NormalBalance.DEBIT, 5800.0, "Cutting and stitching wages"),
                Account("5200", "Factory Overhead & Power", AccountType.EXPENSE, NormalBalance.DEBIT, 2800.0, "Utilities, machine depreciation")
            )
            accountingDao.insertAllAccounts(accounts)

            // 2. Inventory Items
            val item1 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "RM-COT-01",
                    name = "100% Combed Cotton Poplin (Navy)",
                    category = ItemCategory.RAW_MATERIAL,
                    unit = UnitOfMeasure.METERS,
                    currentStock = 2450.0,
                    reorderLevel = 500.0,
                    averageCost = 3.20,
                    storageLocation = "Rack A-04"
                )
            )
            val item2 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "RM-DEN-02",
                    name = "Heavyweight Selvedge Denim 14oz",
                    category = ItemCategory.RAW_MATERIAL,
                    unit = UnitOfMeasure.METERS,
                    currentStock = 1180.0,
                    reorderLevel = 300.0,
                    averageCost = 5.50,
                    storageLocation = "Rack B-01"
                )
            )
            val item3 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "RM-LIN-03",
                    name = "European Linen Weave (Natural Ecru)",
                    category = ItemCategory.RAW_MATERIAL,
                    unit = UnitOfMeasure.METERS,
                    currentStock = 820.0,
                    reorderLevel = 250.0,
                    averageCost = 4.80,
                    storageLocation = "Rack A-08"
                )
            )
            val item4 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "RM-YRN-04",
                    name = "Spun Polyester Yarn 30/1 White",
                    category = ItemCategory.RAW_MATERIAL,
                    unit = UnitOfMeasure.KG,
                    currentStock = 540.0,
                    reorderLevel = 150.0,
                    averageCost = 2.90,
                    storageLocation = "Yarn Room Bay 2"
                )
            )
            val item5 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "TR-ZIP-01",
                    name = "YKK Metal Zipper #5 Antique Brass",
                    category = ItemCategory.TRIM,
                    unit = UnitOfMeasure.PIECES,
                    currentStock = 3200.0,
                    reorderLevel = 500.0,
                    averageCost = 0.65,
                    storageLocation = "Bin T-12"
                )
            )
            val item6 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "TR-BTN-02",
                    name = "Horn Buttons 24L 4-Hole Smoke",
                    category = ItemCategory.TRIM,
                    unit = UnitOfMeasure.PIECES,
                    currentStock = 7500.0,
                    reorderLevel = 1200.0,
                    averageCost = 0.08,
                    storageLocation = "Bin T-03"
                )
            )
            val item7 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "FG-SHT-01",
                    name = "Men's Classic Oxford Shirt (Navy)",
                    category = ItemCategory.FINISHED_GOOD,
                    unit = UnitOfMeasure.PIECES,
                    currentStock = 340.0,
                    reorderLevel = 50.0,
                    averageCost = 12.50,
                    sellingPrice = 38.00,
                    storageLocation = "Finished Goods Bay C"
                )
            )
            val item8 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "FG-JNS-02",
                    name = "Raw Selvedge Denim Tapered Jeans",
                    category = ItemCategory.FINISHED_GOOD,
                    unit = UnitOfMeasure.PIECES,
                    currentStock = 165.0,
                    reorderLevel = 40.0,
                    averageCost = 22.00,
                    sellingPrice = 68.00,
                    storageLocation = "Finished Goods Bay D"
                )
            )

            // 3. Initial Stock Audit Log
            inventoryDao.insertTransaction(
                StockTransaction(
                    itemId = item1,
                    itemName = "100% Combed Cotton Poplin (Navy)",
                    type = TransactionType.PURCHASE_RECEIPT,
                    quantityDelta = 1000.0,
                    unitCost = 3.20,
                    totalCost = 3200.0,
                    referenceNumber = "PO-2026-038",
                    notes = "Received 20 rolls from Indus Mills"
                )
            )

            // 4. Sample Production Order with Cost Sheet & BOM
            val prodOrderId = productionDao.insertOrder(
                ProductionOrder(
                    batchNumber = "BAT-2026-101",
                    finishedItemId = item7,
                    finishedItemName = "Men's Classic Oxford Shirt (Navy)",
                    targetQuantity = 200.0,
                    completedQuantity = 0.0,
                    status = ProductionStatus.IN_PROGRESS,
                    directLaborCost = 650.0, // Cutting & Tailoring
                    overheadCost = 280.0, // Machinery power & packing
                    rawMaterialCost = 1540.0, // Materials subtotal
                    totalBatchCost = 2470.0, // 1540 + 650 + 280
                    unitProductionCost = 12.35, // 2470 / 200
                    notes = "Boutique Spring collection order"
                )
            )

            // BOM Materials for this batch:
            productionDao.insertMaterial(
                ProductionMaterialItem(
                    productionOrderId = prodOrderId,
                    itemId = item1,
                    itemName = "100% Combed Cotton Poplin (Navy)",
                    unit = UnitOfMeasure.METERS,
                    quantityRequired = 440.0, // 2.2m per shirt
                    unitCost = 3.20,
                    totalCost = 1408.0
                )
            )
            productionDao.insertMaterial(
                ProductionMaterialItem(
                    productionOrderId = prodOrderId,
                    itemId = item6,
                    itemName = "Horn Buttons 24L 4-Hole Smoke",
                    unit = UnitOfMeasure.PIECES,
                    quantityRequired = 1650.0, // ~8 buttons per shirt + allowance
                    unitCost = 0.08,
                    totalCost = 132.0
                )
            )

            // 5. Sample Commercial Orders
            commercialDao.createOrderWithItems(
                order = CommercialOrder(
                    orderNumber = "PO-2026-038",
                    type = CommercialType.PURCHASE_ORDER,
                    partyName = "Indus Spinning & Weaving Mills",
                    status = CommercialStatus.FULFILLED,
                    paidAmount = 3200.0,
                    notes = "Supplier payment cleared via Bank Wire"
                ),
                items = listOf(
                    CommercialOrderItem(
                        orderId = 0L,
                        itemId = item1,
                        itemName = "100% Combed Cotton Poplin (Navy)",
                        unit = UnitOfMeasure.METERS,
                        quantity = 1000.0,
                        unitPrice = 3.20,
                        lineTotal = 3200.0
                    )
                )
            )

            commercialDao.createOrderWithItems(
                order = CommercialOrder(
                    orderNumber = "INV-2026-088",
                    type = CommercialType.COMMERCIAL_INVOICE,
                    partyName = "Nordic Apparel Wholesalers",
                    customerId = cust1,
                    status = CommercialStatus.CONFIRMED,
                    paidAmount = 1500.0,
                    notes = "Deposit paid, balance net 30 days"
                ),
                items = listOf(
                    CommercialOrderItem(
                        orderId = 0L,
                        itemId = item7,
                        itemName = "Men's Classic Oxford Shirt (Navy)",
                        unit = UnitOfMeasure.PIECES,
                        quantity = 100.0,
                        unitPrice = 38.00,
                        lineTotal = 3800.0
                    )
                )
            )

            // 6. Recent Balanced Journal Entry
            accountingDao.recordBalancedEntry(
                entry = JournalEntry(
                    entryNumber = "JE-1001",
                    memo = "Sales invoice dispatch to Nordic Apparel (INV-2026-088)",
                    sourceReference = "INV-2026-088",
                    totalAmount = 3800.0
                ),
                lines = listOf(
                    JournalLine(0L, 0L, "1100", "Accounts Receivable (AR)", debit = 3800.0, credit = 0.0),
                    JournalLine(0L, 0L, "4000", "Textile Sales Revenue", debit = 0.0, credit = 3800.0)
                )
            )
        }
    }
}
