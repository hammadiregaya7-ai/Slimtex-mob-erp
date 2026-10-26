# TexERP Enterprise Suite Expansion: End-to-End Procurement, Traceability, Document Chaining & Operational Intelligence

This plan upgrades **TexERP** into a full-lifecycle textile manufacturing and wholesale ERP by connecting supplier procurement, dye-lot roll traceability, multi-currency export packing lists, role-based approval gates, barcode/QR warehouse lookup, interactive visual charts, and native Android document sharing/printing—all powered by the fast, offline-first Room SQLite database.

### User Review & Critical Decisions

> [!IMPORTANT]
> All core scope, operational tooling, and storage preferences have been confirmed through your selections and are incorporated into this blueprint with zero feature pruning.

- **Confirmed ERP Modules & Workflows**:
  1. **Supplier Directory & One-Click Low-Stock PO Generation**: Dedicated mill/vendor directory with lead times, reliability ratings, AP balances, and one-tap conversion of low-stock threshold alerts into Purchase Orders.
  2. **End-to-End Document Chaining (`Sales Quote → Production Work Order → Commercial Invoice → Export Packing List`)**: Linked document lifecycle where quotes convert into manufacturing batches and fulfilled batches convert directly into commercial invoices and shipping packing lists.
  3. **Fabric Roll & Dye-Lot Batch Traceability with Defect Logging**: Roll-level and dye-lot tracking (`4-Point Fabric Inspection System`), shrinkage/shade band grading (`A-Grade`, `B-Grade`, `Quarantine`), and defect point logging per batch.
  4. **Multi-Currency Exchange Rates & Export Shipping Packing Lists**: Live currency conversion (`USD`, `EUR`, `JPY`, `GBP`, `DKK`) with container/bale packing list generation (net/gross weight, carton count, HS codes, Incoterms).
- **Confirmed Operational, Reporting & Export Tools**:
  1. **Printable & Shareable Invoice, PO & Cost Sheet Exports**: Native Android PrintManager / PDF & formatted document sharing for Commercial Invoices, Purchase Orders, Export Packing Lists, and Production Cost Sheets.
  2. **Interactive Visual Charts for Revenue, COGS & Stock Trends**: Custom Compose Canvas charts on the Dashboard and Accounting screens showing Revenue vs. COGS/Expenses, Inventory Valuation by Category, and Stock Movement Velocity.
  3. **Barcode & QR Scanner Lookup for Warehouse SKUs & Batches**: Instant SKU/Roll/Batch barcode & QR code lookup and label preview modal accessible from the Inventory screen and global Awesomebar.
  4. **Role-Based Approval Gates for Large Purchase Orders & Credit Overrides**: Configurable executive approval workflow (`Plant Manager`, `Finance Controller`, `Warehouse Lead`) that gates high-value POs (e.g., `> $5,000`) and customer credit-limit overrides before ledger posting.
- **Confirmed Data Persistence Strategy**:
  - **Offline-First Room SQLite Database**: All new entities, foreign-key relationships, and reactive `StateFlow` streams remain 100% on-device in Room for zero-latency industrial operation.

---

### 1. Overview & Core Concept

- **What It Does**: Bridges the gap between raw material procurement, shop-floor fabric roll inspection, garment production, and international wholesale export. Users can manage textile mills in a dedicated Suppliers directory, auto-generate Purchase Orders from low-stock alerts, inspect incoming fabric rolls by dye lot, chain customer quotes through production into multi-currency invoices and export packing lists, enforce controller approval gates on large commitments, scan/lookup SKU barcodes, and export or print formatted ERP documents.
- **Target Audience / Persona**: Textile mill operations directors, apparel production planners, warehouse supervisors, and wholesale/export finance controllers operating across phones and tablets.
- **Key Value**: Eliminates manual re-entry across departments by chaining every document (`Low-Stock Alert → PO → Roll Lot Receipt → Work Order → QC Inspection → Commercial Invoice & Packing List`) while maintaining strict double-entry accounting and credit/PO approval governance.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Low-Stock Alert to Approved Purchase Order**: From the Inventory Low-Stock Threshold Alerts banner, tapping **Auto-Generate PO** pre-fills a Purchase Order for the item's preferred supplier using the recommended restock quantity. If the PO total exceeds the approval threshold (`$5,000`), it enters `PENDING_APPROVAL` status until authorized via the **Approval Gate** action.
  2. **Fabric Roll & Dye-Lot Traceability**: In Inventory & Production, opening **Roll & Dye-Lot Traceability** displays individual fabric rolls/lots (e.g., `LOT-IND-2604`, Shade Band `A1`, GSM, Width, Defect Points) and lets warehouse staff log 4-Point inspection defects or quarantine off-shade rolls.
  3. **Document Chaining (`Quote → Work Order → Invoice → Packing List`)**: In Clients & Accounting, a **Sales Quotation / Order** can spawn a linked **Production Work Order** with one tap; once completed, tapping **Chain to Invoice & Packing List** generates the Commercial Invoice (with selected currency `USD`/`EUR`/`JPY`/`GBP` and Incoterms `FOB`/`CIF`) and an **Export Packing List** detailing cartons, gross/net kg, and HS tariff codes.
  4. **Barcode / QR Lookup & Label Card**: Tapping the **Scan / QR Lookup** button in the top bar or Inventory header opens an interactive scanner/code simulator & visual QR/Barcode label generator that jumps straight to the matching SKU, Dye Lot, or Production Batch.
  5. **Native Document Print & Share**: On any Commercial Invoice, Purchase Order, Export Packing List, or Production Cost Sheet, tapping **Print / Export PDF** renders a clean, print-ready document preview and triggers Android's native print/share sheet.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-craft industrial ERP precision—structured financial tables, crisp monospace SKU/lot badges, and high-contrast status pills tailored for rapid shop-floor and executive scanning.
  - *Color Palette & Mood*:
    - Primary Slate Navy (`#0F172A` light / `#93C5FD` dark) for authoritative headers and navigation chrome.
    - Emerald Accent (`#10B981` / `#059669` / `#D1FAE5`) for profitability, approved gates, `A-Grade` quality lots, and fulfilled orders.
    - Amber Warning (`#F59E0B` / `#D97706` / `#FEF3C7`) for low-stock threshold alerts, pending controller approvals, and `B-Grade` shade lots.
    - Crimson Alert (`#EF4444` / `#FEE2E2`) for critical stock deficits, credit limit breaches, and quarantined dye lots.
  - *Typography & Hierarchy*: Bold sans-serif section headers paired with `FontFamily.Monospace` for all SKU codes, dye-lot numbers, exchange rates, accounting debits/credits, and barcode strings so numerical columns align cleanly.
  - *Component Styling & Layout*: Adaptive Material 3 canonical layouts (`NavigationRail` on tablet/expanded widths, `NavigationBar` on handhelds) on an 8dp spatial grid with 10dp–12dp rounded card containers and subtle tonal elevation.
- **Interactive Feedback & Motion**: Animated Canvas bar/line charts on the Dashboard and P&L tabs, instant status badge transitions when approving POs or credit overrides, and live currency conversion previews when toggling currencies.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Integrated Document Chaining with Shared Reference Lineage**
  - *Chosen Approach*: Link documents via explicit parent-child reference codes (`parentDocumentRef`, `linkedBatchNumber`, `linkedPackingListNumber`) directly inside the Room database and surface one-tap **"Convert to Next Stage"** buttons on every document card.
  - *Why*: Mirrors ERPNext's core workflow advantage—users never re-type line items when moving from a Customer Quote to a Production Batch or from an Invoice to an Export Packing List.
  - *Alternatives Considered*: Separate unlinked forms for quotes, batches, and packing lists (rejected because manual re-entry causes quantity and costing errors).
- **Decision 2: Role-Based Approval Switcher & Governance Gate**
  - *Chosen Approach*: Provide an active **ERP Role Selector** in the top command bar (`Executive / Controller`, `Production Manager`, `Warehouse Lead`) alongside an **Approval Queue** that flags Purchase Orders above `$5,000` and Invoices that exceed a customer's credit limit.
  - *Why*: Lets the user test and operate multi-role approval workflows immediately on-device without requiring external cloud IAM setup.
  - *Alternatives Considered*: Hard-blocking transactions without an override mechanism (rejected because real textile operations require controller sign-off overrides for urgent mill shipments).
- **Decision 3: Native Compose Canvas Analytics & Android PrintManager Export**
  - *Chosen Approach*: Build financial & stock trend charts using Jetpack Compose `Canvas` and generate printable HTML/PDF documents via Android's native `WebView` + `PrintManager` and `Intent.ACTION_SEND` sharing.
  - *Why*: Keeps APK size fast and compact, works 100% offline, and produces crisp multi-column invoices, POs, packing lists, and cost sheets.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     ErpAppShell (Adaptive Compose UI)                        │
│  ┌──────────────────────────┬────────────────────────┬────────────────────┐  │
│  │ Awesomebar Global Search │ Active Role Switcher   │ Barcode/QR Scanner │  │
│  │ (SKUs, Lots, POs, Docs)  │ (Controller/Prod/Whse) │ & Label Generator  │  │
│  └──────────────────────────┴────────────────────────┴────────────────────┘  │
└───────────────┬──────────────────────────┬───────────────────────┬───────────┘
                │                          │                       │
      ┌─────────▼─────────┐      ┌─────────▼─────────┐   ┌─────────▼──────────┐
      │ Dashboard &       │      │ Inventory,        │   │ Clients, Suppliers │
      │ Visual Analytics  │      │ Suppliers & Lots  │   │ & Accounting Suite │
      │ • Revenue vs COGS │      │ • Threshold Alerts│   │ • Document Chain   │
      │   Canvas Chart    │      │ • One-Click PO Gen│   │   Quote->WO->Inv   │
      │ • Stock Valuation │      │ • Dye-Lot & Roll  │   │ • Multi-Currency   │
      │   Breakdown Chart │      │   Defect Tracker  │   │   & Packing Lists  │
      │ • Approval Queue  │      │ • Supplier Ledger │   │ • Print/Share Docs │
      └─────────┬─────────┘      └─────────┬─────────┘   └─────────┬──────────┘
                │                          │                       │
┌───────────────▼──────────────────────────▼───────────────────────▼───────────┐
│                ErpViewModel & InventoryViewModel (StateFlow)                 │
│  • Reactive UI States • Currency Converter • Approval Gate Validator         │
│  • Document Chaining Engine • Print & Share Document Formatter               │
└──────────────────────────────────────────┬───────────────────────────────────┘
                                           │
┌──────────────────────────────────────────▼───────────────────────────────────┐
│                    ErpRepository (Transactional Domain Layer)                │
│  • Auto-syncs Low-Stock Alerts, Double-Entry Journals & Supplier/Client AR/AP│
└──────────────────────────────────────────┬───────────────────────────────────┘
                                           │
┌──────────────────────────────────────────▼───────────────────────────────────┐
│                     AppDatabase (Offline-First Room SQLite)                  │
│  ┌─────────────────┐ ┌──────────────────┐ ┌───────────────┐ ┌─────────────┐  │
│  │ inventory_items │ │ fabric_roll_lots │ │   suppliers   │ │ commercial_ │  │
│  │ stock_threshold │ │ (dyeLot, shade,  │ │ (leadTime,    │ │ orders &    │  │
│  │ _alerts         │ │  defectPoints,   │ │  apBalance,   │ │ packing_    │  │
│  │ stock_txns      │ │  gradeStatus)    │ │  currency)    │ │ lists       │  │
│  └─────────────────┘ └──────────────────┘ └───────────────┘ └─────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - **`Supplier` Entity (`suppliers` table)**: Stores mill/vendor code (`SUP-201`), name, category specialty (`Yarn Spinner`, `Denim Mill`, `Dyehouse`, `Trim Supplier`), contact details, default currency (`USD`, `EUR`, `JPY`, `GBP`), lead time days, quality rating, and current AP balance.
  - **`FabricRollLot` Entity (`fabric_roll_lots` table)**: Tracks individual fabric rolls and dye lots linked to an `InventoryItem` (`rollNumber`, `dyeLotCode`, `shadeBand`, `lengthMeters`, `gsm`, `widthCm`, `defectPointsPer100SqYds`, `qualityGrade` [`A_GRADE`, `B_GRADE`, `QUARANTINED`], `supplierName`, `inspectorNotes`).
  - **Enhanced `CommercialOrder` & `ExportPackingList` (`commercial_orders` & `export_packing_lists` tables)**:
    - Supports full document lifecycle types (`SALES_QUOTATION`, `PURCHASE_ORDER`, `COMMERCIAL_INVOICE`) and approval states (`APPROVED`, `PENDING_APPROVAL`, `REJECTED`).
    - Tracks `currencyCode` (`USD`, `EUR`, `JPY`, `GBP`, `DKK`), `exchangeRateToUsd`, `incoterms` (`FOB`, `CIF`, `EXW`, `DDP`), `parentDocumentRef` (for document chaining), and packing list metadata (`totalCartons`, `netWeightKg`, `grossWeightKg`, `hsTariffCode`, `portOfLoading`, `portOfDischarge`).
- **Interactive Component & State Mapping**:
  - **One-Click Low-Stock PO Generator**: Clicking **Generate PO** on a `StockThresholdAlert` looks up the item's preferred `Supplier`, calculates `recommendedOrderQuantity * averageCost`, checks if the total exceeds the `$5,000` PO approval gate, and inserts the `CommercialOrder` in Room.
  - **Approval Gate Workflow**: Orders in `PENDING_APPROVAL` display an **Approve & Post to Ledger** action when the active role is `Executive / Controller`, unlocking fulfillment and double-entry journal posting.
  - **Document Chaining Action Bar**:
    - On a `SALES_QUOTATION`: **Convert to Production Work Order** or **Convert to Commercial Invoice**.
    - On a `COMMERCIAL_INVOICE`: **Generate Export Packing List** and **Print / Share Document**.
  - **Barcode / QR Scanner & Label Modal**: Filters `inventoryItems`, `fabricRollLots`, and `productionOrders` in real time by scanned/entered code and renders a visual barcode/QR warehouse tag for printing.
