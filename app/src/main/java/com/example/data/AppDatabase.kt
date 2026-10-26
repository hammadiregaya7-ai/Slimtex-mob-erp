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
import com.example.data.dao.SupplierDao
import com.example.data.dao.TraceabilityDao
import com.example.data.entity.Account
import com.example.data.entity.AccountType
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import com.example.data.entity.Customer
import com.example.data.entity.CustomerAccountType
import com.example.data.entity.FabricRollLot
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import com.example.data.entity.NormalBalance
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import com.example.data.entity.ProductionStatus
import com.example.data.entity.RollQualityGrade
import com.example.data.entity.StockAlertSeverity
import com.example.data.entity.StockThresholdAlert
import com.example.data.entity.StockTransaction
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierSpecialty
import com.example.data.entity.TransactionType
import com.example.data.entity.UnitOfMeasure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InventoryItem::class,
        StockTransaction::class,
        StockThresholdAlert::class,
        FabricRollLot::class,
        Supplier::class,
        ProductionOrder::class,
        ProductionMaterialItem::class,
        Account::class,
        JournalEntry::class,
        JournalLine::class,
        CommercialOrder::class,
        CommercialOrderItem::class,
        Customer::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun productionDao(): ProductionDao
    abstract fun accountingDao(): AccountingDao
    abstract fun commercialDao(): CommercialDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun traceabilityDao(): TraceabilityDao

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
            val supplierDao = db.supplierDao()
            val traceabilityDao = db.traceabilityDao()

            // 0a. Seed Textile Mills & Suppliers
            val sup1 = supplierDao.insertSupplier(
                Supplier(
                    code = "SUP-201",
                    name = "Indus Spinning & Weaving Mills",
                    specialty = SupplierSpecialty.FABRIC_WEAVING_MILL,
                    contactPerson = "Tariq Mahmood",
                    email = "exports@indusweaving.pk",
                    phone = "+92 42 3571 8800",
                    country = "Pakistan",
                    currencyCode = "USD",
                    leadTimeDays = 10,
                    qualityRating = 4.9,
                    paymentTerms = "Net 30",
                    apBalance = 6400.0,
                    notes = "Primary supplier for combed cotton poplin and 14oz selvedge denim"
                )
            )
            val sup2 = supplierDao.insertSupplier(
                Supplier(
                    code = "SUP-202",
                    name = "Bavaria Dyestuffs AG",
                    specialty = SupplierSpecialty.DYEHOUSE_CHEMICALS,
                    contactPerson = "Hannah Weber",
                    email = "orders@bavariadyes.de",
                    phone = "+49 89 2311 400",
                    country = "Germany",
                    currencyCode = "EUR",
                    leadTimeDays = 7,
                    qualityRating = 4.8,
                    paymentTerms = "Net 15",
                    apBalance = 2800.0,
                    notes = "OEKO-TEX certified natural indigo vat dyes and reactive auxiliaries"
                )
            )
            val sup3 = supplierDao.insertSupplier(
                Supplier(
                    code = "SUP-203",
                    name = "Kyoto Silk Weavers",
                    specialty = SupplierSpecialty.FABRIC_WEAVING_MILL,
                    contactPerson = "Haruki Sato",
                    email = "sato@kyotosilkweavers.jp",
                    phone = "+81 75 441 9022",
                    country = "Japan",
                    currencyCode = "JPY",
                    leadTimeDays = 14,
                    qualityRating = 5.0,
                    paymentTerms = "Letter of Credit",
                    apBalance = 5000.0,
                    notes = "Grade-6A mulberry silk habotai and jacquard linings"
                )
            )
            val sup4 = supplierDao.insertSupplier(
                Supplier(
                    code = "SUP-204",
                    name = "YKK Fastening & Trims Europe",
                    specialty = SupplierSpecialty.TRIMS_NOTIONS,
                    contactPerson = "Luca Moretti",
                    email = "b2b@ykkeurope.it",
                    phone = "+39 02 9399 41",
                    country = "Italy",
                    currencyCode = "EUR",
                    leadTimeDays = 5,
                    qualityRating = 4.9,
                    paymentTerms = "Net 30",
                    apBalance = 0.0,
                    notes = "Antique brass zippers, corozo & horn buttons"
                )
            )

            // 0b. Seed Textile Clients & Wholesale Customers
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
            val item9 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "DC-IND-01",
                    name = "Natural Indigo Vat Dye Concentrate",
                    category = ItemCategory.DYE_CHEMICAL,
                    unit = UnitOfMeasure.KG,
                    currentStock = 28.0,
                    reorderLevel = 80.0,
                    criticalThreshold = 35.0,
                    averageCost = 18.50,
                    storageLocation = "Dye House Vault V-2",
                    preferredSupplier = "Bavaria Dyestuffs AG",
                    leadTimeDays = 10
                )
            )
            val item10 = inventoryDao.insertItem(
                InventoryItem(
                    sku = "RM-SLK-05",
                    name = "Mulberry Silk Habotai 16mm (Ivory)",
                    category = ItemCategory.RAW_MATERIAL,
                    unit = UnitOfMeasure.METERS,
                    currentStock = 95.0,
                    reorderLevel = 150.0,
                    criticalThreshold = 60.0,
                    averageCost = 14.20,
                    storageLocation = "Climate Rack S-01",
                    preferredSupplier = "Kyoto Silk Weavers",
                    leadTimeDays = 14
                )
            )

            // Seed Low-Stock Threshold Alerts
            inventoryDao.insertThresholdAlert(
                StockThresholdAlert(
                    itemId = item9,
                    sku = "DC-IND-01",
                    itemName = "Natural Indigo Vat Dye Concentrate",
                    category = ItemCategory.DYE_CHEMICAL,
                    unit = UnitOfMeasure.KG,
                    currentStock = 28.0,
                    thresholdLevel = 80.0,
                    criticalLevel = 35.0,
                    severity = StockAlertSeverity.CRITICAL,
                    deficitQuantity = 52.0,
                    recommendedOrderQuantity = 132.0
                )
            )
            inventoryDao.insertThresholdAlert(
                StockThresholdAlert(
                    itemId = item10,
                    sku = "RM-SLK-05",
                    itemName = "Mulberry Silk Habotai 16mm (Ivory)",
                    category = ItemCategory.RAW_MATERIAL,
                    unit = UnitOfMeasure.METERS,
                    currentStock = 95.0,
                    thresholdLevel = 150.0,
                    criticalLevel = 60.0,
                    severity = StockAlertSeverity.LOW_STOCK,
                    deficitQuantity = 55.0,
                    recommendedOrderQuantity = 205.0
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

            // 3b. Seed Fabric Roll & Dye-Lot Traceability Records (4-Point Inspection)
            traceabilityDao.insertAllRollLots(
                listOf(
                    FabricRollLot(
                        rollNumber = "ROLL-2601",
                        dyeLotCode = "LOT-NVY-88A",
                        itemId = item1,
                        itemSku = "RM-COT-01",
                        itemName = "100% Combed Cotton Poplin (Navy)",
                        supplierName = "Indus Spinning & Weaving Mills",
                        shadeBand = "A1 (Center Shade)",
                        lengthMeters = 120.0,
                        gsm = 135,
                        widthCm = 152,
                        shrinkagePercent = 1.2,
                        defectPointsPer100SqYds = 6,
                        qualityGrade = RollQualityGrade.A_GRADE,
                        batchUsedIn = "BAT-2026-101",
                        inspectorNotes = "Passed 4-Point inspection; uniform warp tension."
                    ),
                    FabricRollLot(
                        rollNumber = "ROLL-2602",
                        dyeLotCode = "LOT-DEN-14B",
                        itemId = item2,
                        itemSku = "RM-DEN-02",
                        itemName = "Heavyweight Selvedge Denim 14oz",
                        supplierName = "Indus Spinning & Weaving Mills",
                        shadeBand = "B2 (Slightly Dark)",
                        lengthMeters = 85.0,
                        gsm = 475,
                        widthCm = 82,
                        shrinkagePercent = 2.8,
                        defectPointsPer100SqYds = 22,
                        qualityGrade = RollQualityGrade.B_GRADE,
                        batchUsedIn = "Unassigned",
                        inspectorNotes = "Minor weft slubbing at 42m mark; within 28-pt tolerance."
                    ),
                    FabricRollLot(
                        rollNumber = "ROLL-2603",
                        dyeLotCode = "LOT-SLK-04Q",
                        itemId = item10,
                        itemSku = "RM-SLK-05",
                        itemName = "Mulberry Silk Habotai 16mm (Ivory)",
                        supplierName = "Kyoto Silk Weavers",
                        shadeBand = "C1 (Off-Shade)",
                        lengthMeters = 45.0,
                        gsm = 68,
                        widthCm = 114,
                        shrinkagePercent = 3.6,
                        defectPointsPer100SqYds = 39,
                        qualityGrade = RollQualityGrade.QUARANTINED,
                        batchUsedIn = "HOLD-QA",
                        inspectorNotes = "Exceeds 28 pts/100 sq yds threshold due to edge dye streak. Hold for supplier claim."
                    )
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

            // 5. Sample Commercial Orders (Fulfilled PO, Approval-Gated PO, Sales Quote, and Multi-Currency Export Invoice)
            commercialDao.createOrderWithItems(
                order = CommercialOrder(
                    orderNumber = "PO-2026-038",
                    type = CommercialType.PURCHASE_ORDER,
                    partyName = "Indus Spinning & Weaving Mills",
                    supplierId = sup1,
                    status = CommercialStatus.FULFILLED,
                    paidAmount = 3200.0,
                    currencyCode = "USD",
                    exchangeRateToUsd = 1.0,
                    approvedByRole = "Executive Controller",
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

            // High-value Purchase Order awaiting Executive Controller Approval (> $5,000 gate)
            commercialDao.createOrderWithItems(
                order = CommercialOrder(
                    orderNumber = "PO-2026-042",
                    type = CommercialType.PURCHASE_ORDER,
                    partyName = "Kyoto Silk Weavers",
                    supplierId = sup3,
                    status = CommercialStatus.PENDING_APPROVAL,
                    requiresApproval = true,
                    currencyCode = "JPY",
                    exchangeRateToUsd = 0.0067,
                    incoterms = "CIF",
                    notes = "Replenishment for low-stock Mulberry Silk Habotai (Requires Controller Approval > $5,000)"
                ),
                items = listOf(
                    CommercialOrderItem(
                        orderId = 0L,
                        itemId = item10,
                        itemName = "Mulberry Silk Habotai 16mm (Ivory)",
                        unit = UnitOfMeasure.METERS,
                        quantity = 450.0,
                        unitPrice = 14.20,
                        lineTotal = 6390.0
                    )
                )
            )

            // Sales Quotation ready for Document Chaining (Quote -> Work Order -> Invoice)
            commercialDao.createOrderWithItems(
                order = CommercialOrder(
                    orderNumber = "QT-2026-014",
                    type = CommercialType.SALES_QUOTATION,
                    partyName = "Atelier & Co Boutique",
                    customerId = cust2,
                    status = CommercialStatus.CONFIRMED,
                    currencyCode = "EUR",
                    exchangeRateToUsd = 1.09,
                    incoterms = "DDP",
                    portOfLoading = "Antwerp Textile Hub",
                    portOfDischarge = "Paris Rue Saint-Honoré",
                    notes = "Autumn Capsule Quotation — Ready to chain into Production Work Order or Invoice"
                ),
                items = listOf(
                    CommercialOrderItem(
                        orderId = 0L,
                        itemId = item8,
                        itemName = "Raw Selvedge Denim Tapered Jeans",
                        unit = UnitOfMeasure.PIECES,
                        quantity = 60.0,
                        unitPrice = 68.00,
                        lineTotal = 4080.0
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
                    currencyCode = "EUR",
                    exchangeRateToUsd = 1.09,
                    incoterms = "FOB",
                    parentDocumentRef = "QT-2026-009",
                    linkedBatchNumber = "BAT-2026-101",
                    packingListNumber = "PKL-2026-088",
                    totalCartons = 5,
                    netWeightKg = 42.0,
                    grossWeightKg = 47.5,
                    hsTariffCode = "6205.20.00",
                    portOfLoading = "Hamburg Container Terminal",
                    portOfDischarge = "Copenhagen Logistics Hub, Dock 4",
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
